package com.kiteclass.gateway.module.auth;

import com.kiteclass.gateway.common.exception.WeakPasswordException;
import com.kiteclass.gateway.module.auth.dto.request.RegisterRequest;
import com.kiteclass.gateway.module.auth.dto.response.AuthResponse;
import com.kiteclass.gateway.module.auth.service.AuthService;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security tests for password policy enforcement.
 * <p>
 * Tests password strength requirements:
 * <ul>
 *   <li>Minimum length (8 characters)</li>
 *   <li>Uppercase letter required</li>
 *   <li>Lowercase letter required</li>
 *   <li>Number required</li>
 *   <li>Special character required</li>
 *   <li>BCrypt hashing verification</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
@SpringBootTest
@Testcontainers
@DisplayName("Password Policy Tests")
class PasswordPolicyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Register should enforce minimum password length of 8 characters")
    void registerShouldEnforceMinPasswordLength() {
        // Given: Password with less than 8 characters
        RegisterRequest request = new RegisterRequest(
            "short@test.com",
            "Pass1!",  // Only 6 characters
            "Short User"
        );

        // When/Then: Registration should fail with WeakPasswordException
        StepVerifier.create(authService.register(request))
            .expectErrorMatches(error ->
                error instanceof WeakPasswordException &&
                error.getMessage().contains("at least 8 characters")
            )
            .verify();
    }

    @Test
    @DisplayName("Register should require uppercase, lowercase, and number")
    void registerShouldRequireUppercaseLowercaseNumber() {
        // Given: Password missing uppercase
        RegisterRequest requestNoUpper = new RegisterRequest(
            "noupper@test.com",
            "password123!",  // No uppercase
            "No Upper User"
        );

        // When/Then: Should fail
        StepVerifier.create(authService.register(requestNoUpper))
            .expectErrorMatches(error ->
                error instanceof WeakPasswordException &&
                error.getMessage().contains("uppercase")
            )
            .verify();

        // Given: Password missing lowercase
        RegisterRequest requestNoLower = new RegisterRequest(
            "nolower@test.com",
            "PASSWORD123!",  // No lowercase
            "No Lower User"
        );

        // When/Then: Should fail
        StepVerifier.create(authService.register(requestNoLower))
            .expectErrorMatches(error ->
                error instanceof WeakPasswordException &&
                error.getMessage().contains("lowercase")
            )
            .verify();

        // Given: Password missing number
        RegisterRequest requestNoNumber = new RegisterRequest(
            "nonumber@test.com",
            "Password!@#",  // No number
            "No Number User"
        );

        // When/Then: Should fail
        StepVerifier.create(authService.register(requestNoNumber))
            .expectErrorMatches(error ->
                error instanceof WeakPasswordException &&
                error.getMessage().contains("number")
            )
            .verify();
    }

    @Test
    @DisplayName("Register should require special character")
    void registerShouldRequireSpecialCharacter() {
        // Given: Password without special character
        RegisterRequest request = new RegisterRequest(
            "nospecial@test.com",
            "Password123",  // No special character
            "No Special User"
        );

        // When/Then: Should fail with WeakPasswordException
        StepVerifier.create(authService.register(request))
            .expectErrorMatches(error ->
                error instanceof WeakPasswordException &&
                error.getMessage().contains("special character")
            )
            .verify();
    }

    @Test
    @DisplayName("Register should hash password with BCrypt")
    void registerShouldHashPasswordWithBcrypt() {
        // Given: Valid strong password
        String plainPassword = "SecurePass123!@#";
        RegisterRequest request = new RegisterRequest(
            "hash@test.com",
            plainPassword,
            "Hash User"
        );

        // When: User registers
        AuthResponse response = authService.register(request).block();
        assertThat(response).isNotNull();

        // Then: Password should be hashed with BCrypt
        User user = userRepository.findByEmail("hash@test.com").block();
        assertThat(user).isNotNull();
        assertThat(user.getPasswordHash())
            .isNotNull()
            .startsWith("$2a$")  // BCrypt prefix
            .hasSize(60)         // BCrypt hash length
            .isNotEqualTo(plainPassword);  // Not stored in plain text

        // And: Original password should not be recoverable
        assertThat(user.getPasswordHash()).doesNotContain(plainPassword);
    }
}
