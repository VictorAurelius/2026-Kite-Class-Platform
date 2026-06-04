package com.kitehub.platform.domain.enums;

/**
 * Subscription status values.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
public enum SubscriptionStatus {
    /**
     * Subscription is active and paid.
     */
    ACTIVE,

    /**
     * Subscription has been cancelled (still active until expiry).
     */
    CANCELLED,

    /**
     * Subscription has expired and is no longer active.
     */
    EXPIRED,

    /**
     * Subscription is suspended (payment failed or admin action).
     */
    SUSPENDED,

    /**
     * Subscription created but awaiting first payment confirmation (SUB-20).
     *
     * <p>Phase 1 BETA create-first-paid manual VietQR gate: POST
     * {@code /api/platform/subscriptions} sets {@code status=PENDING, tier=FREE,
     * pendingTier=<requested>}. Subscription stays PENDING (instance NOT activated,
     * no subscription-created email) until {@code PaymentService.confirmPayment}
     * → {@code SubscriptionService.applyPendingUpgrade} flips it to ACTIVE.</p>
     *
     * @since wave flow-kh3
     */
    PENDING
}
