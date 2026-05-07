package com.kitehub.subscription.beta.entity;

/**
 * Lifecycle status for beta access request (GAP-372 Wave 33).
 *
 * <p>State machine:</p>
 * <pre>
 *   PENDING ─approve()─▶ APPROVED ─completeBetaSignup()─▶ SIGNED_UP
 *      │
 *      └─reject()─▶ REJECTED
 * </pre>
 *
 * <p>Per {@code design-patterns.md §3.3 Status Pattern}, transitions are enforced
 * by {@link com.kitehub.subscription.beta.service.BetaAccessService}, not by direct
 * setter calls. Tests assert this contract.</p>
 *
 * @since Wave 33 — GAP-372
 */
public enum BetaAccessRequestStatus {
    /** Freshly submitted; awaiting coordinator review. */
    PENDING,

    /** Coordinator approved; invite_token issued + email queued via Outbox. */
    APPROVED,

    /** Coordinator rejected (rejectionReason captured). */
    REJECTED,

    /** Approved invitee completed signup with token (token cleared). */
    SIGNED_UP
}
