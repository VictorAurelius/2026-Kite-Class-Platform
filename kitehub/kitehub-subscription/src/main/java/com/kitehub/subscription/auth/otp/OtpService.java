package com.kitehub.subscription.auth.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phone-OTP orchestration for mobile signup (GAP-286).
 *
 * <p>Two-step flow mirroring the {@code passwordreset} package (generate code →
 * deliver → verify → rate-limit):</p>
 * <ol>
 *   <li>{@link #requestOtp(String, String)} — validate phone, enforce the
 *       request rate-limit, mint a 6-digit code (stored HASHED, never plaintext),
 *       dispatch via {@link OtpDeliveryService} (mock → logs the code at INFO).</li>
 *   <li>{@link #verifyOtp(String, String)} — check the code against the hash,
 *       counting attempts; on success issue a short-lived signup token proving
 *       phone ownership ({@link SignupTokenService}).</li>
 * </ol>
 *
 * <p><strong>Store:</strong> in-memory {@link ConcurrentHashMap} with lazy TTL
 * eviction — adequate for Phase 1. <em>TODO Phase 2: back with Redis</em> (native
 * TTL + survives multi-instance + restart).</p>
 *
 * <p>Config keys (all under {@code kitehub.auth.signup-otp}, sensible defaults):
 * {@code code-ttl-seconds} (300), {@code max-verify-attempts} (5),
 * {@code rate-limit-max-requests} (3), {@code rate-limit-window-minutes} (15).</p>
 *
 * @since GAP-286 (mobile signup OTP)
 */
@Service
@Slf4j
public class OtpService {

    /** VN phone number: leading 0 followed by 9-10 digits. */
    static final String PHONE_REGEX = "^0\\d{9,10}$";

    private static final int CODE_BOUND = 1_000_000; // 6-digit space 000000..999999
    private static final String DEFAULT_CHANNEL = "ZALO";

    /** Reasons a verify attempt failed. Mapped to the {@code reason} response field. */
    public enum VerifyFailureReason { INVALID_CODE, EXPIRED, TOO_MANY_ATTEMPTS }

    private final OtpDeliveryService deliveryService;
    private final SignupTokenService signupTokenService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom rng = new SecureRandom();
    private final Clock clock;

    private final long codeTtlSeconds;
    private final int maxVerifyAttempts;
    private final int rateLimitMaxRequests;
    private final long rateLimitWindowSeconds;

    // Pluggable store — TODO Phase 2: back with Redis.
    private final ConcurrentHashMap<String, OtpEntry> store = new ConcurrentHashMap<>();
    // Sliding-window request log per phone for rate-limiting.
    private final ConcurrentHashMap<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();

    @Autowired
    public OtpService(
        OtpDeliveryService deliveryService,
        SignupTokenService signupTokenService,
        @Value("${kitehub.auth.signup-otp.code-ttl-seconds:300}") long codeTtlSeconds,
        @Value("${kitehub.auth.signup-otp.max-verify-attempts:5}") int maxVerifyAttempts,
        @Value("${kitehub.auth.signup-otp.rate-limit-max-requests:3}") int rateLimitMaxRequests,
        @Value("${kitehub.auth.signup-otp.rate-limit-window-minutes:15}") long rateLimitWindowMinutes) {
        this(deliveryService, signupTokenService, codeTtlSeconds, maxVerifyAttempts,
            rateLimitMaxRequests, rateLimitWindowMinutes * 60L, Clock.systemUTC());
    }

    /** Test seam — injects a controllable {@link Clock} + raw window in seconds. */
    OtpService(OtpDeliveryService deliveryService,
               SignupTokenService signupTokenService,
               long codeTtlSeconds,
               int maxVerifyAttempts,
               int rateLimitMaxRequests,
               long rateLimitWindowSeconds,
               Clock clock) {
        this.deliveryService = deliveryService;
        this.signupTokenService = signupTokenService;
        this.codeTtlSeconds = codeTtlSeconds;
        this.maxVerifyAttempts = maxVerifyAttempts;
        this.rateLimitMaxRequests = rateLimitMaxRequests;
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
        this.clock = clock;
    }

    /**
     * Request an OTP for the given phone.
     *
     * @param phone   VN phone ({@value #PHONE_REGEX})
     * @param channel delivery channel (nullable → defaults to {@value #DEFAULT_CHANNEL})
     * @return {@link OtpRequestResult#issued} on success, or
     *         {@link OtpRequestResult#rateLimited} when the per-phone limit is exceeded
     * @throws InvalidPhoneException if the phone fails {@value #PHONE_REGEX}
     */
    public OtpRequestResult requestOtp(String phone, String channel) {
        if (phone == null || !phone.matches(PHONE_REGEX)) {
            throw new InvalidPhoneException("phone must match " + PHONE_REGEX);
        }
        String resolvedChannel = (channel == null || channel.isBlank())
            ? DEFAULT_CHANNEL : channel.trim().toUpperCase();

        Instant now = clock.instant();
        sweepExpired(now); // lazy housekeeping — TODO Phase 2: Redis native TTL

        Deque<Instant> times = requestLog.computeIfAbsent(phone, k -> new ArrayDeque<>());
        synchronized (times) {
            pruneOld(times, now);
            if (times.size() >= rateLimitMaxRequests) {
                long retryAfter = retryAfterSeconds(times, now);
                log.warn("OTP request rate-limited for {} ({} requests in window)",
                    masked(phone), times.size());
                return OtpRequestResult.rateLimited(retryAfter);
            }
            times.addLast(now);
        }

        String code = generateCode();
        OtpEntry entry = new OtpEntry(encoder.encode(code), now.plusSeconds(codeTtlSeconds));
        store.put(phone, entry);

        boolean mock = deliveryService.deliver(phone, code, resolvedChannel);
        String requestId = UUID.randomUUID().toString();
        log.info("OTP issued for {} (channel={}, requestId={}, ttl={}s)",
            masked(phone), resolvedChannel, requestId, codeTtlSeconds);
        return OtpRequestResult.issued(requestId, resolvedChannel, codeTtlSeconds, mock);
    }

    /**
     * Verify an OTP for the given phone.
     *
     * @return {@link OtpVerifyResult#success} (with a signup token) when the code matches,
     *         else {@link OtpVerifyResult#failure} with the {@link VerifyFailureReason}
     */
    public OtpVerifyResult verifyOtp(String phone, String code) {
        OtpEntry entry = (phone == null) ? null : store.get(phone);
        if (entry == null || code == null) {
            return OtpVerifyResult.failure(VerifyFailureReason.INVALID_CODE);
        }
        Instant now = clock.instant();
        synchronized (entry) {
            if (now.isAfter(entry.expiresAt)) {
                store.remove(phone, entry);
                return OtpVerifyResult.failure(VerifyFailureReason.EXPIRED);
            }
            if (entry.attempts >= maxVerifyAttempts) {
                // Code already exhausted — keep the (now-dead) entry so the reason
                // stays sticky until TTL eviction.
                return OtpVerifyResult.failure(VerifyFailureReason.TOO_MANY_ATTEMPTS);
            }
            entry.attempts++;
            if (encoder.matches(code, entry.codeHash)) {
                store.remove(phone, entry); // single-use
                String signupToken = signupTokenService.issue(phone);
                log.info("OTP verified for {} — signup token issued", masked(phone));
                return OtpVerifyResult.success(signupToken);
            }
            return OtpVerifyResult.failure(VerifyFailureReason.INVALID_CODE);
        }
    }

    // ── helpers ──

    private String generateCode() {
        return String.format("%06d", rng.nextInt(CODE_BOUND));
    }

    private void pruneOld(Deque<Instant> times, Instant now) {
        Instant cutoff = now.minusSeconds(rateLimitWindowSeconds);
        while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
            times.pollFirst();
        }
    }

    private long retryAfterSeconds(Deque<Instant> times, Instant now) {
        Instant oldest = times.peekFirst();
        long secs = Duration.between(now, oldest.plusSeconds(rateLimitWindowSeconds)).getSeconds();
        return Math.max(secs, 1);
    }

    private void sweepExpired(Instant now) {
        for (Iterator<Map.Entry<String, OtpEntry>> it = store.entrySet().iterator(); it.hasNext(); ) {
            if (now.isAfter(it.next().getValue().expiresAt)) {
                it.remove();
            }
        }
    }

    private static String masked(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 3);
    }

    // ── value types ──

    /** Mutable in-memory OTP record. Guarded by {@code synchronized(entry)}. */
    private static final class OtpEntry {
        private final String codeHash;
        private final Instant expiresAt;
        private int attempts;

        OtpEntry(String codeHash, Instant expiresAt) {
            this.codeHash = codeHash;
            this.expiresAt = expiresAt;
        }
    }

    /** Result of {@link #requestOtp}. {@code rateLimited} distinguishes the 429 path. */
    public record OtpRequestResult(boolean rateLimited, String requestId, String channel,
                                   long expiresInSeconds, boolean mock, long retryAfterSeconds) {
        static OtpRequestResult issued(String requestId, String channel, long ttl, boolean mock) {
            return new OtpRequestResult(false, requestId, channel, ttl, mock, 0L);
        }

        static OtpRequestResult rateLimited(long retryAfterSeconds) {
            return new OtpRequestResult(true, null, null, 0L, false, retryAfterSeconds);
        }
    }

    /** Result of {@link #verifyOtp}. */
    public record OtpVerifyResult(boolean verified, String signupToken, VerifyFailureReason reason) {
        static OtpVerifyResult success(String signupToken) {
            return new OtpVerifyResult(true, signupToken, null);
        }

        static OtpVerifyResult failure(VerifyFailureReason reason) {
            return new OtpVerifyResult(false, null, reason);
        }
    }

    /** Phone fails {@value #PHONE_REGEX}. Maps to HTTP 400. */
    public static class InvalidPhoneException extends RuntimeException {
        public InvalidPhoneException(String message) {
            super(message);
        }
    }
}
