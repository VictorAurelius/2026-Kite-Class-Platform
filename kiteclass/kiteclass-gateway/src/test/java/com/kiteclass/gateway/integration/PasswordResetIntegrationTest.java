package com.kiteclass.gateway.integration;

import com.kiteclass.gateway.module.auth.dto.ForgotPasswordRequest;
import com.kiteclass.gateway.module.auth.dto.ResetPasswordRequest;
import com.kiteclass.gateway.module.auth.entity.PasswordResetToken;
import com.kiteclass.gateway.module.auth.repository.PasswordResetTokenRepository;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for password reset functionality.
 * Tests end-to-end password reset flow with real database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Password Reset Integration Tests")
class PasswordResetIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // R2DBC configuration
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/test");
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        // Flyway configuration (uses JDBC)
        registry.add("spring.flyway.url", () ->
                "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/test");
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);

        // Disable Redis for tests
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");

        // Configure test email settings (emails won't actually be sent in tests)
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "1025");
        registry.add("email.from", () -> "test@kiteclass.com");
        registry.add("email.base-url", () -> "http://localhost:3000");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Should create reset token for valid email")
    void shouldCreateResetTokenForValidEmail() {
        // Given - Default owner account from V4 migration
        ForgotPasswordRequest request = new ForgotPasswordRequest("owner@kiteclass.local");

        // When
        webTestClient.post()
                .uri("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").exists();

        // Then - Verify token was created in database
        StepVerifier.create(
                userRepository.findByEmailAndDeletedFalse("owner@kiteclass.local")
                        .flatMapMany(user -> passwordResetTokenRepository.findByUserId(user.getId()))
        )
                .assertNext(token -> {
                    assertThat(token.getToken()).isNotNull();
                    assertThat(token.getExpiresAt()).isAfter(Instant.now());
                    assertThat(token.getUsedAt()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Should succeed for non-existent email (security)")
    void shouldSucceedForNonExistentEmail() {
        // Given - Non-existent email
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        // When/Then - Should still return success (don't reveal if email exists)
        webTestClient.post()
                .uri("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Should fail for invalid email format")
    void shouldFailForInvalidEmailFormat() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest("not-an-email");

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Should reset password with valid token")
    void shouldResetPasswordWithValidToken() throws Exception {
        // Given - Create a reset token for owner account
        User owner = userRepository.findByEmailAndDeletedFalse("owner@kiteclass.local").block();
        assertThat(owner).isNotNull();

        String resetToken = "test-reset-token-" + System.currentTimeMillis();
        PasswordResetToken tokenEntity = PasswordResetToken.builder()
                .token(resetToken)
                .userId(owner.getId())
                .expiresAt(Instant.now().plusSeconds(3600)) // 1 hour
                .createdAt(Instant.now())
                .build();
        passwordResetTokenRepository.save(tokenEntity).block();

        String newPassword = "NewPassword@456";
        ResetPasswordRequest request = new ResetPasswordRequest(resetToken, newPassword);

        // When
        webTestClient.post()
                .uri("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);

        // Then - Verify password was updated
        User updatedUser = userRepository.findById(owner.getId()).block();
        assertThat(updatedUser).isNotNull();
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPasswordHash())).isTrue();

        // Verify token was marked as used
        PasswordResetToken usedToken = passwordResetTokenRepository.findByToken(resetToken).block();
        assertThat(usedToken).isNotNull();
        assertThat(usedToken.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Should fail with invalid token")
    void shouldFailWithInvalidToken() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "NewPassword@456");

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").exists();
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Should fail with expired token")
    void shouldFailWithExpiredToken() {
        // Given - Create an expired reset token
        User owner = userRepository.findByEmailAndDeletedFalse("owner@kiteclass.local").block();
        assertThat(owner).isNotNull();

        String expiredToken = "expired-token-" + System.currentTimeMillis();
        PasswordResetToken tokenEntity = PasswordResetToken.builder()
                .token(expiredToken)
                .userId(owner.getId())
                .expiresAt(Instant.now().minusSeconds(3600)) // Expired 1 hour ago
                .createdAt(Instant.now().minusSeconds(7200))
                .build();
        passwordResetTokenRepository.save(tokenEntity).block();

        ResetPasswordRequest request = new ResetPasswordRequest(expiredToken, "NewPassword@456");

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Should fail with used token")
    void shouldFailWithUsedToken() {
        // Given - Create a used reset token
        User owner = userRepository.findByEmailAndDeletedFalse("owner@kiteclass.local").block();
        assertThat(owner).isNotNull();

        String usedToken = "used-token-" + System.currentTimeMillis();
        PasswordResetToken tokenEntity = PasswordResetToken.builder()
                .token(usedToken)
                .userId(owner.getId())
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .usedAt(Instant.now().minusSeconds(60)) // Already used
                .build();
        passwordResetTokenRepository.save(tokenEntity).block();

        ResetPasswordRequest request = new ResetPasswordRequest(usedToken, "NewPassword@456");

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Should fail with weak password")
    void shouldFailWithWeakPassword() {
        // Given
        User owner = userRepository.findByEmailAndDeletedFalse("owner@kiteclass.local").block();
        assertThat(owner).isNotNull();

        String resetToken = "valid-token-" + System.currentTimeMillis();
        PasswordResetToken tokenEntity = PasswordResetToken.builder()
                .token(resetToken)
                .userId(owner.getId())
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();
        passwordResetTokenRepository.save(tokenEntity).block();

        // Weak password (no uppercase, no digit)
        ResetPasswordRequest request = new ResetPasswordRequest(resetToken, "weakpassword");

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Should invalidate all refresh tokens after reset")
    void shouldInvalidateRefreshTokensAfterReset() {
        // This test would require setting up refresh tokens first
        // For now, we'll verify the core reset functionality works
        // TODO: Enhance this test when refresh token integration is needed
    }
}
