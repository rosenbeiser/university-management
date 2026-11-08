package com.university.management.service;

import com.university.management.dto.request.LoginRequest;
import com.university.management.dto.request.RefreshTokenRequest;
import com.university.management.dto.response.AuthResponse;
import com.university.management.dto.response.UserAccountResponse;
import com.university.management.entity.RefreshToken;
import com.university.management.entity.UserAccount;
import com.university.management.enums.Role;
import com.university.management.exception.BusinessException;
import com.university.management.repository.RefreshTokenRepository;
import com.university.management.repository.UserAccountRepository;
import com.university.management.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    private UserAccount enabledAccount;
    private UserAccount disabledAccount;
    private RefreshToken validRefreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);

        enabledAccount = UserAccount.builder()
                .id(1L)
                .username("admin01")
                .email("admin01@example.com")
                .passwordHash("$2a$12$hashed")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        disabledAccount = UserAccount.builder()
                .id(2L)
                .username("disabled_user")
                .email("disabled@example.com")
                .passwordHash("$2a$12$hashed")
                .role(Role.STUDENT)
                .enabled(false)
                .build();

        validRefreshToken = RefreshToken.builder()
                .id(1L)
                .userAccount(enabledAccount)
                .token("valid-refresh-uuid")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: should return AuthResponse with access and refresh tokens on success")
    void login_success_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin01");
        request.setPassword("SecurePass123!");

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("admin01", "SecurePass123!"));
        when(userAccountRepository.findByUsernameOrEmail("admin01", "admin01"))
                .thenReturn(Optional.of(enabledAccount));
        when(jwtService.generateAccessToken(enabledAccount)).thenReturn("access-jwt-token");
        when(refreshTokenRepository.save(any())).thenReturn(validRefreshToken);
        when(jwtService.getExpirationMillis()).thenReturn(900000L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-uuid");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getUsername()).isEqualTo("admin01");
        assertThat(response.getUser().getRole()).isEqualTo(Role.ADMIN);
        verify(jwtService).generateAccessToken(enabledAccount);
    }

    @Test
    @DisplayName("login: should throw BadCredentialsException when authentication fails")
    void login_badCredentials_throws() {
        LoginRequest request = new LoginRequest();
        request.setUsername("wronguser");
        request.setPassword("wrongpass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
        verify(userAccountRepository, never()).findByUsernameOrEmail(any(), any());
    }

    @Test
    @DisplayName("login: should throw DisabledException when account is disabled")
    void login_disabledAccount_throws() {
        LoginRequest request = new LoginRequest();
        request.setUsername("disabled_user");
        request.setPassword("SecurePass123!");

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("disabled_user", "SecurePass123!"));
        when(userAccountRepository.findByUsernameOrEmail("disabled_user", "disabled_user"))
                .thenReturn(Optional.of(disabledAccount));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("disabled");
        verify(jwtService, never()).generateAccessToken(any());
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refresh: valid token should rotate and return new pair")
    void refresh_validToken_returnsNewPair() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-uuid");

        RefreshToken newRefreshToken = RefreshToken.builder()
                .id(2L)
                .userAccount(enabledAccount)
                .token("new-refresh-uuid")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh-uuid"))
                .thenReturn(Optional.of(validRefreshToken));
        when(refreshTokenRepository.save(any())).thenReturn(newRefreshToken);
        when(jwtService.generateAccessToken(enabledAccount)).thenReturn("new-access-token");
        when(jwtService.getExpirationMillis()).thenReturn(900000L);

        AuthResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-uuid");
        // Old token should have been marked as revoked
        assertThat(validRefreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("refresh: expired token should revoke family and throw BusinessException")
    void refresh_expiredToken_throwsAndRevokesFamily() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(3L)
                .userAccount(enabledAccount)
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusDays(1)) // past expiry
                .revoked(false)
                .build();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");

        when(refreshTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).revokeAllByUserAccount(enabledAccount);
    }

    @Test
    @DisplayName("refresh: unknown token should throw BusinessException")
    void refresh_unknownToken_throws() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("unknown-token");

        when(refreshTokenRepository.findByToken("unknown-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logout: should revoke all tokens for existing user")
    void logout_existingUser_revokesAllTokens() {
        when(userAccountRepository.findByUsername("admin01"))
                .thenReturn(Optional.of(enabledAccount));

        authService.logout("admin01");

        verify(refreshTokenRepository).revokeAllByUserAccount(enabledAccount);
    }

    @Test
    @DisplayName("logout: should do nothing silently for non-existent username")
    void logout_nonExistentUser_doesNothing() {
        when(userAccountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> authService.logout("ghost"));
        verify(refreshTokenRepository, never()).revokeAllByUserAccount(any());
    }

    // ─── getCurrentUser ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getCurrentUser: should return mapped UserAccountResponse")
    void getCurrentUser_found_returnsResponse() {
        when(userAccountRepository.findByUsername("admin01"))
                .thenReturn(Optional.of(enabledAccount));

        UserAccountResponse response = authService.getCurrentUser("admin01");

        assertThat(response.getUsername()).isEqualTo("admin01");
        assertThat(response.getEmail()).isEqualTo("admin01@example.com");
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("getCurrentUser: should throw BusinessException when user not found")
    void getCurrentUser_notFound_throws() {
        when(userAccountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("unknown"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
    }
}

