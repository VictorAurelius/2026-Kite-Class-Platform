package com.kitehub.admin.dto;

/**
 * Typed response for the admin payments summary endpoint (GAP-654).
 *
 * <p>Replaces the prior untyped {@code Map<String, Object>} return so the contract is
 * springdoc-discoverable + compiler-checked. Per Wave 92 Bucket D the underlying controller is a
 * pending-only v1 stub — fields the stub cannot yet compute (confirmed/historical breakdown,
 * period window) are exposed with documented defaults rather than fabricated values, keeping the
 * existing business semantics unchanged (pending count is the only real signal the stub produces).</p>
 *
 * @param totalAmountVnd  total VND amount across the payments counted in this summary (pending-only
 *                        scope for the v1 stub); {@code 0} when no payments
 * @param totalCount      total number of payments counted (pending-only scope for the v1 stub)
 * @param currency        ISO currency code for {@code totalAmountVnd} (always {@code VND} for Phase 1 BETA)
 * @param pendingCount    number of payments awaiting admin confirmation
 * @param completedCount  number of confirmed/completed payments — {@code 0} in the v1 stub (breakdown
 *                        deferred to Phase 1.5+ per Wave 92 Bucket D scope)
 * @since 1.0
 */
public record PaymentsSummaryResponse(
        long totalAmountVnd,
        long totalCount,
        String currency,
        long pendingCount,
        long completedCount
) {
}
