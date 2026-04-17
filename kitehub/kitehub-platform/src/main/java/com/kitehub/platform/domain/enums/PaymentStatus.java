package com.kitehub.platform.domain.enums;

/**
 * Payment transaction status.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
public enum PaymentStatus {
    /**
     * Payment created, waiting for user to pay.
     */
    PENDING,

    /**
     * Payment completed successfully.
     */
    COMPLETED,

    /**
     * Payment failed (timeout, insufficient funds, etc.).
     */
    FAILED,

    /**
     * Payment refunded to customer.
     */
    REFUNDED,

    /**
     * Payment cancelled by user or admin.
     */
    CANCELLED
}
