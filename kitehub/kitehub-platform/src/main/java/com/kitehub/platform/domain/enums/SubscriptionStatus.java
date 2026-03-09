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
    SUSPENDED
}
