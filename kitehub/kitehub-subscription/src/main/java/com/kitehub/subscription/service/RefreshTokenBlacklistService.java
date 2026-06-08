package com.kitehub.subscription.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Redis-backed refresh-token blacklist (GAP-1075).
 *
 * <p>Design-canonical per {@code documents/02-architecture/service-catalog-and-auth-flow.md}
 * (Redis = "refresh-token blacklist + session"). On logout the refresh token's SHA-256 hash
 * is stored under key {@code refresh-blacklist:<hash>} with a TTL equal to the token's
 * remaining lifetime, so the entry self-expires exactly when the token would have expired —
 * Redis never accumulates dead keys. The raw token is never persisted, only its hash.</p>
 *
 * <p><strong>Fail-open</strong>: every Redis call is wrapped so a Redis outage never breaks
 * logout or refresh. The trade-off is availability over absolute revocation — during a Redis
 * outage a revoked token may still refresh. This matches the stateless-JWT model and the
 * "best-effort revocation" contract documented in the frontend {@code authApi.logout}.
 * Failures are logged at WARN for alerting.</p>
 *
 * <p>SHA-256 here derives a fixed-length opaque key from an already high-entropy JWT; it is
 * NOT password hashing, so {@code pre-launch-owasp-rest-hardening-checklist.md} §2.2 (which
 * bans MD5/SHA-1 for credential hashing) does not apply.</p>
 */
@Service
@Slf4j
public class RefreshTokenBlacklistService {

    /** Redis key namespace for blacklisted refresh tokens. */
    static final String KEY_PREFIX = "refresh-blacklist:";

    private final StringRedisTemplate redis;

    public RefreshTokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Blacklist a refresh token for the remainder of its lifetime.
     *
     * @param refreshToken the raw refresh-token string (never stored — only its hash)
     * @param ttl          remaining lifetime; a non-positive value is clamped to 1s so an
     *                     already-near-expiry token still blocks an immediate replay
     */
    public void blacklist(String refreshToken, Duration ttl) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        Duration effective = (ttl == null || ttl.isNegative() || ttl.isZero())
            ? Duration.ofSeconds(1)
            : ttl;
        try {
            redis.opsForValue().set(key(refreshToken), "1", effective);
        } catch (Exception ex) {
            // Fail-open: never let a Redis outage break logout.
            log.warn("Refresh-token blacklist write failed (fail-open): {}", ex.getMessage());
        }
    }

    /**
     * @return {@code true} if the token was previously blacklisted; {@code false} on a clean
     *         token OR any Redis error (fail-open).
     */
    public boolean isBlacklisted(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(key(refreshToken)));
        } catch (Exception ex) {
            log.warn("Refresh-token blacklist read failed (fail-open): {}", ex.getMessage());
            return false;
        }
    }

    private String key(String refreshToken) {
        return KEY_PREFIX + sha256(refreshToken);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated present on every JVM — unreachable in practice.
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
