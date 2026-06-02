package com.kiteclass.core.common.idempotency;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Producer-side idempotency guard for outbound email-dispatch events (GAP-840 sister
 * path of GAP-580).
 *
 * <p><strong>Why this exists:</strong> {@code ClassRescheduledEmailConsumer} subscribes
 * to RabbitMQ queue {@code class.rescheduled.queue} (at-least-once delivery) and
 * forwards the payload to {@code class.rescheduled.email.queue} via
 * {@code rabbitTemplate.convertAndSend(...)}. If this listener crashes (OOM) or throws
 * AFTER the broker handoff but BEFORE the inbound ack, RabbitMQ redelivers the
 * inbound {@code ClassRescheduledEvent} and the consumer publishes the same forward
 * twice — producing a duplicate email at the downstream kitehub-email service.</p>
 *
 * <p><strong>Why it isn't covered by the kitehub-email consumer guard:</strong>
 * GAP-580's guard lives on the {@code email.send} queue's consumer (different queue,
 * different payload shape). The {@code class.rescheduled.email.queue} is a separate
 * pipeline; downstream kitehub-email rehydrates each forwarded payload independently
 * and has no view of the kiteclass-core inbound dedup boundary.</p>
 *
 * <p><strong>Design (mirrors kitehub-email guard for shipping symmetry):</strong>
 * backed by Redis {@link StringRedisTemplate} {@code SETNX} ({@code setIfAbsent}
 * with TTL) when the Spring Data Redis bean is wired — atomic check-and-set across
 * processes AND across container restarts. Falls back to an in-process Caffeine cache
 * when Redis is not available (unit tests, environments without a Redis dep) so the
 * guard keeps a defensive single-process safety net rather than dropping protection
 * silently.</p>
 *
 * <p><strong>Coverage:</strong></p>
 * <ul>
 *   <li>✅ Concurrent inbound redelivery + same-process Spring-listener retry
 *       (Redis SETNX is atomic; Caffeine {@code putIfAbsent} is also atomic).</li>
 *   <li>✅ Cross-restart (Redis path): same logical {@code ClassRescheduledEvent}
 *       redelivered AFTER {@code kiteclass-core} restart still maps to the same key
 *       in {@code kite-redis} → second forward suppressed → downstream kitehub-email
 *       never sees the duplicate.</li>
 *   <li>⚠️ Caffeine fallback path is in-process only — defense in depth when Redis is
 *       briefly unreachable. Fail-open semantics keep legitimate forwards from being
 *       dropped.</li>
 * </ul>
 *
 * <p><strong>Key namespace:</strong> {@code class-reschedule:idempotency:<sha256>} —
 * prefix avoids collisions with {@code email:idempotency:*} keys written by the
 * kitehub-email consumer guard (shared {@code kite-redis} instance in local dev).</p>
 *
 * <p>Pattern intentionally mirrors {@code com.kitehub.email.service.EmailIdempotencyGuard}
 * — no shared module exists between kiteclass-core and kitehub-email, so the dedup
 * primitive is duplicated rather than extracted. If a shared module ships later, both
 * copies converge.</p>
 *
 * @since GAP-840 (Wave local-doable-6 Bucket H — sister send path dedup)
 */
@Slf4j
@Component
public class EmailIdempotencyGuard {

    /** Redis key namespace prefix — distinct from kitehub-email's {@code email:idempotency:*}. */
    static final String KEY_PREFIX = "class-reschedule:idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final Cache<String, Boolean> caffeineFallback;
    private final Duration ttl;

    /**
     * Spring-injected constructor. Uses an {@link ObjectProvider} so the
     * {@link StringRedisTemplate} dependency is OPTIONAL — when Redis is not wired,
     * the guard falls back to in-process Caffeine.
     */
    @Autowired
    public EmailIdempotencyGuard(
            @Value("${kiteclass.email.idempotency.ttl-minutes:60}") long ttlMinutes,
            @Value("${kiteclass.email.idempotency.caffeine-max-size:50000}") long caffeineMaxSize,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this(ttlMinutes, caffeineMaxSize,
                redisTemplateProvider == null ? null : redisTemplateProvider.getIfAvailable());
    }

    /**
     * Direct-injection constructor — used by Testcontainers integration tests.
     */
    public EmailIdempotencyGuard(long ttlMinutes, long caffeineMaxSize, StringRedisTemplate redisTemplate) {
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.redisTemplate = redisTemplate;
        this.caffeineFallback = Caffeine.newBuilder()
                .expireAfterWrite(this.ttl)
                .maximumSize(caffeineMaxSize)
                .build();
        log.info("EmailIdempotencyGuard (kiteclass-core) initialised: ttl={}min caffeineFallbackMax={} redis={}",
                ttlMinutes, caffeineMaxSize, redisTemplate != null ? "ENABLED" : "DISABLED (Caffeine-only)");
    }

    /** Unit-test convenience overload — no Redis, in-process Caffeine only. */
    public EmailIdempotencyGuard(long ttlMinutes, long caffeineMaxSize) {
        this(ttlMinutes, caffeineMaxSize, (StringRedisTemplate) null);
    }

    /**
     * Mark the given idempotency key as seen, returning whether this is the FIRST
     * time. Identical contract to {@code com.kitehub.email.service.EmailIdempotencyGuard.markIfFirstSeen}.
     *
     * @param idempotencyKey deterministic key for the forward (never {@code null}/blank)
     * @return {@code true} if first-seen (proceed with forward); {@code false} if
     *         already seen (skip — duplicate)
     */
    public boolean markIfFirstSeen(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true;
        }

        if (redisTemplate != null) {
            try {
                String namespacedKey = KEY_PREFIX + idempotencyKey;
                Boolean firstSeenRedis = redisTemplate.opsForValue()
                        .setIfAbsent(namespacedKey, "1", ttl);
                if (firstSeenRedis == null) {
                    log.warn("Redis SETNX returned null for key={} — failing open (proceed)", idempotencyKey);
                    return true;
                }
                if (!firstSeenRedis) {
                    log.info("Idempotent skip — duplicate class.rescheduled forward suppressed via Redis (key={})",
                            idempotencyKey);
                }
                return firstSeenRedis;
            } catch (Exception ex) {
                log.warn("Redis SETNX failed for key={} — falling back to Caffeine: {}",
                        idempotencyKey, ex.getMessage());
            }
        }

        Boolean previous = caffeineFallback.asMap().putIfAbsent(idempotencyKey, Boolean.TRUE);
        boolean firstSeen = (previous == null);
        if (!firstSeen) {
            log.info("Idempotent skip — duplicate class.rescheduled forward suppressed via Caffeine fallback (key={})",
                    idempotencyKey);
        }
        return firstSeen;
    }

    /**
     * Compute a deterministic idempotency key for a {@code ClassRescheduledEvent}
     * forward. Key derived from classId + rescheduledAt + sorted recipient set.
     *
     * @param classId          class identifier (nullable handled defensively)
     * @param rescheduledAt    reschedule timestamp (Instant — second-precision OK)
     * @param recipientListKey stable string capturing recipient set
     * @return non-null, non-blank deterministic SHA-256 hex key
     */
    public String computeKey(Long classId, Instant rescheduledAt, String recipientListKey) {
        StringBuilder material = new StringBuilder();
        material.append(classId == null ? "" : classId.toString()).append('|')
                .append(rescheduledAt == null ? "" : rescheduledAt.toString()).append('|')
                .append(recipientListKey == null ? "" : recipientListKey);
        return sha256(material.toString());
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
