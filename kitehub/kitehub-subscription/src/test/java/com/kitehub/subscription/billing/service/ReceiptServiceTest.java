package com.kitehub.subscription.billing.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.billing.dto.ReceiptResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReceiptService} — non-VAT receipt generation (GAP-1266).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptService Unit Tests")
class ReceiptServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private InstanceRepository instanceRepository;

    @InjectMocks private ReceiptService receiptService;

    @Test
    @DisplayName("GAP-1266: receipt for a confirmed payment carries number + org + tier + non-VAT note")
    void generateReceipt_completedPayment() {
        UUID payId = UUID.fromString("1a2b3c4d-0000-0000-0000-000000000000");
        UUID subId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();

        Payment payment = new Payment();
        payment.setId(payId);
        payment.setSubscriptionId(subId);
        payment.setInstanceId(instId);
        payment.setAmountVnd(500_000L);
        payment.setCurrency("VND");
        payment.setPaymentMethod(PaymentMethod.VIETQR);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId("VCB-001");
        payment.setPaidAt(LocalDateTime.of(2026, 6, 13, 10, 0));
        when(paymentRepository.findById(payId)).thenReturn(Optional.of(payment));

        Subscription sub = new Subscription();
        sub.setTier(PricingTier.BASIC);
        sub.setBillingCycle(BillingCycle.MONTHLY);
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(sub));

        Instance instance = new Instance();
        instance.setOrganizationName("Trung tâm Demo");
        when(instanceRepository.findById(instId)).thenReturn(Optional.of(instance));

        ReceiptResponse receipt = receiptService.generateReceipt(payId);

        assertThat(receipt.getReceiptNumber()).isEqualTo("BN-2026-1A2B3C4D");
        assertThat(receipt.getOrganizationName()).isEqualTo("Trung tâm Demo");
        assertThat(receipt.getTier()).isEqualTo("BASIC");
        assertThat(receipt.getBillingCycle()).isEqualTo("MONTHLY");
        assertThat(receipt.getAmountVnd()).isEqualTo(500_000L);
        assertThat(receipt.getTransactionId()).isEqualTo("VCB-001");
        assertThat(receipt.getNote()).contains("không phải hóa đơn GTGT");
    }

    @Test
    @DisplayName("GAP-1266: receipt rejected for a non-confirmed payment")
    void generateReceipt_rejectsPending() {
        UUID payId = UUID.randomUUID();
        Payment pending = new Payment();
        pending.setId(payId);
        pending.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(payId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> receiptService.generateReceipt(payId))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
