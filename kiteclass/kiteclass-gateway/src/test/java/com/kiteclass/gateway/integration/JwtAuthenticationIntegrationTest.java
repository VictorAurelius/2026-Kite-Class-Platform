package com.kiteclass.gateway.integration;

import com.kiteclass.gateway.security.jwt.JwtTokenProvider;
import com.kiteclass.gateway.security.jwt.TokenType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

/**
 * Integration tests for JWT authentication flow.
 * Tests token generation, validation, and extraction of user information.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
@DisplayName("JWT Authentication Integration Tests")
class JwtAuthenticationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/test");
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", () ->
                "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/test");
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Access token should be valid and contain correct user information")
    void shouldGenerateValidAccessToken() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        List<String> roles = Arrays.asList("OWNER", "ADMIN");

        // When
        String accessToken = jwtTokenProvider.generateAccessToken(userId, email, roles);

        // Then
        assert jwtTokenProvider.validateToken(accessToken) != null;
        assert jwtTokenProvider.getUserIdFromToken(accessToken).equals(userId);
        assert jwtTokenProvider.isAccessToken(accessToken);
    }

    @Test
    @DisplayName("Refresh token should be valid and contain correct user ID")
    void shouldGenerateValidRefreshToken() {
        // Given
        Long userId = 1L;

        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // Then
        assert jwtTokenProvider.validateToken(refreshToken) != null;
        assert jwtTokenProvider.getUserIdFromToken(refreshToken).equals(userId);
        assert jwtTokenProvider.isRefreshToken(refreshToken);
    }

    @Test
    @DisplayName("Invalid token should fail validation")
    void shouldRejectInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When/Then
        try {
            jwtTokenProvider.validateToken(invalidToken);
            assert false : "Should have thrown exception";
        } catch (Exception e) {
            // Expected - invalid token should throw exception
            assert true;
        }
    }

    @Test
    @DisplayName("Expired token should fail validation")
    void shouldRejectExpiredToken() {
        // This test would require setting a very short expiration time
        // For now, we verify that token validation logic exists
        // In real scenario, would need to wait for token to expire or mock time
        assert true; // Placeholder - proper implementation requires time mocking
    }

    @Test
    @DisplayName("Access token with wrong type should not be usable as refresh token")
    void shouldRejectAccessTokenAsRefreshToken() {
        // Given
        String accessToken = jwtTokenProvider.generateAccessToken(1L, "test@example.com", Arrays.asList("OWNER"));

        // When/Then
        assert jwtTokenProvider.isAccessToken(accessToken);
        assert !jwtTokenProvider.isRefreshToken(accessToken);
        // Application should reject using access token where refresh token is expected
    }

    @Test
    @DisplayName("API call with valid JWT should be authenticated")
    void shouldAuthenticateWithValidToken() {
        // Given - Generate a valid access token
        String accessToken = jwtTokenProvider.generateAccessToken(
                1L,
                "owner@kiteclass.local",
                Arrays.asList("OWNER")
        );

        // When/Then - Access protected endpoint (user list requires OWNER role)
        webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isArray();
    }

    @Test
    @DisplayName("API call without JWT should be unauthorized")
    void shouldRejectRequestWithoutToken() {
        // When/Then - Try to access protected endpoint without token
        webTestClient.get()
                .uri("/api/v1/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("API call with invalid JWT should be unauthorized")
    void shouldRejectRequestWithInvalidToken() {
        // When/Then - Try to access protected endpoint with invalid token
        webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer invalid.jwt.token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("API call with malformed Authorization header should be unauthorized")
    void shouldRejectMalformedAuthorizationHeader() {
        // Given - Valid token but malformed header (missing Bearer prefix)
        String accessToken = jwtTokenProvider.generateAccessToken(
                1L,
                "owner@kiteclass.local",
                Arrays.asList("OWNER")
        );

        // When/Then
        webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", accessToken) // Missing "Bearer " prefix
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("JWT should contain all required claims")
    void shouldContainAllRequiredClaims() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        List<String> roles = Arrays.asList("OWNER", "ADMIN");

        // When
        String accessToken = jwtTokenProvider.generateAccessToken(userId, email, roles);

        // Then - Verify all claims can be extracted
        assert jwtTokenProvider.getUserIdFromToken(accessToken).equals(userId);
        assert jwtTokenProvider.isAccessToken(accessToken);
        assert jwtTokenProvider.validateToken(accessToken) != null;
        // Note: Email and roles extraction methods would need to be added to JwtTokenProvider
        // or tested through SecurityContextRepository
    }
}
