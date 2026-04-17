package com.kiteclass.core.module.payment.enums;

/**
 * Payment method types supported by the platform.
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
