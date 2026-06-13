package com.kitehub.subscription.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed one-time exchange-code store for cross-product SSO
 * KiteHub → KiteClass (ADR-040 Option A, GAP-1138).
 *
 * <p>Owner/Staff authenticate at KiteHub ({@code :3001}). To enter the KiteClass
 * owner-shell ({@code :3000}) without re-login, KiteHub mints a short-lived
 * single-use opaque code bound to the user's identity, redirects the browser to
 * KiteClass with the code in the URL, and KiteClass exchanges the code for a real
 * KiteHub-minted JWT (gateway-validatable via the shared {@code JWT_SECRET}, per
 * ADR-039). Only the opaque code — never the JWT — travels in the URL.</p>
 *
 * <p><strong>Security properties:</strong></p>
 * <ul>
 *   <li><b>Single-use</b> — {@link #consumeCode} uses Redis {@code GETDEL}
 *       ({@code ValueOperations#getAndDelete}) so the read + delete are atomic.
 *       A replayed code finds nothing on the second attempt → rejected.</li>
 *   <li><b>Short TTL</b> — codes self-expire after {@code kitehub.sso.code-ttl-seconds}
 *       (clamped to ≤60s per ADR-040). An intercepted-but-unused code is worthless
 *       within a minute.</li>
 *   <li><b>High entropy</b> — 256-bit {@link SecureRandom} value, URL-safe Base64.</li>
 *   <li><b>No raw JWT stored</b> — only the issuing user's identity tuple
 *       (userId, email, role). The JWT is re-minted fresh at exchange time so
 *       tenant + tier claims reflect current DB state.</li>
 * </ul>
 *
 * <p>Unlike {@link RefreshTokenBlacklistService} this store is NOT fail-open:
 * a Redis outage means issue/exchange fail loudly (the SSO convenience path is
 * unavailable) rather than silently minting unverifiable sessions — owner/staff
 * can still use the KiteClass dual-path fallback login (per ADR-040 §Beta unblock).</p>
 *
 * @since GAP-1138 (Wave RBAC-SSO 1)
 */
@Service
@Slf4j
public class SsoCodeService {

    /** Redis key namespace for one-time SSO exchange codes. */
    static final String KEY_PREFIX = "sso-exchange-code:";

    /** Field separator (ASCII unit separator) — never present in email/role/UUID. */
    private static final String SEP = "\u001f";

    /** Code entropy in bytes (256-bit). */
    private static final int CODE_BYTES = 32;

    /** Hard upper bound on TTL per ADR-040 (≤60s single-use). */
    private static final long MAX_TTL_SECONDS = 60L;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public SsoCodeService(
        StringRedisTemplate redis,
        @Value("${kitehub.sso.code-ttl-seconds:60}") long ttlSeconds
    ) {
        this.redis = redis;
        long clamped = Math.max(1L, Math.min(ttlSeconds, MAX_TTL_SECONDS));
        if (clamped != ttlSeconds) {
            log.warn("kitehub.sso.code-ttl-seconds={} clamped to {} (ADR-040 mandates 1..{}s)",
                ttlSeconds, clamped, MAX_TTL_SECONDS);
        }
        this.ttl = Duration.ofSeconds(clamped);
    }

    /**
     * Mint a one-time exchange code bound to the authenticated KiteHub user.
     *
     * @param userId issuing user's UUID (JWT subject) — required
     * @param email  user email (echoed back at exchange) — may be null/blank
     * @param role   user role (re-minted into the exchanged JWT) — may be null/blank
     * @return the opaque code to place in the KiteClass callback URL
     */
    public String issueCode(UUID userId, String email, String role) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required to issue an SSO code");
        }
        String code = generateCode();
        String value = userId + SEP + nullToEmpty(email) + SEP + nullToEmpty(role);
        redis.opsForValue().set(KEY_PREFIX + code, value, ttl);
        log.info("Issued SSO exchange code for userId={} (ttl={}s)", userId, ttl.getSeconds());
        return code;
    }

    /**
     * Atomically consume (read + delete) a one-time code. Single-use: a second
     * call with the same code returns {@link Optional#empty()} because the key is
     * deleted on the first read (Redis {@code GETDEL}).
     *
     * @param code the opaque code from the KiteClass callback
     * @return the bound principal, or empty if the code is missing / expired / already used
     */
    public Optional<SsoPrincipal> consumeCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String value = redis.opsForValue().getAndDelete(KEY_PREFIX + code);
        if (value == null) {
            return Optional.empty();
        }
        String[] parts = value.split(SEP, -1);
        if (parts.length != 3) {
            log.warn("Malformed SSO code payload (unexpected field count) — rejecting");
            return Optional.empty();
        }
        UUID userId;
        try {
            userId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException ex) {
            log.warn("Malformed SSO code payload (bad userId) — rejecting");
            return Optional.empty();
        }
        return Optional.of(new SsoPrincipal(userId, emptyToNull(parts[1]), emptyToNull(parts[2])));
    }

    /** @return configured TTL in seconds (post-clamp) for the issue-code response. */
    public long ttlSeconds() {
        return ttl.getSeconds();
    }

    private static String generateCode() {
        byte[] bytes = new byte[CODE_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    /**
     * Identity tuple bound to a one-time code. Re-minted into a fresh JWT at
     * exchange time (tenant + tier claims resolved from current DB state).
     */
    public record SsoPrincipal(UUID userId, String email, String role) {
    }
}
