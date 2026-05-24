package com.kitehub.subscription.consent.immutable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Immutable consent record store với SHA-256 hash chain — Wave br-4 Bucket B (GAP-353b).
 *
 * <p>3 operations:
 * <ol>
 *   <li>{@link #recordConsent(Long, Long, Map, String, String)} — INSERT new row,
 *       prev_hash = latest row's currentHash for user_id (NULL nếu chain head).</li>
 *   <li>{@link #withdrawConsent(Long, Long, String, String)} — INSERT row mới với
 *       granted={essential:true,analytics:false,marketing:false}. KHÔNG flip existing row
 *       (RLS blocks UPDATE anyway).</li>
 *   <li>{@link #findHistory(Long)} — return full chain oldest→newest, validate integrity.</li>
 * </ol>
 *
 * <p>Concurrency: actual INSERT happens trong {@link ConsentInserter} với SERIALIZABLE
 * isolation + REQUIRES_NEW propagation. Service-level retry loop bao quanh attempt:
 * concurrent threads racing on cùng userId → Postgres serialization-failure → caught
 * here → backoff + retry. Hash chain preserved (no fork).
 *
 * @since Wave beta-readiness-4 Bucket B — GAP-353b
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_INITIAL_DELAY_MS = 50L;

    private final ConsentRecordImmutableRepository repository;
    private final ConsentInserter inserter;

    /**
     * INSERT row mới với hash chain. Public API.
     *
     * @param userId    nullable — marketing-surface visitor before login
     * @param tenantId  nullable — pre-tenant visitor
     * @param granted   shape {"essential":true,"analytics":bool,"marketing":bool}; essential coerced true
     * @param ipAddress IPv4/IPv6 from request (gateway X-Forwarded-For OR remoteAddr)
     * @param userAgent User-Agent header
     * @return persisted row với currentHash + prevHash populated
     */
    public ConsentRecordImmutable recordConsent(
            Long userId,
            Long tenantId,
            Map<String, Boolean> granted,
            String ipAddress,
            String userAgent) {

        if (granted == null || granted.isEmpty()) {
            throw new IllegalArgumentException("granted categories required");
        }
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException("ipAddress required");
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("userAgent required");
        }

        // Server-side coerce: essential always true per BR-PDPL-CONSENT-001.
        Map<String, Boolean> sanitized = new TreeMap<>(granted);
        sanitized.put("essential", Boolean.TRUE);
        sanitized.putIfAbsent("analytics", Boolean.FALSE);
        sanitized.putIfAbsent("marketing", Boolean.FALSE);

        return doInsertWithRetry(userId, tenantId, sanitized, ipAddress, userAgent);
    }

    private ConsentRecordImmutable doInsertWithRetry(
            Long userId,
            Long tenantId,
            Map<String, Boolean> sanitized,
            String ipAddress,
            String userAgent) {

        long delay = RETRY_INITIAL_DELAY_MS;
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return inserter.insertOnce(userId, tenantId, sanitized, ipAddress, userAgent);
            } catch (ConcurrencyFailureException | DataIntegrityViolationException ex) {
                lastException = ex;
                log.warn("Consent insert retry {}/{} for user={}: {}",
                        attempt, MAX_RETRY_ATTEMPTS, userId, ex.getMessage());
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                    delay *= 2;
                }
            }
        }
        throw lastException;
    }

    /**
     * Withdraw consent — INSERT row mới (NOT flip). Hash chain extends naturally.
     * Idempotent: nếu latest row đã withdraw thì vẫn insert thêm row mới để audit
     * trail honest về số lần user clicked withdraw button.
     */
    public ConsentRecordImmutable withdrawConsent(
            Long userId,
            Long tenantId,
            String ipAddress,
            String userAgent) {
        Map<String, Boolean> revoked = Map.of(
                "essential", Boolean.TRUE,
                "analytics", Boolean.FALSE,
                "marketing", Boolean.FALSE);
        return recordConsent(userId, tenantId, revoked, ipAddress, userAgent);
    }

    /**
     * Return consent history oldest→newest cho user.
     * Validate hash chain integrity — throws {@link ConsentChainIntegrityException}
     * nếu phát hiện tampering (prevHash mismatch OR currentHash recomputation fails).
     */
    @Transactional(readOnly = true)
    public List<ConsentRecordImmutable> findHistory(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId required");
        }
        List<ConsentRecordImmutable> history = repository.findByUserIdOrderBySignedAtAsc(userId);
        verifyChainIntegrity(history);
        return history;
    }

    /**
     * Standalone chain verification — useful cho audit jobs + IT tests.
     *
     * @throws ConsentChainIntegrityException nếu phát hiện tampering
     */
    @Transactional(readOnly = true)
    public void verifyChainIntegrity(Long userId) {
        verifyChainIntegrity(repository.findByUserIdOrderBySignedAtAsc(userId));
    }

    private void verifyChainIntegrity(List<ConsentRecordImmutable> history) {
        String expectedPrevHash = null;
        for (ConsentRecordImmutable row : history) {
            String actualPrev = row.getPrevHash();
            if (expectedPrevHash == null ? actualPrev != null : !expectedPrevHash.equals(actualPrev)) {
                throw new ConsentChainIntegrityException(
                        "prev_hash mismatch at row id=" + row.getId()
                                + ": expected=" + expectedPrevHash + " actual=" + actualPrev);
            }
            String recomputed = ConsentInserter.computeHash(
                    row.getPrevHash(),
                    row.getUserId(),
                    row.getTenantId(),
                    row.getGranted(),
                    row.getIpAddress(),
                    row.getUserAgent(),
                    row.getSignedAt());
            if (!recomputed.equals(row.getCurrentHash())) {
                throw new ConsentChainIntegrityException(
                        "current_hash mismatch at row id=" + row.getId()
                                + ": stored=" + row.getCurrentHash() + " recomputed=" + recomputed);
            }
            expectedPrevHash = row.getCurrentHash();
        }
    }

    /** Latest row cho user — convenience cho FE current state query. */
    @Transactional(readOnly = true)
    public Optional<ConsentRecordImmutable> findLatest(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return repository.findFirstByUserIdOrderBySignedAtDesc(userId);
    }

    /** Thrown khi hash chain validation phát hiện tampering. HTTP 500 status. */
    public static class ConsentChainIntegrityException extends RuntimeException {
        public ConsentChainIntegrityException(String message) {
            super(message);
        }
    }
}
