package com.kitehub.email.service;

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
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Consumer-side idempotency guard for the transactional email pipeline (GAP-580).
 *
 * <p><strong>Why this exists:</strong> RabbitMQ {@code email.send} delivery is
 * at-least-once. If {@code kitehub-email} crashes (OOM) or the listener throws
 * AFTER the provider send (Resend/SES) but BEFORE the broker ack, RabbitMQ
 * redelivers the same {@code EmailEvent} and the recipient receives a duplicate
 * email — trust damage for the beta cohort (Wave 85 Bucket A simulation cell 22).
 * Producer-side {@code EmailServiceClient.alreadySentToday} guards only the
 * <em>producer</em> publishing twice; it does NOT cover consumer redelivery,
 * because the event is already in the queue and the producer dedup never re-runs.</p>
 *
 * <p><strong>Design (post-Wave local-doable-5):</strong> backed by Redis
 * {@link StringRedisTemplate} {@code SETNX} ({@code setIfAbsent} with TTL) when the
 * Spring Data Redis bean is wired — atomic check-and-set across processes AND across
 * container restarts. Falls back to an in-process Caffeine cache when Redis is not
 * available (unit tests, environments without a Redis dep) so the guard keeps a
 * defensive single-process safety net rather than dropping protection silently.
 * Cross-restart idempotency was the lone unchecked AC carried forward from the
 * original GAP-580 Wave phase2-beta ship.</p>
 *
 * <p><strong>Coverage:</strong></p>
 * <ul>
 *   <li>✅ Concurrent redelivery + same-process Spring-listener retry (Redis SETNX
 *       is atomic; Caffeine {@code putIfAbsent} is also atomic).</li>
 *   <li>✅ Cross-restart (Redis path): the same logical {@code EmailEvent} replayed
 *       AFTER {@code kitehub-email} restart still maps to the same key in
 *       {@code kite-redis} (the broker survived too) → second send suppressed.</li>
 *   <li>⚠️ Caffeine fallback path is still in-process only — used as defense in depth
 *       when Redis is briefly unreachable. Fail-open semantics keep legitimate sends
 *       from being dropped.</li>
 * </ul>
 *
 * <p><strong>Key namespace:</strong> {@code email:idempotency:<sha256>} — prefix
 * avoids collisions with other Redis tenants (Wave 86 Bucket B branding cache shares
 * the same single-node container in local dev).</p>
 *
 * @since GAP-580 (Wave phase2-beta — consumer-side email idempotency, defense-in-depth)
 */
@Slf4j
@Component
public class EmailIdempotencyGuard {

    /** Redis key namespace prefix — avoids collisions with sibling Redis users. */
    static final String KEY_PREFIX = "email:idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final Cache<String, Boolean> caffeineFallback;
    private final Duration ttl;

    /**
     * Spring-injected constructor. Uses an {@link ObjectProvider} so the
     * {@link StringRedisTemplate} dependency is OPTIONAL — when Redis is not on the
     * classpath / not configured, the guard falls back to in-process Caffeine. A single
     * Spring-resolvable constructor avoids the multiple-public-constructor ambiguity
     * Spring Boot 3.x flags as {@code no default constructor found}.
     */
    @Autowired
    public EmailIdempotencyGuard(
            @Value("${kitehub.email.idempotency.ttl-minutes:60}") long ttlMinutes,
            @Value("${kitehub.email.idempotency.caffeine-max-size:50000}") long caffeineMaxSize,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this(ttlMinutes, caffeineMaxSize,
                redisTemplateProvider == null ? null : redisTemplateProvider.getIfAvailable());
    }

    /**
     * Direct-injection constructor — used by Testcontainers integration tests that need
     * to wire a known {@link StringRedisTemplate} (or {@code null} for Caffeine-only).
     * NOT auto-selected by Spring because the single-arg-{@link ObjectProvider} variant
     * above is the constructor it resolves at startup.
     */
    public EmailIdempotencyGuard(long ttlMinutes, long caffeineMaxSize, StringRedisTemplate redisTemplate) {
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.redisTemplate = redisTemplate;
        this.caffeineFallback = Caffeine.newBuilder()
                .expireAfterWrite(this.ttl)
                .maximumSize(caffeineMaxSize)
                .build();
        log.info("EmailIdempotencyGuard initialised: ttl={}min caffeineFallbackMax={} redis={}",
                ttlMinutes, caffeineMaxSize, redisTemplate != null ? "ENABLED" : "DISABLED (Caffeine-only)");
    }

