package com.kitehub.subscription.billing.dto;

import com.kitehub.platform.domain.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Non-VAT receipt (biên nhận) issued after a payment is confirmed (GAP-1266).
 *
 * <p>Phase 1 BETA ships a simple, non-tax receipt — NOT a VAT e-invoice (hóa đơn GTGT),
 * which is deferred to the MISA MeInvoice partnership (GAP-185 / GAP-634). The receipt is
 * derived on-demand from the completed {@code Payment} (+ its subscription + instance), so it
 * needs no separate storage and stays retrievable via
 * {@code GET /api/platform/payments/{id}/receipt} as long as the payment row exists.</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Data
@Builder
public class ReceiptResponse {

    /** Human-friendly receipt number, derived deterministically from the payment (e.g. BN-2026-1A2B3C4D). */
    private String receiptNumber;

    private UUID paymentId;
    private UUID subscriptionId;
    private UUID instanceId;

    /** Payer organization name (trung tâm). */
    private String organizationName;

    /** Subscription tier the payment was for. */
    private String tier;
    /** Billing cycle (MONTHLY / ANNUALLY). */
    private String billingCycle;

    private Long amountVnd;
    private String currency;
    private PaymentMethod paymentMethod;

    /** Bank transaction id captured at admin confirm / webhook. */
    private String transactionId;

    /** When the payment was confirmed (paid). */
    private LocalDateTime paidAt;
    /** When this receipt representation was issued/rendered. */
    private LocalDateTime issuedAt;

    /** Non-VAT disclaimer text (Vietnamese). */
    private String note;
}
