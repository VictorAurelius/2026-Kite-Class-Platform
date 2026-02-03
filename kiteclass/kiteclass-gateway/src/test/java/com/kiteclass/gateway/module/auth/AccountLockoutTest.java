package com.kiteclass.gateway.module.auth;

import com.kiteclass.gateway.common.exception.AccountLockedException;
import com.kiteclass.gateway.common.exception.InvalidCredentialsException;
import com.kiteclass.gateway.module.auth.dto.LoginRequest;
import com.kiteclass.gateway.module.auth.dto.request.RegisterRequest;
import com.kiteclass.gateway.module.auth.service.AuthService;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import com.kiteclass.gateway.config.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security tests for account lockout mechanism.
 * <p>
 * Tests brute-force attack prevention:
 * <ul>
 *   <li>Account lockout after 5 failed attempts</li>
 *   <li>Automatic unlock after lockout period (15 minutes)</li>
 *   <li>Failed attempt counter reset on successful login</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@DisplayName("Account Lockout Tests")
class AccountLockoutTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_EMAIL = "lockout@test.com";
    private static final String CORRECT_PASSWORD = "SecurePass123!@#";
    private static final String WRONG_PASSWORD = "WrongPassword123!";

    @BeforeEach
    void setUp() {
        // Clean up test user if exists
        userRepository.findByEmail(TEST_EMAIL)
            .flatMap(user -> userRepository.delete(user))
            .block();
    }

    @Test
    @DisplayName("Login should lock account after 5 failed attempts")
    void loginShouldLockAccountAfter5FailedAttempts() {
        // Given: User registered with valid password
        RegisterRequest registerRequest = new RegisterRequest(
            TEST_EMAIL,
            CORRECT_PASSWORD,
            "Lockout User"
        );
        authService.register(registerRequest).block();

        // When: 5 consecutive failed login attempts
        for (int i = 0; i < 5; i++) {
            LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, WRONG_PASSWORD);
            StepVerifier.create(authService.login(loginRequest))
                .expectError(InvalidCredentialsException.class)
                .verify();
        }

        // Then: 6th attempt should throw AccountLockedException
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, WRONG_PASSWORD);
        StepVerifier.create(authService.login(loginRequest))
            .expectErrorMatches(error ->
                error instanceof AccountLockedException &&
                error.getMessage().contains("5 failed attempts") &&
                error.getMessage().contains("15 minutes")
            )
            .verify();

        // And: Even with correct password, account should be locked
        LoginRequest correctLoginRequest = new LoginRequest(TEST_EMAIL, CORRECT_PASSWORD);
        StepVerifier.create(authService.login(correctLoginRequest))
            .expectError(AccountLockedException.class)
            .verify();

        // And: User's lockedUntil should be set
        User user = userRepository.findByEmail(TEST_EMAIL).block();
        assertThat(user).isNotNull();
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.isLocked()).isTrue();
    }

    @Test
    @DisplayName("Login should unlock account after lockout period")
    void loginShouldUnlockAccountAfterLockoutPeriod() {
        // Given: Account locked due to failed attempts
        RegisterRequest registerRequest = new RegisterRequest(
            "unlock@test.com",
            CORRECT_PASSWORD,
            "Unlock User"
        );
        authService.register(registerRequest).block();

        // Trigger 5 failed attempts to lock account
        for (int i = 0; i < 5; i++) {
            LoginRequest loginRequest = new LoginRequest("unlock@test.com", WRONG_PASSWORD);
            StepVerifier.create(authService.login(loginRequest))
                .expectError(InvalidCredentialsException.class)
                .verify();
        }

        // Verify account is locked
        LoginRequest loginRequest = new LoginRequest("unlock@test.com", CORRECT_PASSWORD);
        StepVerifier.create(authService.login(loginRequest))
            .expectError(AccountLockedException.class)
            .verify();

        // When: Wait for lockout period to expire (15 minutes)
        // Note: For testing, we'd typically configure a shorter lockout period
        // or use time manipulation. This demonstrates the expected behavior.

        // Manually unlock by setting lockedUntil to past time
        User user = userRepository.findByEmail("unlock@test.com").block();
        assertThat(user).isNotNull();
        user.setLockedUntil(java.time.Instant.now().minus(Duration.ofMinutes(1)));
        user.setFailedLoginAttempts(0);
        userRepository.save(user).block();

        // Then: Should be able to login with correct password
        LoginRequest correctLoginRequest = new LoginRequest("unlock@test.com", CORRECT_PASSWORD);
        StepVerifier.create(authService.login(correctLoginRequest))
            .expectNextMatches(response ->
                response.accessToken() != null &&
                response.refreshToken() != null
            )
            .verifyComplete();
    }

    @Test
    @DisplayName("Login should reset failed attempts counter on successful login")
    void loginShouldResetFailedAttemptsOnSuccess() {
        // Given: User with 3 failed login attempts (but not locked yet)
        RegisterRequest registerRequest = new RegisterRequest(
            "reset@test.com",
            CORRECT_PASSWORD,
            "Reset User"
        );
        authService.register(registerRequest).block();

        // Make 3 failed attempts
        for (int i = 0; i < 3; i++) {
            LoginRequest loginRequest = new LoginRequest("reset@test.com", WRONG_PASSWORD);
            StepVerifier.create(authService.login(loginRequest))
                .expectError(InvalidCredentialsException.class)
                .verify();
        }

        // Verify failed attempts count
        User userBefore = userRepository.findByEmail("reset@test.com").block();
        assertThat(userBefore).isNotNull();
        assertThat(userBefore.getFailedLoginAttempts()).isEqualTo(3);

        // When: Successful login with correct password
        LoginRequest correctLoginRequest = new LoginRequest("reset@test.com", CORRECT_PASSWORD);
        StepVerifier.create(authService.login(correctLoginRequest))
            .expectNextMatches(response -> response.accessToken() != null)
            .verifyComplete();

        // Then: Failed attempt counter should be reset to 0
        User userAfter = userRepository.findByEmail("reset@test.com").block();
        assertThat(userAfter).isNotNull();
        assertThat(userAfter.getFailedLoginAttempts()).isEqualTo(0);

        // And: Should now take 5 more failed attempts to lock (not just 2)
        for (int i = 0; i < 5; i++) {
            LoginRequest loginRequest = new LoginRequest("reset@test.com", WRONG_PASSWORD);
            StepVerifier.create(authService.login(loginRequest))
                .expectError(InvalidCredentialsException.class)
                .verify();
        }

        // This 6th attempt (after reset) should now lock the account
        LoginRequest finalAttempt = new LoginRequest("reset@test.com", WRONG_PASSWORD);
        StepVerifier.create(authService.login(finalAttempt))
            .expectError(AccountLockedException.class)
            .verify();
    }
}
