package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.dto.CreateSubscriptionRequest;
import com.kitehub.subscription.dto.SubscriptionResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubscriptionService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService Unit Tests")
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private VietQRService vietQRService;

    @Mock
    private com.kitehub.subscription.client.EmailServiceClient emailServiceClient;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private UUID instanceId;
    private Instance instance;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();
        instance = new Instance();
        instance.setId(instanceId);
        instance.setStatus(InstanceStatus.TRIAL);
    }

    @Test
    @DisplayName("Should create PENDING subscription with payment gate (SUB-20)")
    void shouldCreatePendingSubscriptionForPaidTier() {
        // Given — Owner submits create request for paid tier (per SUB-20)
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
            .instanceId(instanceId)
            .tier(PricingTier.BASIC)
            .billingCycle(BillingCycle.MONTHLY)
            .autoRenew(true)
            .build();

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(subscriptionRepository.findActiveByInstanceId(instanceId)).thenReturn(Optional.empty());
        // Use any() (matches null) — first save() happens before ID is generated.
        when(vietQRService.generatePaymentContent(any())).thenReturn("KITEHUB ABCD1234");
        when(vietQRService.generateQRCode(any(), any(), any()))
            .thenReturn("https://qr.example/payment.png");
        when(vietQRService.getBankCode()).thenReturn("VCB");
        when(vietQRService.getAccountNumber()).thenReturn("1234567890");
        when(vietQRService.getAccountName()).thenReturn("CONG TY KITECLASS");

        // Save subscription returns the same entity passed in (so subsequent mutations stick).
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UUID paymentId = UUID.randomUUID();
        Payment savedPayment = new Payment();
        savedPayment.setId(paymentId);
        savedPayment.setAmountVnd(500_000L);
        savedPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // When
        SubscriptionResponse response = subscriptionService.createSubscription(request);

        // Then — response carries PENDING shape per SUB-20 + api-contract.md
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(response.getTier()).isEqualTo(PricingTier.FREE);
        assertThat(response.getPendingTier()).isEqualTo(PricingTier.BASIC);
        assertThat(response.getPendingPaymentId()).isEqualTo(paymentId);
        assertThat(response.getPriceVnd()).isEqualTo(500_000L);
        assertThat(response.getStartedAt()).isNull();
        assertThat(response.getExpiresAt()).isNull();

        // Instance must NOT be activated, no email yet (deferred to applyPendingUpgrade).
        verify(instanceRepository, never()).save(any(Instance.class));
        verify(emailServiceClient, never()).sendSubscriptionCreatedEmail(
            any(UUID.class), any(String.class), any(String.class), any(String.class), any(String.class));

        // Payment PENDING was spawned via VietQR helper.
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment capturedPayment = paymentCaptor.getValue();
        assertThat(capturedPayment.getAmountVnd()).isEqualTo(500_000L);
        assertThat(capturedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.VIETQR);
        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(capturedPayment.getPaymentContent()).isEqualTo("KITEHUB ABCD1234");
    }

    @Test
    @DisplayName("applyPendingUpgrade on PENDING create-flow flips ACTIVE + activates instance + sends email")
    void shouldActivatePendingCreateOnPaymentConfirm() {
        // Given — subscription persisted via createSubscription (PENDING/FREE/pendingTier=BASIC)
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.FREE);
        subscription.setPendingTier(PricingTier.BASIC);
        subscription.setPendingPaymentId(paymentId);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setPriceVnd(500_000L);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(instance);

        // When
        subscriptionService.applyPendingUpgrade(subscriptionId, paymentId);

        // Then — flip to ACTIVE/BASIC + instance ACTIVE + email sent
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getTier()).isEqualTo(PricingTier.BASIC);
        assertThat(subscription.getPendingTier()).isNull();
        assertThat(subscription.getPendingPaymentId()).isNull();
        assertThat(subscription.getStartedAt()).isNotNull();
        assertThat(subscription.getExpiresAt()).isNotNull();

        verify(instanceRepository).save(any(Instance.class));
        assertThat(instance.getStatus()).isEqualTo(InstanceStatus.ACTIVE);

        verify(emailServiceClient).sendSubscriptionCreatedEmail(
            eq(instanceId),
            any(),
            any(),
            eq(PricingTier.BASIC.name()),
            eq(BillingCycle.MONTHLY.name())
        );
    }

    @Test
    @DisplayName("clearPendingUpgrade on PENDING create-flow cancels subscription")
    void shouldCancelPendingCreateOnPaymentReject() {
        // Given — subscription PENDING/FREE awaiting payment, admin rejects
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setTier(PricingTier.FREE);
        subscription.setPendingTier(PricingTier.BASIC);
        subscription.setPendingPaymentId(paymentId);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setAutoRenew(true);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        subscriptionService.clearPendingUpgrade(subscriptionId, paymentId);

        // Then — cancelled cleanly so owner can submit fresh request
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(subscription.getPendingTier()).isNull();
        assertThat(subscription.getPendingPaymentId()).isNull();
        assertThat(subscription.getAutoRenew()).isFalse();
    }

    @Test
    @DisplayName("Should calculate prorated charge correctly")
    void shouldCalculateProratedChargeCorrectly() {
        // When
        long prorated = subscriptionService.calculateProratedCharge(
            PricingTier.BASIC,      // 500k/month
            PricingTier.PREMIUM,    // 1.5M/month
            15,                      // 15 days left
            BillingCycle.MONTHLY
        );

        // Then
        // Price diff = 1.5M - 500k = 1M
        // Daily rate = 1M / 30 = 33,333
        // Prorated = 33,333 * 15 = 500,000
        assertThat(prorated).isEqualTo(500_000L);
    }

    @Test
    @DisplayName("Upgrade should keep current tier until pending payment is confirmed")
    void shouldUpgradeSubscriptionByCreatingPendingPaymentOnly() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPriceVnd(PricingTier.BASIC.getPrice(BillingCycle.MONTHLY));
        subscription.setExpiresAt(java.time.LocalDateTime.now().plusDays(15));

        Payment savedPayment = new Payment();
        savedPayment.setId(paymentId);
        savedPayment.setSubscriptionId(subscriptionId);
        savedPayment.setAmountVnd(500_000L);
        savedPayment.setStatus(PaymentStatus.PENDING);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(vietQRService.generatePaymentContent(subscriptionId)).thenReturn("KITEHUB ABCD1234");
        when(vietQRService.generateQRCode(any(UUID.class), any(Long.class), eq(subscriptionId))).thenReturn("https://qr.example/payment.png");
        when(vietQRService.getBankCode()).thenReturn("VCB");
        when(vietQRService.getAccountNumber()).thenReturn("1234567890");
        when(vietQRService.getAccountName()).thenReturn("CONG TY KITECLASS");
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        // When
        SubscriptionResponse response = subscriptionService.upgradeSubscription(subscriptionId, PricingTier.PREMIUM);

        // Then
        assertThat(response.getTier()).isEqualTo(PricingTier.BASIC);
        assertThat(response.getPendingTier()).isEqualTo(PricingTier.PREMIUM);
        assertThat(response.getPendingPaymentId()).isEqualTo(paymentId);
        verify(paymentRepository).save(any(Payment.class));
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Upgrade payment should contain VietQR manual-transfer details")
    void shouldCreateVietQrPaymentForUpgrade() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(java.time.LocalDateTime.now().plusDays(15));

        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setSubscriptionId(subscriptionId);
        savedPayment.setAmountVnd(500_000L);
        savedPayment.setStatus(PaymentStatus.PENDING);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(vietQRService.generatePaymentContent(subscriptionId)).thenReturn("KITEHUB ABCD1234");
        when(vietQRService.generateQRCode(any(UUID.class), any(Long.class), eq(subscriptionId))).thenReturn("https://qr.example/payment.png");
        when(vietQRService.getBankCode()).thenReturn("VCB");
        when(vietQRService.getAccountNumber()).thenReturn("1234567890");
        when(vietQRService.getAccountName()).thenReturn("CONG TY KITECLASS");
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        // When
        subscriptionService.upgradeSubscription(subscriptionId, PricingTier.PREMIUM);

        // Then
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment capturedPayment = paymentCaptor.getValue();
        assertThat(capturedPayment.getSubscriptionId()).isEqualTo(subscriptionId);
        // Prorated charge for 14 days left: (1.5M - 500k) / 30 * 14 = 466,667 VNĐ.
        // savedPayment mock returns 500k, but the captured argument carries the real prorated amount.
        assertThat(capturedPayment.getAmountVnd()).isCloseTo(466_667L, org.assertj.core.data.Offset.offset(100L));
        assertThat(capturedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.VIETQR);
        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(capturedPayment.getPaymentContent()).isEqualTo("KITEHUB ABCD1234");
        assertThat(capturedPayment.getQrCodeUrl()).isEqualTo("https://qr.example/payment.png");
        assertThat(capturedPayment.getBankCode()).isEqualTo("VCB");
        assertThat(capturedPayment.getAccountNumber()).isEqualTo("1234567890");
        assertThat(capturedPayment.getAccountName()).isEqualTo("CONG TY KITECLASS");
    }


    @Test
    @DisplayName("Should apply pending upgrade after matching payment is confirmed")
    void shouldApplyPendingUpgrade() {
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setPendingTier(PricingTier.PREMIUM);
        subscription.setPendingPaymentId(paymentId);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.applyPendingUpgrade(subscriptionId, paymentId);

        assertThat(subscription.getTier()).isEqualTo(PricingTier.PREMIUM);
        assertThat(subscription.getPriceVnd()).isEqualTo(PricingTier.PREMIUM.getPrice(BillingCycle.MONTHLY));
        assertThat(subscription.getPendingTier()).isNull();
        assertThat(subscription.getPendingPaymentId()).isNull();
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    @DisplayName("Should clear pending upgrade after payment rejection without changing current tier")
    void shouldClearPendingUpgrade() {
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setPendingTier(PricingTier.PREMIUM);
        subscription.setPendingPaymentId(paymentId);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.clearPendingUpgrade(subscriptionId, paymentId);

        assertThat(subscription.getTier()).isEqualTo(PricingTier.BASIC);
        assertThat(subscription.getPendingTier()).isNull();
        assertThat(subscription.getPendingPaymentId()).isNull();
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    @DisplayName("Should store pending tier for downgrade")
    void shouldStorePendingTierForDowngrade() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setTier(PricingTier.PREMIUM);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(java.time.LocalDateTime.now().plusDays(15));

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        // When
        SubscriptionResponse response = subscriptionService.downgradeSubscription(subscriptionId, PricingTier.BASIC);

        // Then
        assertThat(response).isNotNull();

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());

        Subscription saved = captor.getValue();
        assertThat(saved.getPendingTier()).isEqualTo(PricingTier.BASIC);
        assertThat(saved.getTier()).isEqualTo(PricingTier.PREMIUM); // Current tier unchanged
    }

    @Test
    @DisplayName("Should throw exception when upgrading to lower tier")
    void shouldThrowExceptionWhenUpgradingToLowerTier() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setTier(PricingTier.PREMIUM);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        // When & Then
        assertThatThrownBy(() -> subscriptionService.upgradeSubscription(subscriptionId, PricingTier.BASIC))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("only upgrade to higher tier");
    }

    @Test
    @DisplayName("Should cancel subscription immediately")
    void shouldCancelSubscriptionImmediately() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(java.time.LocalDateTime.now().plusDays(15));

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        // When
        subscriptionService.cancelSubscription(subscriptionId, true);

        // Then
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        
        Subscription saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(saved.getAutoRenew()).isFalse();
    }
}
