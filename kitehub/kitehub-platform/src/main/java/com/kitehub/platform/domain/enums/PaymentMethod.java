package com.kitehub.platform.domain.enums;

/**
 * Canonical PaymentMethod enum for KiteHub (subscription billing domain).
 *
 * <p>Scope: monthly/annual subscription payment for center owners.
 * For KiteClass school payment, see
 * {@code com.kiteclass.core.module.payment.enums.PaymentMethod} (separate domain).
 *
 * <p>GAP-739 (Wave beta-readiness-8 Bucket C 2026-05-25): FE TypeScript union
 * {@code kitehub-frontend/src/types/payment.ts} synced — previously missing
 * {@code VNPAY} + {@code MANUAL} causing 3-way drift with audit findings.
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
