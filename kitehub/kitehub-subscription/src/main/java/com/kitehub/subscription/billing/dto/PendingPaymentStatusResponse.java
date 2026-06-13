package com.kitehub.subscription.billing.dto;

import com.kitehub.platform.domain.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Owner-facing pending-payment status for the "đang chờ xác nhận" billing screen
 * (GAP-1257-BE). FE polls {@code GET /api/platform/subscriptions/instance/{id}/pending-payment-status}.
 *
 * <p>Surfaces the in-flight VietQR payment the owner is waiting for the platform
 * admin to reconcile (SUB-19 admin-confirm capture source). {@code expiresAt} is the
 * derived admin-confirm SLA deadline ({@code payment.createdAt + adminConfirmSlaHours})
 * so the FE can render a countdown; {@code adminConfirmSlaHours} is the raw SLA window.</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Data
@Builder
public class PendingPaymentStatusResponse {

    /** {@code true} when the instance has an in-flight pending payment awaiting admin confirm. */
    private boolean hasPendingPayment;

    /** Subscription owning the pending payment (null when {@code hasPendingPayment=false}). */
    private UUID subscriptionId;

    /** Pending payment id (null when none). FE redirects to {@code /billing/payment/{pendingPaymentId}}. */
    private UUID pendingPaymentId;

    /** Pending payment amount in VND minor unit (integer đồng). */
    private Long amount;

    /** Currency (always {@code VND} in Phase 1 BETA). */
    private String currency;

    /** Pending payment status (PENDING while awaiting admin confirm). */
    private PaymentStatus status;

    /** Subscription tier the owner is paying to activate/keep (pendingTier or active tier). */
    private String tier;

    /** When the payment record was created. */
    private LocalDateTime createdAt;

    /**
     * Derived admin-confirm SLA deadline = {@code createdAt + adminConfirmSlaHours}.
     * FE renders a "admin sẽ đối soát trong" countdown against this. Null when no pending.
     */
    private LocalDateTime expiresAt;

    /** Admin-confirm SLA window in hours (config {@code kitehub.payment.admin-confirm-sla-hours}). */
    private long adminConfirmSlaHours;
}
