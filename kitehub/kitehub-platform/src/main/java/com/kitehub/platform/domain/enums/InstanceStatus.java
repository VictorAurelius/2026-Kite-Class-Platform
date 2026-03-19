package com.kitehub.platform.domain.enums;

/**
 * Instance status enum.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
public enum InstanceStatus {
    /**
     * Pending email verification (no DB provisioned yet).
     */
    PENDING,

    /**
     * Trial period (14 days free).
     */
    TRIAL,

    /**
     * Active paid subscription.
     */
    ACTIVE,

    /**
     * Suspended (subscription expired or payment failed).
     */
    SUSPENDED,

    /**
     * Soft deleted.
     */
    DELETED
}
