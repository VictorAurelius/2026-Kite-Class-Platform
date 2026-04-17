package com.kiteclass.core.module.payment.enums;

/**
 * Payment status lifecycle.
 *
 * @since 1.0.0
 */
public enum PaymentStatus {
    /**
     * Payment initiated but not yet processed by gateway.
     */
    PENDING,

    /**
     * Payment being processed by gateway.
     */
    PROCESSING,

    /**
     * Payment successfully completed.
     */
    COMPLETED,

    /**
     * Payment failed or cancelled.
     */
    FAILED,

    /**
     * Payment was refunded.
     */
    REFUNDED
}