    /**
     * Unit-test convenience overload — no Redis, in-process Caffeine only. Preserves the
     * pre-Wave-local-doable-5 public test API.
     */
    public EmailIdempotencyGuard(long ttlMinutes, long caffeineMaxSize) {
        this(ttlMinutes, caffeineMaxSize, (StringRedisTemplate) null);
    }

    /**
     * Mark the given idempotency key as seen, returning whether this is the FIRST time.
     *
     * <p>Redis path: {@code SETNX(KEY_PREFIX+key, "1", ttl)} — atomic across processes +
     * survives container restart while the key is in its TTL window. Returns {@code true}
     * on first set (proceed with send) and {@code false} when the key already exists.</p>
     *
     * <p>Caffeine fallback: invoked when Redis is not wired OR throws transiently.
     * In-process atomic {@code putIfAbsent}; same return semantics. Fail-open is intentional
     * — better to send a possible duplicate than drop a legitimate email.</p>
     *
     * @param idempotencyKey deterministic key for the send (never {@code null}/blank)
     * @return {@code true} if this key was not seen within the TTL window (proceed with send);
     *         {@code false} if already seen (skip the send — duplicate)
     */
    public boolean markIfFirstSeen(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            // No key → cannot dedup; fail-open (send) rather than drop a legitimate email.
            return true;
        }

        if (redisTemplate != null) {
            try {
                String namespacedKey = KEY_PREFIX + idempotencyKey;
                Boolean firstSeenRedis = redisTemplate.opsForValue()
                        .setIfAbsent(namespacedKey, "1", ttl);
                // Bean returns null only when running inside an active transaction without
                // a queued read-back, which is not our case. Treat null defensively as
                // "could not confirm" → fail-open (proceed with send).
                if (firstSeenRedis == null) {
                    log.warn("Redis SETNX returned null for key={} — failing open (proceed)", idempotencyKey);
                    return true;
                }
                if (!firstSeenRedis) {
                    log.info("Idempotent skip — duplicate email suppressed via Redis (key={})", idempotencyKey);
                }
                return firstSeenRedis;
            } catch (Exception ex) {
                // Redis unreachable → fall back to in-process Caffeine for defense in depth.
                // Logging at WARN so an outage shows up but does not drop emails.
                log.warn("Redis SETNX failed for key={} — falling back to Caffeine: {}",
                        idempotencyKey, ex.getMessage());
            }
        }

        Boolean previous = caffeineFallback.asMap().putIfAbsent(idempotencyKey, Boolean.TRUE);
        boolean firstSeen = (previous == null);
        if (!firstSeen) {
            log.info("Idempotent skip — duplicate email suppressed via Caffeine fallback (key={})",
                    idempotencyKey);
        }
        return firstSeen;
    }

    /**
     * Compute a deterministic idempotency key for an email send.
     *
     * <p>Prefers an explicit producer-supplied key; otherwise derives a stable
     * SHA-256 hash from the dedup-relevant fields (recipient + template + type +
     * sorted variables). The same logical email always maps to the same key, so a
     * redelivery of the identical {@code EmailEvent} collides with the first send.</p>
     *
     * @param explicitKey producer-supplied idempotency key (nullable)
     * @param to          recipient email
     * @param templateName template name
     * @param emailType   email type
     * @param variables   template variables (nullable)
     * @return a non-null, non-blank deterministic key
     */
    public String computeKey(String explicitKey, String to, String templateName,
                             String emailType, Map<String, Object> variables) {
        if (explicitKey != null && !explicitKey.isBlank()) {
            return explicitKey;
        }
        StringBuilder material = new StringBuilder();
        material.append(to == null ? "" : to).append('|')
                .append(templateName == null ? "" : templateName).append('|')
                .append(emailType == null ? "" : emailType).append('|');
        if (variables != null && !variables.isEmpty()) {
            // TreeMap → deterministic ordering regardless of source map iteration order.
            for (Map.Entry<String, Object> e : new TreeMap<>(variables).entrySet()) {
                material.append(e.getKey()).append('=')
                        .append(e.getValue()).append(';');
            }
        }
        return sha256(material.toString());
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandated by the JDK — cannot happen. Fall back to raw material.
            return Integer.toHexString(input.hashCode());
        }
    }
}
