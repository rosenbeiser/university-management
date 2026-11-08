package com.university.management.security;

import com.university.management.entity.UserAccount;
import com.university.management.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private UserAccount testUser;

    // Valid Base64-encoded 256-bit key for HS256
    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 900000L);

        testUser = UserAccount.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$12$hash")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("generateAccessToken: should return non-null, non-blank token")
    void generateAccessToken_returnsToken() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("extractUsername: should return correct subject from token")
    void extractUsername_returnsCorrectUsername() {
        String token = jwtService.generateAccessToken(testUser);
        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("isTokenValid: valid token for correct user returns true")
    void isTokenValid_correctUser_returnsTrue() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.isTokenValid(token, "testuser")).isTrue();
    }

    @Test
    @DisplayName("isTokenValid: valid token for wrong user returns false")
    void isTokenValid_wrongUser_returnsFalse() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.isTokenValid(token, "otheruser")).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired: fresh token should not be expired")
    void isTokenExpired_freshToken_notExpired() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired: token with 1ms expiry should be expired immediately")
    void isTokenExpired_immediatelyExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L);
        String token = jwtService.generateAccessToken(testUser);
        Thread.sleep(10);
        assertThat(jwtService.isTokenExpired(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid: malformed token returns false without exception")
    void isTokenValid_malformedToken_returnsFalse() {
        assertThat(jwtService.isTokenValid("this.is.notvalid", "testuser")).isFalse();
    }

    @Test
    @DisplayName("isTokenValid: tampered token returns false")
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtService.generateAccessToken(testUser);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtService.isTokenValid(tampered, "testuser")).isFalse();
    }

    @Test
    @DisplayName("token: role is embedded in claims")
    void token_containsRoleClaim() {
        String token = jwtService.generateAccessToken(testUser);
        String roleValue = jwtService.extractClaim(token,
                claims -> claims.get("role", String.class));
        assertThat(roleValue).isEqualTo("ADMIN");
    }
}
