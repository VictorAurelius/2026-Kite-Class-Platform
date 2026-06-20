package com.kitehub.subscription.auth.otp;

import com.kitehub.subscription.auth.otp.OtpService.OtpRequestResult;
import com.kitehub.subscription.auth.otp.OtpService.OtpVerifyResult;
import com.kitehub.subscription.auth.otp.OtpService.VerifyFailureReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link OtpService} (GAP-286). No Spring context — a controllable
 * {@link MutableClock} drives TTL/rate-limit windows and a capturing delivery stub
 * exposes the generated code. Mirrors the {@code passwordreset}/{@code twofactor}
 * service test style (AssertJ, in-memory doubles).
 */
@DisplayName("OtpService — request / verify / rate-limit")
class OtpServiceTest {

    private static final String PHONE = "0901234567";
    private static final long CODE_TTL = 300;       // 5 min
    private static final int MAX_ATTEMPTS = 5;
    private static final int RATE_MAX = 3;
    private static final long RATE_WINDOW = 15 * 60; // 15 min

    private CapturingDelivery delivery;
    private MutableClock clock;
    private OtpService svc;

    @BeforeEach
    void setUp() {
        delivery = new CapturingDelivery();
        SignupTokenService tokenSvc =
            new SignupTokenService("test-signup-secret-32-chars-aaaaaaaa", new MockEnvironment());
        clock = new MutableClock(Instant.parse("2026-06-21T00:00:00Z"));
        svc = new OtpService(delivery, tokenSvc, CODE_TTL, MAX_ATTEMPTS, RATE_MAX, RATE_WINDOW, clock);
    }

    @Test
    @DisplayName("requestOtp issues a code, dispatches via mock channel (default ZALO), 5-min TTL")
    void requestOtp_issues() {
        OtpRequestResult result = svc.requestOtp(PHONE, null);

        assertThat(result.rateLimited()).isFalse();
        assertThat(result.requestId()).isNotBlank();
        assertThat(result.channel()).isEqualTo("ZALO");
        assertThat(result.expiresInSeconds()).isEqualTo(CODE_TTL);
        assertThat(result.mock()).isTrue();
        assertThat(delivery.lastPhone).isEqualTo(PHONE);
        assertThat(delivery.lastCode).matches("\\d{6}");
    }

    @Test
    @DisplayName("requestOtp rejects a phone failing ^0\\d{9,10}$")
    void requestOtp_invalidPhone() {
        assertThatExceptionOfType(OtpService.InvalidPhoneException.class)
            .isThrownBy(() -> svc.requestOtp("12345", "ZALO"));
    }

    @Test
    @DisplayName("verifyOtp happy path returns verified + a non-blank signup token; single-use")
    void verifyOtp_happyPath() {
        svc.requestOtp(PHONE, "ZALO");

        OtpVerifyResult ok = svc.verifyOtp(PHONE, delivery.lastCode);
        assertThat(ok.verified()).isTrue();
        assertThat(ok.signupToken()).isNotBlank();

        // Single-use: re-verifying the same (now consumed) code fails.
        OtpVerifyResult again = svc.verifyOtp(PHONE, delivery.lastCode);
        assertThat(again.verified()).isFalse();
        assertThat(again.reason()).isEqualTo(VerifyFailureReason.INVALID_CODE);
    }

    @Test
    @DisplayName("verifyOtp returns EXPIRED once the code TTL has elapsed")
    void verifyOtp_expired() {
        svc.requestOtp(PHONE, "ZALO");
        clock.advance(Duration.ofSeconds(CODE_TTL + 1));

        OtpVerifyResult result = svc.verifyOtp(PHONE, delivery.lastCode);
        assertThat(result.verified()).isFalse();
        assertThat(result.reason()).isEqualTo(VerifyFailureReason.EXPIRED);
    }

    @Test
    @DisplayName("verifyOtp returns INVALID_CODE for a wrong code")
    void verifyOtp_wrongCode() {
        svc.requestOtp(PHONE, "ZALO");

        OtpVerifyResult result = svc.verifyOtp(PHONE, wrongCode(delivery.lastCode));
        assertThat(result.verified()).isFalse();
        assertThat(result.reason()).isEqualTo(VerifyFailureReason.INVALID_CODE);
    }

    @Test
    @DisplayName("requestOtp rate-limits the 4th request within the 15-min window → retryAfter > 0")
    void requestOtp_rateLimited() {
        for (int i = 0; i < RATE_MAX; i++) {
            assertThat(svc.requestOtp(PHONE, "ZALO").rateLimited()).isFalse();
        }
        OtpRequestResult blocked = svc.requestOtp(PHONE, "ZALO");
        assertThat(blocked.rateLimited()).isTrue();
        assertThat(blocked.retryAfterSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("verifyOtp invalidates the code after max attempts: 6th wrong attempt → TOO_MANY_ATTEMPTS")
    void verifyOtp_maxAttempts() {
        svc.requestOtp(PHONE, "ZALO");
        String wrong = wrongCode(delivery.lastCode);

        // 5 allowed wrong attempts all report INVALID_CODE.
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThat(svc.verifyOtp(PHONE, wrong).reason())
                .isEqualTo(VerifyFailureReason.INVALID_CODE);
        }
        // 6th attempt is blocked — code invalidated.
        OtpVerifyResult sixth = svc.verifyOtp(PHONE, wrong);
        assertThat(sixth.verified()).isFalse();
        assertThat(sixth.reason()).isEqualTo(VerifyFailureReason.TOO_MANY_ATTEMPTS);
    }

    // ── helpers ──

    private static String wrongCode(String actual) {
        return "000000".equals(actual) ? "111111" : "000000";
    }

    /** Capturing stub — records the plaintext code the service tried to deliver. */
    static final class CapturingDelivery extends OtpDeliveryService {
        String lastPhone;
        String lastCode;
        String lastChannel;

        CapturingDelivery() {
            super(true);
        }

        @Override
        public boolean deliver(String phone, String code, String channel) {
            this.lastPhone = phone;
            this.lastCode = code;
            this.lastChannel = channel;
            return true;
        }
    }

    /** Manually-advanced clock so TTL/window tests need no {@code Thread.sleep}. */
    static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration d) {
            this.instant = this.instant.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
