package com.kitehub.platform.domain.enums;

/**
 * Payment methods supported by KiteHub platform.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
public enum PaymentMethod {
    /**
     * VietQR bank transfer (scan QR code to pay).
     */
    VIETQR,

    /**
     * MoMo e-wallet payment.
     */
    MOMO,

    /**
     * VNPAY payment gateway.
     */
    VNPAY,

    /**
     * Direct bank transfer (manual).
     */
    BANK_TRANSFER,

    /**
     * Manual payment (admin entry).
     */
    MANUAL
}
