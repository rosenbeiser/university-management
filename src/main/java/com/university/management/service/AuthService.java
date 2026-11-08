package com.university.management.service;

import com.university.management.dto.request.LoginRequest;
import com.university.management.dto.request.RefreshTokenRequest;
import com.university.management.dto.response.AuthResponse;
import com.university.management.dto.response.UserAccountResponse;
import com.university.management.entity.RefreshToken;
import com.university.management.entity.UserAccount;
import com.university.management.exception.BusinessException;
import com.university.management.repository.RefreshTokenRepository;
import com.university.management.repository.UserAccountRepository;
import com.university.management.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    // ─── Login ────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Authenticate — throws BadCredentialsException or DisabledException on failure
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserAccount account = userAccountRepository
                .findByUsernameOrEmail(request.getUsername(), request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!account.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        String accessToken = jwtService.generateAccessToken(account);
        RefreshToken refreshToken = createRefreshToken(account);

        return buildAuthResponse(accessToken, refreshToken.getToken(), account);
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("Invalid refresh token."));

        if (!token.isValid()) {
            // Revoke the entire family on token reuse/expiry attempt (rotation)
            refreshTokenRepository.revokeAllByUserAccount(token.getUserAccount());
            throw new BusinessException("Refresh token has expired or been revoked. Please log in again.");
        }

        // Rotate: revoke old token, issue new pair
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        UserAccount account = token.getUserAccount();
        String newAccessToken = jwtService.generateAccessToken(account);
        RefreshToken newRefreshToken = createRefreshToken(account);

        return buildAuthResponse(newAccessToken, newRefreshToken.getToken(), account);
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String username) {
        userAccountRepository.findByUsername(username)
                .ifPresent(refreshTokenRepository::revokeAllByUserAccount);
    }

    // ─── Current user ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserAccountResponse getCurrentUser(String username) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found."));
        return mapToUserAccountResponse(account);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private RefreshToken createRefreshToken(UserAccount account) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userAccount(account)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshTokenValue, UserAccount account) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMillis() / 1000)
                .user(mapToUserAccountResponse(account))
                .build();
    }

    public static UserAccountResponse mapToUserAccountResponse(UserAccount account) {
        return UserAccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .role(account.getRole())
                .enabled(account.isEnabled())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
