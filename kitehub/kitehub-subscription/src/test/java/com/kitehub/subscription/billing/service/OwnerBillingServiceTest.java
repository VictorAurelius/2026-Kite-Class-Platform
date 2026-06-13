package com.kitehub.subscription.billing.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.billing.dto.DowngradePreviewResponse;
import com.kitehub.subscription.billing.dto.PendingPaymentStatusResponse;
import com.kitehub.subscription.billing.dto.ReactivateResponse;
import com.kitehub.subscription.exception.SubscriptionConflictException;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import com.kitehub.subscription.service.VietQRService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OwnerBillingService} — pending-payment status (GAP-1257-BE),
 * downgrade preview (GAP-1261), and win-back reactivation (GAP-1263-BE).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerBillingService Unit Tests")
class OwnerBillingServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private InstanceRepository instanceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private VietQRService vietQRService;

    @InjectMocks private OwnerBillingService service;

    private UUID instanceId;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();
        ReflectionTestUtils.setField(service, "adminConfirmSlaHours", 24L);
        ReflectionTestUtils.setField(service, "betaModeEnabled", false);
        ReflectionTestUtils.setField(service, "betaOverrideAmountVnd", 10_000L);
    }

    // ---- pending-payment-status (GAP-1257-BE) ----

    @Test
    @DisplayName("GAP-1257-BE: returns the in-flight pending payment + derived SLA deadline")
    void getPendingPaymentStatus_returnsPending() {
        UUID payId = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.of(2026, 6, 13, 9, 0);

        Subscription sub = new Subscription();
        sub.setId(UUID.randomUUID());
        sub.setInstanceId(instanceId);
        sub.setTier(PricingTier.FREE);
        sub.setPendingTier(PricingTier.BASIC);
        sub.setPendingPaymentId(payId);
        sub.setCreatedAt(created);
        when(subscriptionRepository.findByInstanceId(instanceId)).thenReturn(List.of(sub));

        Payment payment = new Payment();
        payment.setId(payId);
        payment.setAmountVnd(500_000L);
        payment.setCurrency("VND");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(created);
        when(paymentRepository.findById(payId)).thenReturn(Optional.of(payment));

        PendingPaymentStatusResponse resp = service.getPendingPaymentStatus(instanceId);

        assertThat(resp.isHasPendingPayment()).isTrue();
        assertThat(resp.getPendingPaymentId()).isEqualTo(payId);
        assertThat(resp.getAmount()).isEqualTo(500_000L);
        assertThat(resp.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(resp.getTier()).isEqualTo("BASIC");
        assertThat(resp.getAdminConfirmSlaHours()).isEqualTo(24L);
        assertThat(resp.getExpiresAt()).isEqualTo(created.plusHours(24));
    }

    @Test
    @DisplayName("GAP-1257-BE: no in-flight payment → hasPendingPayment=false")
    void getPendingPaymentStatus_none() {
        Subscription active = new Subscription();
        active.setInstanceId(instanceId);
        active.setPendingPaymentId(null);
        when(subscriptionRepository.findByInstanceId(instanceId)).thenReturn(List.of(active));

        PendingPaymentStatusResponse resp = service.getPendingPaymentStatus(instanceId);

        assertThat(resp.isHasPendingPayment()).isFalse();
        assertThat(resp.getPendingPaymentId()).isNull();
        assertThat(resp.getAdminConfirmSlaHours()).isEqualTo(24L);
    }

    // ---- downgrade-preview (GAP-1261) ----

    @Test
    @DisplayName("GAP-1261: PREMIUM→BASIC surfaces shrunk caps + custom-domain loss warning")
    void getDowngradePreview_premiumToBasic() {
        Instance instance = new Instance();
        instance.setId(instanceId);
        instance.setTier(PricingTier.PREMIUM);
        instance.setCustomDomain("school.edu.vn");
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(subscriptionRepository.findActiveByInstanceId(instanceId)).thenReturn(Optional.empty());

        DowngradePreviewResponse resp = service.getDowngradePreview(instanceId, PricingTier.BASIC);

        assertThat(resp.getCurrentTier()).isEqualTo(PricingTier.PREMIUM);
        assertThat(resp.getTargetTier()).isEqualTo(PricingTier.BASIC);
        assertThat(resp.getCurrentMaxStudents()).isEqualTo(200);
        assertThat(resp.getTargetMaxStudents()).isEqualTo(50);
        assertThat(resp.isCustomDomainWillBeDisabled()).isTrue();
        assertThat(resp.isHasActiveCustomDomain()).isTrue();
        assertThat(resp.getWarnings()).isNotEmpty();
        assertThat(resp.getUsageDataNote()).isNotBlank();
    }

    @Test
    @DisplayName("GAP-1261: rejecting a non-downgrade direction → IllegalArgumentException (400)")
    void getDowngradePreview_rejectsUpgradeDirection() {
        Instance instance = new Instance();
        instance.setId(instanceId);
        instance.setTier(PricingTier.BASIC);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(subscriptionRepository.findActiveByInstanceId(instanceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDowngradePreview(instanceId, PricingTier.PREMIUM))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("GAP-1261: missing instance → EntityNotFoundException (404)")
    void getDowngradePreview_instanceNotFound() {
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDowngradePreview(instanceId, PricingTier.BASIC))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- reactivate (GAP-1263-BE) ----

    @Test
    @DisplayName("GAP-1263-BE: PURGED instance is a tombstone → 409 conflict, no reactivation")
    void reactivate_purgedTombstone() {
        Instance instance = new Instance();
        instance.setId(instanceId);
        instance.setStatus(InstanceStatus.PURGED);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.reactivate(instanceId))
            .isInstanceOf(SubscriptionConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("GAP-1263-BE: DELETED instance (fraud/admin tombstone) → 409 conflict")
    void reactivate_deletedTombstone() {
        Instance instance = new Instance();
        instance.setId(instanceId);
        instance.setStatus(InstanceStatus.DELETED);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.reactivate(instanceId))
            .isInstanceOf(SubscriptionConflictException.class);
    }

    @Test
    @DisplayName("GAP-1263-BE: ACTIVE instance → idempotent ALREADY_ACTIVE, no payment")
    void reactivate_alreadyActive() {
        Instance instance = new Instance();
        instance.setId(instanceId);
        instance.setStatus(InstanceStatus.ACTIVE);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        ReactivateResponse resp = service.reactivate(instanceId);

        assertThat(resp.getOutcome()).isEqualTo(ReactivateResponse.Outcome.ALREADY_ACTIVE);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("GAP-1263-BE: SUSPENDED + no subscription → NO_SUBSCRIPTION")
    void reactivate_noSubscription() {
        Instance instance = new Instance();
        instance.setId(instanceId);
        instance.setStatus(InstanceStatus.SUSPENDED);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(subscriptionRepository.findByInstanceId(instanceId)).thenReturn(List.of());

        ReactivateResponse resp = service.reactivate(instanceId);

        assertThat(resp.getOutcome()).isEqualTo(ReactivateResponse.Outcome.NO_SUBSCRIPTION);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("GAP-1263-BE: SUSPENDED + EXPIRED sub → involuntary win-back, creates pending payment")
    void reactivate_suspendedInvoluntaryCreatesPayment() {
        Instance instance = new Instance();
        instance.setId(instanceId);
        instance.setStatus(InstanceStatus.SUSPENDED);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        Subscription sub = new Subscription();
        sub.setId(UUID.randomUUID());
        sub.setInstanceId(instanceId);
        sub.setStatus(SubscriptionStatus.EXPIRED);
        sub.setTier(PricingTier.BASIC);
        sub.setBillingCycle(BillingCycle.MONTHLY);
        sub.setPriceVnd(500_000L);
        sub.setCreatedAt(LocalDateTime.now());
        when(subscriptionRepository.findByInstanceId(instanceId)).thenReturn(List.of(sub));

        lenient().when(vietQRService.generateQRCode(any(UUID.class), anyLong(), anyString()))
            .thenReturn("https://img.vietqr.io/x.png");
        lenient().when(vietQRService.getBankCode()).thenReturn("VCB");
        lenient().when(vietQRService.getAccountNumber()).thenReturn("123");
        lenient().when(vietQRService.getAccountName()).thenReturn("KITECLASS");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });

        ReactivateResponse resp = service.reactivate(instanceId);

        assertThat(resp.getOutcome()).isEqualTo(ReactivateResponse.Outcome.PAYMENT_REQUIRED);
        assertThat(resp.getChurnType()).isEqualTo(ReactivateResponse.ChurnType.INVOLUNTARY);
        assertThat(resp.getPendingPaymentId()).isNotNull();
        assertThat(resp.getAmount()).isEqualTo(500_000L);
        assertThat(sub.getPendingPaymentId()).isEqualTo(resp.getPendingPaymentId());
        verify(subscriptionRepository).save(sub);
    }

    @Test
    @DisplayName("GAP-1263-BE: CANCELLED sub on suspended instance → voluntary churn classification")
    void reactivate_voluntaryCancel() {
        Instance instance = new Instance();
        instance.setId(instanceId);
        instance.setStatus(InstanceStatus.SUSPENDED);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        Subscription sub = new Subscription();
        sub.setId(UUID.randomUUID());
        sub.setInstanceId(instanceId);
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setTier(PricingTier.PREMIUM);
        sub.setBillingCycle(BillingCycle.MONTHLY);
        sub.setPriceVnd(1_500_000L);
        sub.setCreatedAt(LocalDateTime.now());
        when(subscriptionRepository.findByInstanceId(instanceId)).thenReturn(List.of(sub));

        lenient().when(vietQRService.generateQRCode(any(UUID.class), anyLong(), anyString()))
            .thenReturn("https://img.vietqr.io/x.png");
        lenient().when(vietQRService.getBankCode()).thenReturn("VCB");
        lenient().when(vietQRService.getAccountNumber()).thenReturn("123");
        lenient().when(vietQRService.getAccountName()).thenReturn("KITECLASS");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });

        ReactivateResponse resp = service.reactivate(instanceId);

        assertThat(resp.getChurnType()).isEqualTo(ReactivateResponse.ChurnType.VOLUNTARY);
        assertThat(resp.getOutcome()).isEqualTo(ReactivateResponse.Outcome.PAYMENT_REQUIRED);
    }

    @Test
    @DisplayName("GAP-1263-BE: idempotent — existing pending payment is returned, no new payment")
    void reactivate_idempotent() {
        Instance instance = new Instance();
        instance.setId(instanceId);
        instance.setStatus(InstanceStatus.SUSPENDED);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        UUID existingPayId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setId(UUID.randomUUID());
        sub.setInstanceId(instanceId);
        sub.setStatus(SubscriptionStatus.EXPIRED);
        sub.setTier(PricingTier.BASIC);
        sub.setBillingCycle(BillingCycle.MONTHLY);
        sub.setPriceVnd(500_000L);
        sub.setPendingPaymentId(existingPayId);
        sub.setCreatedAt(LocalDateTime.now());
        when(subscriptionRepository.findByInstanceId(instanceId)).thenReturn(List.of(sub));

        Payment existing = new Payment();
        existing.setId(existingPayId);
        existing.setAmountVnd(500_000L);
        existing.setCurrency("VND");
        existing.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(existingPayId)).thenReturn(Optional.of(existing));

        ReactivateResponse resp = service.reactivate(instanceId);

        assertThat(resp.getOutcome()).isEqualTo(ReactivateResponse.Outcome.PAYMENT_REQUIRED);
        assertThat(resp.getPendingPaymentId()).isEqualTo(existingPayId);
        verify(paymentRepository, never()).save(any());
        verify(subscriptionRepository, never()).save(any());
    }
}
