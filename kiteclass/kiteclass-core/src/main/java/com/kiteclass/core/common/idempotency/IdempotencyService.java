package com.kiteclass.core.common.idempotency;

import com.kiteclass.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Wave beta-readiness-2 Bucket A — Shared idempotency for POST mutations (GAP-730).
 *
 * <p>Generalizes the Wave 105 Bucket D {@code PaymentIdempotencyService}
 * pattern to cover SIGNUP / ENROLLMENT / BETA_REQUEST scopes. Same
 * {@code Idempotency-Key} header replayed within the same tenant + scope
 * returns the FIRST request's cached response status + body — no second
 * INSERT executes.
 *
 * <p>Contract:
 * <ol>
 *   <li>Client OPTIONALLY sends {@code Idempotency-Key: &lt;uuid&gt;} header. If
 *       absent, the controller skips idempotency entirely (best-effort
 *       semantics — GAP-730 v1 ships header optional to avoid breaking
 *       existing clients).</li>
 *   <li>If header present, controller calls {@link #findExisting} BEFORE
 *       executing the normal handler. Cache hit → controller returns the
 *       cached response immediately.</li>
 *   <li>On cache miss, controller runs the normal handler, then calls
 *       {@link #recordRequest} to persist the mapping. Unique constraint on
 *       {@code (tenant_id, idempotency_key, scope)} protects against the
 *       race where two concurrent requests both miss the lookup — exactly
 *       one INSERT wins; the loser detects the duplicate + re-reads.</li>
 *   <li>{@code request_hash} (SHA-256 of normalized body) lets a future
 *       request with same key but DIFFERENT body be detected as a client
 *       bug (replay must mean "same request again").</li>
 * </ol>
 *
 * <p>Per `audit-service-isolation.md` — this service is NOT an audit
 * service; failures here MUST surface to the caller because the
 * caller's correctness depends on the idempotency check. Default
 * {@code @Transactional} propagation (REQUIRED) is correct here.
 *
 * @since 3.1.0 (Wave beta-readiness-2 Bucket A)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    /** Accepts UUID v4 dashed (36 char), ksuid (27), ulid (26), plus longer client IDs up to 255. */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,255}$");

    private static final String LOOKUP_SQL =
            "SELECT user_id, request_hash, response_status, response_body, created_at " +
            "FROM idempotency_keys " +
            "WHERE tenant_id = ? AND idempotency_key = ? AND scope = ?";

    private static final String INSERT_SQL =
            "INSERT INTO idempotency_keys " +
            "(tenant_id, idempotency_key, scope, user_id, request_hash, " +
            "response_status, response_body, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Validates Idempotency-Key header format. Used by controllers that want
     * to ENFORCE the header (return 400 if missing). GAP-730 v1 ships
     * header optional, so this helper is for opt-in callers.
     *
     * @throws BusinessException 400 {@code INVALID_IDEMPOTENCY_KEY} if malformed
     */
    public String requireValidKey(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (!KEY_PATTERN.matcher(headerValue).matches()) {
            throw new BusinessException("INVALID_IDEMPOTENCY_KEY", HttpStatus.BAD_REQUEST);
        }
        return headerValue;
    }

    /**
     * Format-validate a key but return {@code Optional.empty()} when absent
     * instead of throwing — GAP-730 v1 optional-header path.
     */
    public Optional<String> validKeyOrEmpty(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return Optional.empty();
        }
        if (!KEY_PATTERN.matcher(headerValue).matches()) {
            throw new BusinessException("INVALID_IDEMPOTENCY_KEY", HttpStatus.BAD_REQUEST);
        }
        return Optional.of(headerValue);
    }

    /**
     * Lookup an existing idempotency record. Returns {@link CachedResponse}
     * if a previous request with the same {@code (tenantId, key, scope)}
     * triple already completed; controller short-circuits with the cached
     * response. Returns {@code Optional.empty()} on cache miss.
     */
    public Optional<CachedResponse> findExisting(UUID tenantId, String key, IdempotencyScope scope) {
        try {
            CachedResponse row = jdbcTemplate.queryForObject(
                    LOOKUP_SQL,
                    (rs, rowNum) -> new CachedResponse(
                            rs.getObject("user_id", UUID.class),
                            rs.getString("request_hash"),
                            rs.getInt("response_status"),
                            rs.getString("response_body")),
                    tenantId, key, scope.name());
            return Optional.ofNullable(row);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * Persist the first-write mapping. If a concurrent request already
     * inserted the same {@code (tenantId, key, scope)}, the unique
     * constraint trips and this method returns {@code false} — the caller
     * MUST re-lookup to fetch the winner's cached response.
     *
     * @return {@code true} if INSERT succeeded; {@code false} if duplicate race lost
     */
    public boolean recordRequest(UUID tenantId,
                                 String key,
                                 IdempotencyScope scope,
                                 UUID userId,
                                 String requestHash,
                                 int responseStatus,
                                 String responseBody) {
        try {
            jdbcTemplate.update(INSERT_SQL,
                    tenantId, key, scope.name(), userId,
                    requestHash, responseStatus, responseBody,
                    OffsetDateTime.now());
            log.info("Idempotency recorded: scope={} key={} status={}",
                    scope, key, responseStatus);
            return true;
        } catch (DuplicateKeyException ex) {
            log.info("Idempotency race lost: scope={} key={} — caller should re-lookup",
                    scope, key);
            return false;
        }
    }

    /**
     * Compute the {@code request_hash} column value from a canonical
     * representation of the request payload. Caller passes the serialized
     * JSON (or any deterministic string) — service computes SHA-256.
     *
     * <p>Used so that a future request with the same key but different
     * body can be detected as a client bug rather than silently treated
     * as a replay.
     */
    public static String hashRequest(String canonicalBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is a JRE-mandated algorithm — should never throw.
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /**
     * Cached idempotency record returned on replay. Caller reconstructs the
     * HTTP response from {@code responseStatus} + {@code responseBody}.
     */
    public record CachedResponse(UUID userId,
                                   String requestHash,
                                   int responseStatus,
                                   String responseBody) {
    }
}
