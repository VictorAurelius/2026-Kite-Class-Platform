package com.kiteclass.core.module.payment.enums;

/**
 * Canonical PaymentMethod enum for KiteClass (school payment domain).
 *
 * <p>Scope: invoice + installment payment for school students/parents.
 * For KiteHub subscription billing, see
 * {@code com.kitehub.platform.domain.enums.PaymentMethod} (separate domain).
 *
 * <p>GAP-739 (Wave beta-readiness-8 Bucket C 2026-05-25): consolidated duplicate
 * declaration — removed orphan {@code com.kiteclass.core.common.constant.PaymentMethod}
 * (zero consumers, drift risk). FE TypeScript union synced in
 * {@code kiteclass-frontend/src/types/payment.ts}.
 *
 * @since 1.0.0
 */
public enum PaymentMethod {
    /**
     * Cash payment (offline).
     */
    CASH(false),

    /**
     * Bank transfer (offline).
     */
    BANK_TRANSFER(false),

    /**
     * MoMo e-wallet (online gateway).
     */
    MOMO(true),

    /**
     * VNPay payment gateway (online).
     */
    VNPAY(true),

    /**
     * ZaloPay e-wallet (online gateway).
     */
    ZALOPAY(true),

    /**
     * Credit card payment (online gateway).
     */
    CREDIT_CARD(true);

    private final boolean online;

    PaymentMethod(boolean online) {
        this.online = online;
    }

    /**
     * Checks if this payment method requires online gateway integration.
     *
     * @return true if online payment method
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Checks if this payment method is offline (manual processing).
     *
     * @return true if offline payment method
     */
    public boolean isOffline() {
        return !online;
    }
}
