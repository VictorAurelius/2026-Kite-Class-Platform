package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.dto.LoginRequest;
import com.kitehub.subscription.exception.AccountLockedException;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.UserRepository;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService} account-lockout policy (GAP-515).
 *
 * <p>Covers:
 * <ul>
 *   <li>Wrong password increments {@code failedLoginAttempts}.</li>
 *   <li>5th wrong password sets {@code lockedUntil} (15 min, lockoutCount=1).</li>
 *   <li>Subsequent attempt while locked throws {@link AccountLockedException}
 *       BEFORE comparing the password.</li>
 *   <li>Successful login resets the failure counter but preserves
 *       {@code lockoutCount} for backoff history.</li>
 *   <li>Exponential backoff: 2nd lockout = 1h, 3rd = 24h.</li>
 * </ul>
 *
 * @since 1.0.0 (Wave 72a GAP-515)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService — Account Lockout (GAP-515)")
class AuthServiceLockoutTest {

    @Mock InstanceRepository instanceRepository;
    @Mock InstanceService instanceService;
    @Mock UserRepository userRepository;
    @Mock CaptchaService captchaService;
    @Mock EmailSenderService emailSenderService;
    @Mock JwtKeyService jwtKeyService;

    AuthService service;
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    User user;

    private static final String CORRECT_PASSWORD = "CorrectHorseBatteryStaple-2026";
    private static final String WRONG_PASSWORD = "guess-wrong";

    @BeforeEach
    void setUp() {
        service = new AuthService(
            instanceRepository, instanceService, userRepository,
            captchaService, emailSenderService, jwtKeyService
        );
        // Bypass @PostConstruct's jwtSecret check — we only test the lockout path.
        ReflectionTestUtils.setField(service, "jwtSecret", "x".repeat(32));

        // signing key only used on success path; provide a real one so token-gen doesn't NPE.
        SecretKey key = Keys.hmacShaKeyFor("x".repeat(64).getBytes(StandardCharsets.UTF_8));
        when(jwtKeyService.signingKey()).thenReturn(key);

        user = User.builder()
            .id(UUID.randomUUID())
            .email("user@example.test")
            .name("Tester")
            .passwordHash(encoder.encode(CORRECT_PASSWORD))
            .role("OWNER")
            .emailVerified(true)
            .build();

        when(userRepository.findByEmail("user@example.test")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(instanceService.getInstancesByOwner(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("wrong password increments failedLoginAttempts")
    void wrongPasswordIncrementsCounter() {
        attemptLogin(WRONG_PASSWORD); // 1st failure
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();

        attemptLogin(WRONG_PASSWORD); // 2nd failure
        assertThat(user.getFailedLoginAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("5 wrong passwords trigger lockout (15 min, lockoutCount=1)")
    void fiveWrongPasswordsLockAccount() {
        for (int i = 0; i < 5; i++) {
            attemptLogin(WRONG_PASSWORD);
        }
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.getLockoutCount()).isEqualTo(1);
        assertThat(user.getLockedUntil())
            .isAfter(LocalDateTime.now().plusMinutes(14))
            .isBefore(LocalDateTime.now().plusMinutes(16));
        // Failure counter reset; lockedUntil is now the gate.
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    @DisplayName("login while locked throws AccountLockedException without password compare")
    void lockedAccountRejectsAttempt() {
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        assertThatExceptionOfType(AccountLockedException.class)
            .isThrownBy(() -> service.login(loginRequest(CORRECT_PASSWORD)))
            .extracting(AccountLockedException::getLockedUntil)
            .isNotNull();
    }

    @Test
    @DisplayName("successful login clears failedLoginAttempts but preserves lockoutCount")
    void successResetsCounterPreservesHistory() {
        user.setFailedLoginAttempts(3);
        user.setLastFailedLoginAt(LocalDateTime.now().minusMinutes(1));
        user.setLockoutCount(2); // hist: previously locked twice

        var resp = service.login(loginRequest(CORRECT_PASSWORD));
        assertThat(resp).isNotNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLastFailedLoginAt()).isNull();
        // History preserved so next breach uses 24h backoff.
        assertThat(user.getLockoutCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("exponential backoff: 2nd lockout = 1 hour")
    void secondLockoutIsOneHour() {
        user.setLockoutCount(1); // already locked once before

        for (int i = 0; i < 5; i++) {
            attemptLogin(WRONG_PASSWORD);
        }
        assertThat(user.getLockedUntil())
            .isAfter(LocalDateTime.now().plusMinutes(55))
            .isBefore(LocalDateTime.now().plusMinutes(65));
        assertThat(user.getLockoutCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("exponential backoff: 3rd+ lockout = 24 hours (capped)")
    void thirdLockoutIs24Hours() {
        user.setLockoutCount(2);

        for (int i = 0; i < 5; i++) {
            attemptLogin(WRONG_PASSWORD);
        }
        assertThat(user.getLockedUntil())
            .isAfter(LocalDateTime.now().plusHours(23))
            .isBefore(LocalDateTime.now().plusHours(25));
        assertThat(user.getLockoutCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("failures outside the rolling window reset the counter")
    void staleFailuresDoNotCompound() {
        // Inject a stale failure history (~30 min ago) — outside the 15 min window.
        user.setFailedLoginAttempts(4);
        user.setLastFailedLoginAt(LocalDateTime.now().minusMinutes(30));

        attemptLogin(WRONG_PASSWORD);

        // Counter reset to 1 (fresh window starts), no lockout yet.
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
    }

    /* helpers */

    private void attemptLogin(String password) {
        assertThatThrownBy(() -> service.login(loginRequest(password)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private LoginRequest loginRequest(String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail("user@example.test");
        r.setPassword(password);
        return r;
    }
}
