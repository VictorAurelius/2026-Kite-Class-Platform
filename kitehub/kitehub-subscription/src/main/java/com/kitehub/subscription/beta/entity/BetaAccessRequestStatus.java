package com.kitehub.subscription.beta.entity;

/**
 * Lifecycle status for beta access request (GAP-372 Wave 33; GAP-600 Wave 92).
 *
 * <p>State machine:</p>
 * <pre>
 *   PENDING ─approve()─▶ APPROVED ─completeBetaSignup()─▶ SIGNED_UP
 *      │
 *      ├─reject()─▶ REJECTED
 *      │
 *      └─cleanupAbort()─▶ ABORTED  (GAP-600 — scheduled sweep stale PENDING > threshold)
 * </pre>
 *
 * <p>Per {@code design-patterns.md §3.3 Status Pattern}, transitions are enforced
 * by {@link com.kitehub.subscription.beta.service.BetaAccessService}, not by direct
 * setter calls. Tests assert this contract.</p>
 *
 * <p>{@link #ABORTED} is a terminal state preserving the audit trail (NOT delete) —
 * future re-submit của cùng email được phép vì unique constraint on
 * {@code (invite_token)} only (xem V28 migration). Re-submit từ user tạo PENDING row mới;
 * ABORTED row giữ lại để forensics.</p>
 *
 * @since Wave 33 — GAP-372; ABORTED added Wave 92 — GAP-600
 */
public enum BetaAccessRequestStatus {
    /** Freshly submitted; awaiting coordinator review. */
    PENDING,

    /** Coordinator approved; invite_token issued + email queued via Outbox. */
    APPROVED,

    /** Coordinator rejected (rejectionReason captured). */
    REJECTED,

    /** Approved invitee completed signup with token (token cleared). */
    SIGNED_UP,

    /**
     * Auto-aborted by {@link com.kitehub.subscription.beta.scheduler.BetaRequestAbortCleanupScheduler}
     * — PENDING row exceeded stale threshold (default 24h) without coordinator decision.
     * Row preserved for audit; user MAY re-submit beta access request với cùng email.
     *
     * @since Wave 92 — GAP-600
     */
    ABORTED
}
