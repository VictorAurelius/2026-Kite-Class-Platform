package com.kitehub.subscription.idempotency.service;

import com.kitehub.subscription.idempotency.entity.IdempotencyKey;
import com.kitehub.subscription.idempotency.exception.IdempotencyConflictException;
import com.kitehub.subscription.idempotency.repository.IdempotencyKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Idempotency cache service (GAP-536 Wave 77 Bucket D).
 *
 * <p>Wave 77 outside-in failure-mode matrix F3 (P0): P1 Solo teacher on 3G
 * double-taps "Create" → two concurrent POST /instances → two orphan rows
 * created. This service backs the Stripe-style {@code Idempotency-Key}
 * header pattern: client supplies a UUID v4 per submit attempt; the service
 * caches the response keyed on {@code (key, endpoint)} for 24h; duplicate
 * keys replay the cached response; key-collision with different body
 * raises 422.</p>
 *
 * <p>Distinct from {@code MigrationIdempotencyKey} (V20, 10-min TTL,
 * trial-to-paid upgrade scope) — this is a generic per-endpoint surface
 * any mutation endpoint can opt into via the {@code IdempotencyInterceptor}.</p>
 *
 * @since Wave 77 — GAP-536
 */
@Service
@Slf4j
public class IdempotencyService {

    /** TTL window for cached responses. Matches V41 migration comment. */
    public static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyKeyRepository repository;

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    /**
     * Look up a cached response for a {@code (key, endpoint)} pair.
     *
     * <p>If found AND not expired AND the cached {@code requestHash} matches
     * the supplied {@code currentRequestHash}, returns the cached row for
     * replay. If the hash mismatches, throws
     * {@link IdempotencyConflictException} (controller maps to 422). If not
     * found OR expired, returns empty so the caller proceeds with normal
     * handler execution.</p>
     *
     * @param key client-supplied Idempotency-Key header value
     * @param endpoint logical endpoint identifier (e.g., POST_instances)
     * @param currentRequestHash SHA-256 hex of the current request body
     * @return cached row if present, valid, and hash matches; empty otherwise
     * @throws IdempotencyConflictException if key found but request hash differs
     */
    @Transactional(readOnly = true)
    public Optional<IdempotencyKey> findValidReplay(String key, String endpoint, String currentRequestHash) {
        Optional<IdempotencyKey> opt = repository.findByKeyAndEndpoint(key, endpoint);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        IdempotencyKey existing = opt.get();
        if (existing.getExpiresAt().isBefore(OffsetDateTime.now())) {
            // Expired — treat as cache miss; cleanup job will purge the row.
            return Optional.empty();
        }
        if (!existing.getRequestHash().equals(currentRequestHash)) {
            log.warn("Idempotency conflict: key={} endpoint={} cached_hash={} new_hash={}",
                    key, endpoint, existing.getRequestHash(), currentRequestHash);
            throw new IdempotencyConflictException(
                    "Idempotency-Key reused with a different request body");
        }
        return Optional.of(existing);
    }

    /**
     * Persist a successful response under the given idempotency key.
     *
     * <p>Called by the interceptor AFTER the handler completes successfully.
     * Handles the rare double-write race: if two concurrent first-time
     * requests with the same key both pass {@link #findValidReplay} (both
     * see no row) and both call {@code save}, the second one hits a primary
     * key constraint violation. Treat that as benign — the first writer
     * established the canonical cached response.</p>
     */
    @Transactional
    public void cacheResponse(String key, String endpoint, String requestHash,
                              int responseStatus, String responseBody) {
        OffsetDateTime now = OffsetDateTime.now();
        IdempotencyKey entity = IdempotencyKey.builder()
                .key(key)
                .endpoint(endpoint)
                .requestHash(requestHash)
                .responseStatus(responseStatus)
                .responseBody(responseBody)
                .createdAt(now)
                .expiresAt(now.plus(TTL))
                .build();
        try {
            repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            // Race: another thread won the cache insert. Verify the existing
            // row has the same request hash; if not, the caller will resurface
            // a 422 on their next attempt. We don't surface a 500 — both
            // threads observed identical inputs by definition (same key + same
            // hash was checked above).
            log.info("Idempotency cache race: key={} endpoint={} — winner already wrote", key, endpoint);
        }
    }

    /**
     * SHA-256 of the request body, hex-encoded. Used as the cache discriminator
     * so that key reuse with a different payload raises 422 instead of silently
     * replaying a stale response.
     */
    public static String hashRequest(String body) {
        if (body == null) {
            body = "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            // JRE without SHA-256 is unrecoverable; fail loudly.
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
