package com.kitehub.subscription.idempotency;

import com.kitehub.subscription.dto.UpgradeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service wrapper over {@link MigrationIdempotencyKeyRepository} (GAP-192 Phase 4b-i).
 *
 * <p>Two public operations:
 * <ul>
 *   <li>{@link #findExisting} — non-mutating lookup used at the top of
 *       {@code TrialToPaidService.initiateUpgrade}. Returns the cached response so the
 *       controller can short-circuit with a 202.</li>
 *   <li>{@link #persist} — stores the response AFTER the migration has been initiated.
 *       Persistence inside the caller's txn guarantees that either both the migration
 *       row and the idempotency row commit, or neither.</li>
 * </ul>
 * <p>Expired keys are reaped by the scheduler, not by reads (keeps the hot path lean).</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192 Phase 4b-i)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationIdempotencyKeyService {

    private final MigrationIdempotencyKeyRepository repository;

    /** TTL per api-contract.md — 10 minutes. */
    @Value("${kitehub.trial-to-paid.idempotency.ttl-minutes:10}")
    private int ttlMinutes;

    /**
     * Return the cached upgrade response for this key, if still valid. Callers use this
     * to short-circuit duplicate-request retries.
     */
    @Transactional(readOnly = true)
    public Optional<UpgradeResponse> findExisting(String idempotencyKey, UUID instanceId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        Optional<MigrationIdempotencyKey> record = repository.findByIdempotencyKeyAndInstanceId(
            idempotencyKey, instanceId);
        if (record.isEmpty()) {
            return Optional.empty();
        }
        MigrationIdempotencyKey k = record.get();
        if (k.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.debug("Idempotency key expired: {}", idempotencyKey);
            return Optional.empty();
        }
        log.info("Returning cached upgrade response for idempotency key {}", idempotencyKey);
        return Optional.of(UpgradeResponse.builder()
            .instanceId(k.getInstanceId())
            .migrationPhase(k.getResponsePhase())
            .startedAt(k.getResponseStartedAt())
            .estimatedCompletionSeconds(k.getResponseEstimatedCompletionSeconds())
            .pollUrl(k.getResponsePollUrl())
            .build());
    }

    /**
     * Persist the response envelope keyed by {@code idempotencyKey}. Silently skips
     * when the key is null/blank (callers not using the feature) or when the row
     * already exists (concurrent-creation race — we accept whichever row wins).
     *
     * <p>GAP-1271 — the idempotency-cache write is a best-effort side-effect: the
     * caller's {@code initiateUpgrade} migration must NOT fail just because a concurrent
     * request inserted the same {@code (idempotencyKey, instanceId)} row first. It runs in
     * its OWN transaction ({@link Propagation#REQUIRES_NEW}) so that a UNIQUE-constraint
     * violation poisons only this sub-transaction — never the parent migration txn (a
     * Postgres constraint violation aborts the whole txn; catching the exception inside the
     * parent txn would still leave it rollback-only and the parent would 500). Because
     * {@code persist} is the terminal step of {@code initiateUpgrade} (nothing mutates after
     * it), the REQUIRES_NEW boundary carries effectively zero orphan-row risk. The race
     * loser thus returns its own freshly-built 202 envelope (same shape) and the winner's
     * row serves future replays — idempotent replay, never a 500.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(String idempotencyKey, UpgradeResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        if (repository.findByIdempotencyKeyAndInstanceId(idempotencyKey, response.getInstanceId()).isPresent()) {
            log.debug("Idempotency key already persisted, skipping: {}", idempotencyKey);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        MigrationIdempotencyKey record = MigrationIdempotencyKey.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(idempotencyKey)
            .instanceId(response.getInstanceId())
            .responsePhase(response.getMigrationPhase())
            .responseStartedAt(response.getStartedAt())
            .responsePollUrl(response.getPollUrl())
            .responseEstimatedCompletionSeconds(response.getEstimatedCompletionSeconds())
            .createdAt(now)
            .expiresAt(now.plusMinutes(ttlMinutes))
            .build();
        try {
            repository.save(record);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent winner inserted the same (key, instanceId) between the existence
            // check above and this save. The winner's row already covers the cache — treat
            // as a successful idempotent replay rather than letting the violation 500 the
            // caller. REQUIRES_NEW keeps this rollback isolated from the parent migration txn.
            log.info("Concurrent idempotency-row insert for key {} — treating as idempotent replay", idempotencyKey);
        }
    }

    /**
     * Delete all rows past their TTL. Called from the scheduler; return value exposes
     * row-count to Micrometer for a retention metric.
     */
    @Transactional
    public int purgeExpired() {
        int count = repository.deleteExpired(LocalDateTime.now());
        if (count > 0) {
            log.info("Purged {} expired idempotency keys", count);
        }
        return count;
    }
}
