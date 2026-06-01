package com.kitehub.email.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * <p><strong>Design — why Caffeine, not a DB/Redis store:</strong> {@code kitehub-email}
 * is deliberately stateless (no JPA datasource, no Redis). Adding shared-state infra
 * to it is an architecture decision out of scope for this fix and would duplicate the
 * producer-side {@code email_sent_log} store. This guard uses the already-present
 * Caffeine cache (Strategy: same backend as the branding-lookup cache) with a TTL
 * window keyed by a deterministic idempotency key.</p>
 *
 * <p><strong>Coverage + limitation (documented honestly):</strong></p>
 * <ul>
 *   <li>✅ Covers in-flight concurrent redelivery + same-process Spring-listener retry
 *       (the common duplicate cause when a single redelivery storm hits a live process).</li>
 *   <li>⚠️ Does NOT survive process restart — an in-process cache is empty after an
 *       OOM-crash-restart, so a message redelivered AFTER restart can still be re-sent.
 *       Full cross-restart idempotency requires a shared store (Redis / DB) — tracked
 *       as a follow-up (GAP-840) per the gap's PARTIAL exit ramp.</li>
 * </ul>
 *
 * @since GAP-580 (Wave phase2-beta — consumer-side email idempotency, defense-in-depth)
 */
@Slf4j
@Component
public class EmailIdempotencyGuard {

    private final Cache<String, Boolean> seenKeys;

    public EmailIdempotencyGuard(
            @Value("${kitehub.email.idempotency.ttl-minutes:60}") long ttlMinutes,
            @Value("${kitehub.email.idempotency.max-size:50000}") long maxSize) {
        this.seenKeys = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .maximumSize(maxSize)
                .build();
        log.info("EmailIdempotencyGuard initialised: ttl={}min maxSize={}", ttlMinutes, maxSize);
    }

    /**
     * Mark the given idempotency key as seen, returning whether this is the FIRST time.
     *
     * <p>Atomic check-and-set via Caffeine's {@code asMap().putIfAbsent} — concurrent
     * redeliveries of the same key see exactly one {@code true} (proceed) and the rest
     * {@code false} (skip).</p>
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
        Boolean previous = seenKeys.asMap().putIfAbsent(idempotencyKey, Boolean.TRUE);
        boolean firstSeen = (previous == null);
        if (!firstSeen) {
            log.info("Idempotent skip — duplicate email suppressed (key={})", idempotencyKey);
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
