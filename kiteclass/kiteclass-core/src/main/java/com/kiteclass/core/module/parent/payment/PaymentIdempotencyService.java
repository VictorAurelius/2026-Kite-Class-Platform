package com.kiteclass.core.module.parent.payment;

import com.kiteclass.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Wave 105 Bucket D — Idempotency-Key handling for parent payment endpoints.
 *
 * <p>Per `pre-handoff-self-test-completeness.md` §2.6 Payment flow gap (d):
 * <em>"Same key replayed → no double-charge; row in payment_attempts table
 * with idempotency state"</em>.
 *
 * <p>Contract:
 * <ol>
 *   <li>Client sends {@code Idempotency-Key: &lt;uuid&gt;} header on every
 *       POST to {@code /api/v1/parent/payments/...}.</li>
 *   <li>Server first looks up {@code payment_idempotency_keys} by
 *       {@code (instance_id, idempotency_key)}.</li>
 *   <li>If found → return the cached payment_id + qr_payload (no second
 *       PaymentService.createPayment invocation).</li>
 *   <li>If not found → invoke PaymentService.createPayment, then INSERT
 *       the mapping row. The unique constraint {@code uk_payment_idempotency_scope}
 *       protects against race: if two concurrent requests both miss the
 *       lookup, exactly one INSERT wins; the loser catches DuplicateKeyException
 *       and re-reads the winner's payment_id.</li>
 *   <li>Window 24h (V61 column default) — matches VietQR partner-bank txn
 *       expiry. Background sweeper deletes rows past expires_at (future scope).</li>
 * </ol>
 *
 * <p>This service is the seam between {@link com.kiteclass.core.module.parent.payment.ParentPaymentController}
 * and the underlying {@code PaymentService}. It does NOT depend on Bucket E
 * fix (PaymentController userId hardcoded `1L`) because parent controller
 * uses {@code X-User-Reference-Id} header populated by gateway from
 * {@code users.reference_id} — same pattern as ParentController.
 *
 * @since 3.0.0 (Wave 105 Bucket D)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentIdempotencyService {

    /**
     * UUID v4 / ksuid / ulid pattern. 36 char dashed UUID matches most clients.
     * 27-char ksuid + 26-char ulid also accepted (alphanumeric, no special).
     */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");

    private static final String LOOKUP_SQL =
            "SELECT payment_id, qr_payload " +
            "FROM payment_idempotency_keys " +
            "WHERE instance_id = ?::uuid AND idempotency_key = ? " +
            "AND expires_at > NOW()";

    private static final String INSERT_SQL =
            "INSERT INTO payment_idempotency_keys " +
            "(instance_id, idempotency_key, user_id, invoice_id, payment_id, qr_payload) " +
            "VALUES (?::uuid, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Validates Idempotency-Key header format. Rejects null / blank / wrong
     * format with 400 BAD_REQUEST so client doesn't get cryptic SQL error.
     *
     * @throws BusinessException 400 INVALID_IDEMPOTENCY_KEY if missing or malformed
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
     * Lookup existing idempotency mapping. Returns Optional of (paymentId, qrPayload).
     * Caller decides whether to short-circuit (replay path) or proceed (new path).
     */
    public Optional<IdempotentResult> lookup(String tenantId, String key) {
        try {
            IdempotentResult result = jdbcTemplate.queryForObject(
                    LOOKUP_SQL,
                    (rs, rowNum) -> new IdempotentResult(
                            rs.getLong("payment_id"),
                            rs.getString("qr_payload")),
                    tenantId, key);
            return Optional.ofNullable(result);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * Persist the first-write mapping. If a concurrent request wins the race,
     * this throws DuplicateKeyException → caller retries lookup to get the
     * winner's payment_id.
     *
     * @return true if INSERT succeeded; false if duplicate (caller must re-lookup)
     */
    public boolean recordFirstWrite(String tenantId, String key, Long userId,
                                     Long invoiceId, Long paymentId, String qrPayload) {
        try {
            jdbcTemplate.update(INSERT_SQL,
                    tenantId, key, userId, invoiceId, paymentId, qrPayload);
            log.info("Idempotency key recorded: key={} paymentId={}", key, paymentId);
            return true;
        } catch (DuplicateKeyException ex) {
            log.info("Idempotency key race lost: key={} — re-lookup needed", key);
            return false;
        }
    }

    /**
     * Result tuple for idempotent replay.
     */
    public record IdempotentResult(Long paymentId, String qrPayload) {
    }
}
