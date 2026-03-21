package com.kiteclass.gateway.module.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.gateway.common.constant.UserStatus;
import com.kiteclass.gateway.config.TestContainersConfiguration;
import com.kiteclass.gateway.module.auth.dto.LoginRequest;
import com.kiteclass.gateway.module.auth.dto.RefreshTokenRequest;
import com.kiteclass.gateway.module.auth.repository.RefreshTokenRepository;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AuthController} with real database and full Spring context.
 * Tests end-to-end authentication flows with PostgreSQL via Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(TestContainersConfiguration.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {


    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Reset the owner account state before each test to prevent contamination from failed login attempts
        // Also ensure password hash is correct (DefaultUserInitializer doesn't run in test profile)
        userRepository.findByEmailAndDeletedFalse("owner@kiteclass.local")
                .flatMap(user -> {
                    // Clean up any existing refresh tokens for owner to prevent test pollution
                    return refreshTokenRepository.deleteByUserId(user.getId())
                            .then(Mono.defer(() -> {
                                user.setFailedLoginAttempts(0);
                                user.setLockedUntil(null);
                                user.setStatus(UserStatus.ACTIVE);
                                user.setPasswordHash(passwordEncoder.encode("Admin@123"));
                                return userRepository.save(user);
                            }));
                })
                .block();
    }

    @AfterEach
    void tearDown() {
        // Clean up the owner account state and refresh tokens after each test
        userRepository.findByEmailAndDeletedFalse("owner@kiteclass.local")
                .flatMap(user -> {
                    return refreshTokenRepository.deleteByUserId(user.getId())
                            .then(Mono.defer(() -> {
                                user.setFailedLoginAttempts(0);
                                user.setLockedUntil(null);
                                user.setStatus(UserStatus.ACTIVE);
                                return userRepository.save(user);
                            }));
                })
                .block();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Success with default owner account")
    void shouldLoginSuccessfully() {
        // Given - Default owner account from V4 migration
        LoginRequest request = new LoginRequest("owner@kiteclass.local", "Admin@123");

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.accessToken").exists()
                .jsonPath("$.data.refreshToken").exists()
                .jsonPath("$.data.tokenType").isEqualTo("Bearer")
                .jsonPath("$.data.expiresIn").exists()
                .jsonPath("$.data.user.email").isEqualTo("owner@kiteclass.local")
                .jsonPath("$.data.user.name").isEqualTo("System Owner")
                .jsonPath("$.data.user.roles[0]").isEqualTo("OWNER");
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Invalid email")
    void shouldReturnUnauthorizedForInvalidEmail() {
        // Given
        LoginRequest request = new LoginRequest("notexist@example.com", "Admin@123");

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").exists()
                .jsonPath("$.message").exists()
                .jsonPath("$.path").exists()
                .jsonPath("$.timestamp").exists();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Invalid password")
    void shouldReturnUnauthorizedForInvalidPassword() {
        // Given
        LoginRequest request = new LoginRequest("owner@kiteclass.local", "WrongPassword");

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").exists()
                .jsonPath("$.message").exists()
                .jsonPath("$.path").exists()
                .jsonPath("$.timestamp").exists();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Validation error (missing email)")
    void shouldReturnBadRequestForMissingEmail() {
        // Given
        String requestBody = "{\"password\":\"Admin@123\"}";

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").exists()
                .jsonPath("$.message").exists()
                .jsonPath("$.path").exists()
                .jsonPath("$.timestamp").exists();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Validation error (missing password)")
    void shouldReturnBadRequestForMissingPassword() {
        // Given
        String requestBody = "{\"email\":\"owner@kiteclass.local\"}";

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").exists()
                .jsonPath("$.message").exists()
                .jsonPath("$.path").exists()
                .jsonPath("$.timestamp").exists();
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Success")
    void shouldRefreshTokenSuccessfully() throws Exception {
        // Given - First login to get refresh token
        LoginRequest loginRequest = new LoginRequest("owner@kiteclass.local", "Admin@123");

        byte[] responseBody = webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        // Extract refresh token from JSON response
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String refreshToken = jsonNode.get("data").get("refreshToken").asText();

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(refreshRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.accessToken").exists()
                .jsonPath("$.data.refreshToken").exists();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Refresh token persisted to database")
    void shouldPersistRefreshTokenToDatabaseOnLogin() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("owner@kiteclass.local", "Admin@123");

        // When - Login
        byte[] responseBody = webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        // Extract tokens from response
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String refreshToken = jsonNode.get("data").get("refreshToken").asText();
        Long userId = jsonNode.get("data").get("user").get("id").asLong();

        // Then - Verify refresh token exists in database
        refreshTokenRepository.findByToken(refreshToken)
                .as(StepVerifier::create)
                .assertNext(token -> {
                    assertThat(token.getToken()).isEqualTo(refreshToken);
                    assertThat(token.getUserId()).isEqualTo(userId);
                    assertThat(token.getExpiresAt()).isAfter(Instant.now());
                })
                .verifyComplete();

        // And - Verify token can be used for refresh
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        webTestClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(refreshRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.accessToken").exists()
                .jsonPath("$.data.refreshToken").exists();

        // And - Verify old token was deleted (wait for reactive transaction to complete)
        // Note: Added delay to prevent flakiness from reactive transaction timing
        Mono.delay(java.time.Duration.ofMillis(100))
                .then(refreshTokenRepository.findByToken(refreshToken))
                .as(StepVerifier::create)
                .verifyComplete(); // Old token should be deleted
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Invalid token")
    void shouldReturnUnauthorizedForInvalidRefreshToken() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").exists()
                .jsonPath("$.message").exists()
                .jsonPath("$.path").exists()
                .jsonPath("$.timestamp").exists();
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Success")
    void shouldLogoutSuccessfully() throws Exception {
        // Given - First login to get access token
        LoginRequest loginRequest = new LoginRequest("owner@kiteclass.local", "Admin@123");

        byte[] responseBody = webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        // Extract refresh token from JSON response
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String refreshToken = jsonNode.get("data").get("refreshToken").asText();

        // When/Then
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(refreshToken);
        webTestClient.post()
                .uri("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(logoutRequest)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Success")
    void shouldSendForgotPasswordEmail() {
        // Given
        String requestBody = "{\"email\":\"owner@kiteclass.local\"}";

        // When/Then - Email not implemented yet, just checks API works
        webTestClient.post()
                .uri("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").exists();
    }

}
