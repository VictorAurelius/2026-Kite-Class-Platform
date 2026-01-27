package com.kiteclass.gateway.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtTokenProvider}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-must-be-at-least-512-bits-long-for-hs512-algorithm-security");
        jwtProperties.setAccessTokenExpiration(3600000L); // 1 hour
        jwtProperties.setRefreshTokenExpiration(604800000L); // 7 days

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        List<String> roles = List.of("OWNER", "ADMIN");

        // When
        String token = jwtTokenProvider.generateAccessToken(userId, email, roles);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(email);
        assertThat(jwtTokenProvider.getRolesFromToken(token)).containsExactlyInAnyOrderElementsOf(roles);
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        // Given
        Long userId = 1L;

        // When
        String token = jwtTokenProvider.generateRefreshToken(userId);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should validate token successfully")
    void shouldValidateTokenSuccessfully() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        List<String> roles = List.of("OWNER");
        String token = jwtTokenProvider.generateAccessToken(userId, email, roles);

        // When/Then
        assertThatNoException().isThrownBy(() -> jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should throw exception for invalid token")
    void shouldThrowExceptionForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When/Then
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should throw exception for expired token")
    void shouldThrowExceptionForExpiredToken() {
        // Given - create token with very short expiration
        jwtProperties.setAccessTokenExpiration(1L); // 1ms
        JwtTokenProvider shortExpiryProvider = new JwtTokenProvider(jwtProperties);
        String token = shortExpiryProvider.generateAccessToken(1L, "test@example.com", List.of("OWNER"));

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When/Then
        assertThatThrownBy(() -> shortExpiryProvider.validateToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        // Given
        Long userId = 123L;
        String token = jwtTokenProvider.generateAccessToken(userId, "test@example.com", List.of("OWNER"));

        // When
        Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmailFromToken() {
        // Given
        String email = "test@example.com";
        String token = jwtTokenProvider.generateAccessToken(1L, email, List.of("OWNER"));

        // When
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // Then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("Should extract roles from token")
    void shouldExtractRolesFromToken() {
        // Given
        List<String> roles = List.of("OWNER", "ADMIN", "TEACHER");
        String token = jwtTokenProvider.generateAccessToken(1L, "test@example.com", roles);

        // When
        List<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);

        // Then
        assertThat(extractedRoles).containsExactlyInAnyOrderElementsOf(roles);
    }

    @Test
    @DisplayName("Should identify access token correctly")
    void shouldIdentifyAccessTokenCorrectly() {
        // Given
        String accessToken = jwtTokenProvider.generateAccessToken(1L, "test@example.com", List.of("OWNER"));
        String refreshToken = jwtTokenProvider.generateRefreshToken(1L);

        // When/Then
        assertThat(jwtTokenProvider.isAccessToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(refreshToken)).isFalse();
    }

    @Test
    @DisplayName("Should identify refresh token correctly")
    void shouldIdentifyRefreshTokenCorrectly() {
        // Given
        String accessToken = jwtTokenProvider.generateAccessToken(1L, "test@example.com", List.of("OWNER"));
        String refreshToken = jwtTokenProvider.generateRefreshToken(1L);

        // When/Then
        assertThat(jwtTokenProvider.isRefreshToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(accessToken)).isFalse();
    }
}
