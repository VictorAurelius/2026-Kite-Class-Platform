package com.kitehub.subscription.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Result of a win-back reactivation attempt for a SUSPENDED/cancelled instance (GAP-1263-BE).
 * {@code POST /api/platform/subscriptions/instance/{id}/reactivate}.
 *
 * <p>Phase 1 BETA: reactivation goes through the manual VietQR payment gate (mirrors SUB-20 +
 * GAP-1016 manual-renewal) — the owner cannot self-reactivate for free. On {@link Outcome#PAYMENT_REQUIRED}
 * a PENDING reactivation payment is created; the instance flips back to ACTIVE only after the
 * platform admin confirms it (existing {@code applyConfirmedRenewal} path). Idempotent: a repeat
 * call while a reactivation payment is already in flight returns the same payment.</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Data
@Builder
public class ReactivateResponse {

    /** Reactivation outcome. */
    public enum Outcome {
        /** Instance is SUSPENDED and reactivatable — a PENDING payment was created/returned. */
        PAYMENT_REQUIRED,
        /** Instance is already ACTIVE/on-trial — nothing to do (idempotent). */
        ALREADY_ACTIVE,
        /** Instance has no subscription to revive — owner must create a fresh paid subscription. */
        NO_SUBSCRIPTION
    }

    /** Churn classification that led to the suspension (win-back vs distinct from fraud tombstone). */
    public enum ChurnType {
        /** Owner-initiated cancellation (subscription CANCELLED). */
        VOLUNTARY,
        /** Non-payment lapse (subscription EXPIRED/SUSPENDED past grace, SUB-24). */
        INVOLUNTARY,
        /** Not applicable (already active / no subscription). */
        NONE
    }

    private UUID instanceId;
    private Outcome outcome;
    private ChurnType churnType;

    /** Subscription being reactivated (null for NO_SUBSCRIPTION). */
    private UUID subscriptionId;

    /** PENDING reactivation payment (present only for PAYMENT_REQUIRED). */
    private UUID pendingPaymentId;
    private Long amount;
    private String currency;

    /** Human-readable Vietnamese status message for the FE. */
    private String message;
}
