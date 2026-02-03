package com.kiteclass.gateway.module.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.gateway.common.constant.UserStatus;
import com.kiteclass.gateway.config.TestContainersConfiguration;
import com.kiteclass.gateway.module.auth.dto.LoginRequest;
import com.kiteclass.gateway.module.auth.dto.RefreshTokenRequest;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    @SuppressWarnings("resource") // Managed by Testcontainers framework


    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Reset the owner account state before each test to prevent contamination from failed login attempts
        // Also ensure password hash is correct (DefaultUserInitializer doesn't run in test profile)
        userRepository.findByEmailAndDeletedFalse("owner@kiteclass.local")
                .flatMap(user -> {
                    user.setFailedLoginAttempts(0);
                    user.setLockedUntil(null);
                    user.setStatus(UserStatus.ACTIVE);
                    user.setPasswordHash(passwordEncoder.encode("Admin@123"));
                    return userRepository.save(user);
                })
                .block();
    }

    @AfterEach
    void tearDown() {
        // Clean up the owner account state after each test
        userRepository.findByEmailAndDeletedFalse("owner@kiteclass.local")
                .flatMap(user -> {
                    user.setFailedLoginAttempts(0);
                    user.setLockedUntil(null);
                    user.setStatus(UserStatus.ACTIVE);
                    return userRepository.save(user);
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

        // Extract access token from JSON response
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String accessToken = jsonNode.get("data").get("accessToken").asText();

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.message").exists();
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
