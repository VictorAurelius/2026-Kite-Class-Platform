package com.kitehub.subscription.billing.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.subscription.billing.dto.ReceiptResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

/**
 * Builds the Phase 1 BETA non-VAT receipt (biên nhận) for a confirmed payment (GAP-1266).
 *
 * <p>Read-only + derive-on-demand: a receipt is reconstructed from the completed {@code Payment}
 * row (+ its subscription tier + owning instance), so no extra persistence is required and the
 * receipt stays retrievable as long as the payment exists. Distinct from a VAT e-invoice
 * (deferred — GAP-185/634 MISA MeInvoice partnership).</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    static final String NON_VAT_NOTE =
        "Đây là biên nhận thanh toán (không phải hóa đơn GTGT). "
            + "Hóa đơn điện tử có giá trị về thuế sẽ được phát hành riêng nếu trung tâm yêu cầu.";

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InstanceRepository instanceRepository;

    /**
     * Generate the receipt for a confirmed payment.
     *
     * @param paymentId payment UUID
     * @return receipt representation
     * @throws IllegalArgumentException if the payment is not found OR not COMPLETED
     */
    @Transactional(readOnly = true)
    public ReceiptResponse generateReceipt(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        return buildReceipt(payment);
    }

    /**
     * Build a receipt for an already-loaded payment entity (used inline by the confirm-notify
     * flow so it does not re-query). The payment MUST be COMPLETED.
     *
     * @param payment confirmed payment entity
     * @return receipt representation
     * @throws IllegalArgumentException if the payment is not COMPLETED
     */
    @Transactional(readOnly = true)
    public ReceiptResponse buildReceipt(Payment payment) {
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException(
                "Receipt is only available after the payment is confirmed: " + payment.getId());
        }

        Subscription subscription = subscriptionRepository.findById(payment.getSubscriptionId())
            .orElse(null);
        Instance instance = payment.getInstanceId() == null ? null
            : instanceRepository.findById(payment.getInstanceId()).orElse(null);

        LocalDateTime paidAt = payment.getPaidAt() != null ? payment.getPaidAt() : LocalDateTime.now();

        return ReceiptResponse.builder()
            .receiptNumber(buildReceiptNumber(payment, paidAt))
            .paymentId(payment.getId())
            .subscriptionId(payment.getSubscriptionId())
            .instanceId(payment.getInstanceId())
            .organizationName(instance != null ? instance.getOrganizationName() : null)
            .tier(subscription != null && subscription.getTier() != null
                ? subscription.getTier().name() : null)
            .billingCycle(subscription != null && subscription.getBillingCycle() != null
                ? subscription.getBillingCycle().name() : null)
            .amountVnd(payment.getAmountVnd())
            .currency(payment.getCurrency())
            .paymentMethod(payment.getPaymentMethod())
            .transactionId(payment.getTransactionId())
            .paidAt(paidAt)
            .issuedAt(LocalDateTime.now())
            .note(NON_VAT_NOTE)
            .build();
    }

    /**
     * Deterministic receipt number {@code BN-<year>-<8 uppercase hex of payment id>}.
     */
    static String buildReceiptNumber(Payment payment, LocalDateTime paidAt) {
        int year = paidAt != null ? paidAt.getYear() : Year.now().getValue();
        String idPart = payment.getId() == null
            ? "00000000"
            : payment.getId().toString().substring(0, 8).toUpperCase();
        return "BN-" + year + "-" + idPart;
    }
}
