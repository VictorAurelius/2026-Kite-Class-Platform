package com.kiteclass.gateway.integration;

import com.kiteclass.gateway.common.constant.UserStatus;
import com.kiteclass.gateway.module.auth.dto.LoginRequest;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for account locking functionality.
 * Tests failed login attempts tracking and automatic account locking.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Account Locking Integration Tests")
class AccountLockingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

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
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a test user for locking tests
        testUser = new User();
        testUser.setEmail("locktest@example.com");
        testUser.setName("Lock Test User");
        testUser.setPasswordHash(passwordEncoder.encode("Test@123"));
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setFailedLoginAttempts(0);
        testUser.setLockedUntil(null);
        testUser.setDeleted(false);

        // Delete if exists and save
        userRepository.findByEmailAndDeletedFalse(testUser.getEmail())
                .flatMap(existing -> userRepository.deleteById(existing.getId()))
                .then(userRepository.save(testUser))
                .block();

        // Refresh test user
        testUser = userRepository.findByEmailAndDeletedFalse(testUser.getEmail()).block();
        assertThat(testUser).isNotNull();
    }

    @Test
    @DisplayName("First failed login should set failed attempts to 1")
    void shouldIncrementFailedAttemptsOnFirstFailure() {
        // Given
        LoginRequest request = new LoginRequest(testUser.getEmail(), "WrongPassword");

        // When - First failed attempt
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();

        // Then - Failed attempts should be 1
        User updatedUser = userRepository.findByEmailAndDeletedFalse(testUser.getEmail()).block();
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(updatedUser.getLockedUntil()).isNull(); // Not locked yet
    }

    @Test
    @DisplayName("Five failed logins should lock account for 30 minutes")
    void shouldLockAccountAfterFiveFailedAttempts() {
        // Given
        LoginRequest request = new LoginRequest(testUser.getEmail(), "WrongPassword");

        // When - Fail 5 times
        for (int i = 0; i < 5; i++) {
            webTestClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        // Then - Account should be locked
        User lockedUser = userRepository.findByEmailAndDeletedFalse(testUser.getEmail()).block();
        assertThat(lockedUser).isNotNull();
        assertThat(lockedUser.getFailedLoginAttempts()).isGreaterThanOrEqualTo(5);
        assertThat(lockedUser.getLockedUntil()).isNotNull();
        assertThat(lockedUser.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Login attempt on locked account should be rejected")
    void shouldRejectLoginOnLockedAccount() throws Exception {
        // Given - Lock the account
        testUser.setFailedLoginAttempts(5);
        testUser.setLockedUntil(Instant.now().plusSeconds(1800)); // 30 minutes
        userRepository.save(testUser).block();

        // When - Try to login with correct password
        LoginRequest request = new LoginRequest(testUser.getEmail(), "Test@123");

        // Then - Should be rejected due to lock
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("AUTH_ACCOUNT_LOCKED");
    }

    @Test
    @DisplayName("Successful login should reset failed attempts counter")
    void shouldResetFailedAttemptsOnSuccessfulLogin() {
        // Given - Set some failed attempts
        testUser.setFailedLoginAttempts(3);
        userRepository.save(testUser).block();

        // When - Login successfully
        LoginRequest request = new LoginRequest(testUser.getEmail(), "Test@123");

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        // Then - Failed attempts should be reset to 0
        User updatedUser = userRepository.findByEmailAndDeletedFalse(testUser.getEmail()).block();
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(updatedUser.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("Account should auto-unlock after 30 minutes")
    void shouldAutoUnlockAfterLockPeriod() throws Exception {
        // Given - Lock the account but in the past
        testUser.setFailedLoginAttempts(5);
        testUser.setLockedUntil(Instant.now().minusSeconds(60)); // 1 minute ago (expired)
        userRepository.save(testUser).block();

        // When - Try to login with correct password
        LoginRequest request = new LoginRequest(testUser.getEmail(), "Test@123");

        // Then - Should succeed because lock expired
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.accessToken").exists();

        // And failed attempts should be reset
        User updatedUser = userRepository.findByEmailAndDeletedFalse(testUser.getEmail()).block();
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("Failed attempts should increment correctly up to 5")
    void shouldIncrementFailedAttemptsCorrectly() {
        // Given
        LoginRequest request = new LoginRequest(testUser.getEmail(), "WrongPassword");

        // When/Then - Test each attempt
        for (int expectedAttempts = 1; expectedAttempts <= 5; expectedAttempts++) {
            webTestClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isUnauthorized();

            User updatedUser = userRepository.findByEmailAndDeletedFalse(testUser.getEmail()).block();
            assertThat(updatedUser).isNotNull();
            assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(expectedAttempts);

            if (expectedAttempts < 5) {
                assertThat(updatedUser.getLockedUntil()).isNull(); // Not locked yet
            } else {
                assertThat(updatedUser.getLockedUntil()).isNotNull(); // Locked at 5th attempt
            }
        }
    }

    @Test
    @DisplayName("Locked account should show appropriate error message")
    void shouldShowLockedAccountErrorMessage() throws Exception {
        // Given - Lock the account
        testUser.setFailedLoginAttempts(5);
        testUser.setLockedUntil(Instant.now().plusSeconds(1800));
        userRepository.save(testUser).block();

        // When - Try to login
        LoginRequest request = new LoginRequest(testUser.getEmail(), "Test@123");

        // Then - Should show locked account message
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("AUTH_ACCOUNT_LOCKED")
                .jsonPath("$.error.message").exists();
    }
}
