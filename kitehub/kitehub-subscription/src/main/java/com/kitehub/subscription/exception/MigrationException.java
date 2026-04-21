package com.kitehub.subscription.exception;

/**
 * Signals a domain-level error during trial-to-paid migration.
 *
 * <p>Carries a stable {@link #code} enum mapped to HTTP status + API error envelope
 * by {@code GlobalExceptionHandler}. See {@code api-contract.md} "Error Codes Reference".</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
public class MigrationException extends RuntimeException {

    public enum Code {
        /** Another migration already in flight for this instance (T2P-08). HTTP 409. */
        MIGRATION_IN_FLIGHT,

        /** Rollback requested beyond 24h window (T2P-04). HTTP 410. */
        REVERSAL_WINDOW_EXPIRED,

        /** Upgrade attempted beyond 24h rescue window post trial expiry (T2P-05). HTTP 410. */
        RESCUE_WINDOW_EXPIRED,

        /** Instance in MIGRATION_FAILED; requires manual ops resolution. HTTP 423. */
        MIGRATION_FAILED_LOCKED,

        /** Payment method pre-validation failed. HTTP 402. */
        PAYMENT_DECLINED,

        /** Webhook/admin action attempted against unexpected phase. HTTP 409. */
        INVALID_PHASE_TRANSITION
    }

    private final Code code;

    public MigrationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public MigrationException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
