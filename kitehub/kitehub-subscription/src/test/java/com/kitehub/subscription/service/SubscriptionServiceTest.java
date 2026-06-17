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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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

    // GAP-1256: real stateless helper (not a mock) so instances.tier sync actually runs
    // and tier assertions hold; @InjectMocks wires the spy via the constructor.
    @Spy
    private InstanceTierSyncService instanceTierSyncService = new InstanceTierSyncService();

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

        // createSubscription does an existence check only (existsById), not findById,
        // since #2160 removed the unused instance local var.
        when(instanceRepository.existsById(instanceId)).thenReturn(true);
        when(subscriptionRepository.findActiveByInstanceId(instanceId)).thenReturn(Optional.empty());
        // Use any() (matches null) — first save() happens before ID is generated.
        // GAP-1087 / Bug D: createPendingPayment passes the String txnRef as the QR memo.
        when(vietQRService.generateQRCode(any(UUID.class), any(Long.class), anyString()))
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
        // GAP-1087 / Bug D: paymentContent == txnRef (KH3SUB token SePay matches on).
        assertThat(capturedPayment.getPaymentContent())
            .isEqualTo(capturedPayment.getTxnRef())
            .matches("KH3SUB[A-F0-9]{8}");
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
        // GAP-1090 (SUB-21): create-flow must sync instances.tier to the activated paid tier.
        assertThat(instance.getTier()).isEqualTo(PricingTier.BASIC);

        verify(emailServiceClient).sendSubscriptionCreatedEmail(
            eq(instanceId),
            any(),
            any(),
            eq(PricingTier.BASIC.name()),
            eq(BillingCycle.MONTHLY.name())
        );
    }

    @Test
    @DisplayName("GAP-974: applyPendingUpgrade on ACTIVE upgrade-flow sends subscription-activated email")
    void shouldSendActivatedEmailOnUpgradeFlow() {
        // Given — an already-ACTIVE subscription upgrading BASIC -> PREMIUM after payment
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setPendingTier(PricingTier.PREMIUM);
        subscription.setPendingPaymentId(paymentId);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(LocalDateTime.now().plusMonths(1));

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        subscriptionService.applyPendingUpgrade(subscriptionId, paymentId);

        // Then — tier upgraded, no create-flow re-activation, activated email sent
        assertThat(subscription.getTier()).isEqualTo(PricingTier.PREMIUM);
        // GAP-1090 (SUB-21): upgrade-flow now syncs instances.tier to the new tier + saves it
        // (previously the upgrade branch only loaded the instance to send the email).
        verify(instanceRepository).save(any(Instance.class));
        assertThat(instance.getTier()).isEqualTo(PricingTier.PREMIUM);
        verify(emailServiceClient, never()).sendSubscriptionCreatedEmail(
            any(), any(), any(), any(), any());
        verify(emailServiceClient).sendSubscriptionActivatedEmail(
            eq(instanceId),
            any(),
            any(),
            eq(PricingTier.PREMIUM.name()),
            any()
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
        // GAP-1087 / Bug D: createPendingPayment passes the String txnRef as the QR memo.
        when(vietQRService.generateQRCode(any(UUID.class), any(Long.class), anyString())).thenReturn("https://qr.example/payment.png");
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
        // GAP-1087 / Bug D: createPendingPayment passes the String txnRef as the QR memo.
        when(vietQRService.generateQRCode(any(UUID.class), any(Long.class), anyString())).thenReturn("https://qr.example/payment.png");
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
        // GAP-1087 / Bug D: paymentContent == txnRef (KH3SUB token SePay matches on).
        assertThat(capturedPayment.getPaymentContent())
            .isEqualTo(capturedPayment.getTxnRef())
            .matches("KH3SUB[A-F0-9]{8}");
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
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setPendingTier(PricingTier.PREMIUM);
        subscription.setPendingPaymentId(paymentId);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // GAP-1090 (SUB-21): upgrade-flow now loads + tier-syncs + saves the instance.
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        subscriptionService.applyPendingUpgrade(subscriptionId, paymentId);

        assertThat(subscription.getTier()).isEqualTo(PricingTier.PREMIUM);
        assertThat(subscription.getPriceVnd()).isEqualTo(PricingTier.PREMIUM.getPrice(BillingCycle.MONTHLY));
        assertThat(subscription.getPendingTier()).isNull();
        assertThat(subscription.getPendingPaymentId()).isNull();
        verify(subscriptionRepository).save(subscription);
        // GAP-1090 (SUB-21): instances.tier synced to the upgraded tier + persisted.
        assertThat(instance.getTier()).isEqualTo(PricingTier.PREMIUM);
        verify(instanceRepository).save(any(Instance.class));
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
    @DisplayName("KH-5 FM-5: should reject downgrade while a pending tier-change payment is in flight")
    void shouldRejectDowngradeWhenPendingPaymentExists() {
        // Given — an upgrade left a pending payment; downgrading now would corrupt the pair.
        // Uses PREMIUM->BASIC (a valid non-FREE downgrade) so the assertion isolates the FM-5
        // pending-payment guard, independent of the GAP-1018 bug-4 downgrade-to-FREE guard.
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setTier(PricingTier.PREMIUM);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(java.time.LocalDateTime.now().plusDays(15));
        subscription.setPendingTier(PricingTier.ENTERPRISE);
        subscription.setPendingPaymentId(UUID.randomUUID());

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        // When & Then
        assertThatThrownBy(() -> subscriptionService.downgradeSubscription(subscriptionId, PricingTier.BASIC))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("pending tier change payment");
        verify(subscriptionRepository, never()).save(any(Subscription.class));
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

    @Test
    @DisplayName("GAP-1080: create is idempotent — same-tier retry returns existing PENDING, no new row")
    void shouldReturnExistingPendingOnIdempotentCreate() {
        // Given — instance already has a PENDING subscription for BASIC (e.g. FE double-click)
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
            .instanceId(instanceId)
            .tier(PricingTier.BASIC)
            .billingCycle(BillingCycle.MONTHLY)
            .autoRenew(true)
            .build();

        UUID existingPaymentId = UUID.randomUUID();
        Subscription existing = new Subscription();
        existing.setId(UUID.randomUUID());
        existing.setInstanceId(instanceId);
        existing.setTier(PricingTier.FREE);
        existing.setPendingTier(PricingTier.BASIC);
        existing.setPendingPaymentId(existingPaymentId);
        existing.setStatus(SubscriptionStatus.PENDING);
        existing.setPriceVnd(500_000L);

        when(instanceRepository.existsById(instanceId)).thenReturn(true);
        when(subscriptionRepository.findActiveByInstanceId(instanceId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByInstanceId(instanceId))
            .thenReturn(java.util.List.of(existing));

        // When
        SubscriptionResponse response = subscriptionService.createSubscription(request);

        // Then — existing PENDING returned, NO new subscription / payment created
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(response.getPendingPaymentId()).isEqualTo(existingPaymentId);
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("GAP-1080: create with a different tier while a PENDING exists -> 409 conflict")
    void shouldRejectCreateWhenDifferentTierPending() {
        // Given — a PENDING BASIC exists, owner now requests PREMIUM
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
            .instanceId(instanceId)
            .tier(PricingTier.PREMIUM)
            .billingCycle(BillingCycle.MONTHLY)
            .autoRenew(true)
            .build();

        Subscription existing = new Subscription();
        existing.setId(UUID.randomUUID());
        existing.setInstanceId(instanceId);
        existing.setPendingTier(PricingTier.BASIC);
        existing.setStatus(SubscriptionStatus.PENDING);

        when(instanceRepository.existsById(instanceId)).thenReturn(true);
        when(subscriptionRepository.findActiveByInstanceId(instanceId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByInstanceId(instanceId))
            .thenReturn(java.util.List.of(existing));

        // When & Then — 409 (SubscriptionConflictException), no new payment
        assertThatThrownBy(() -> subscriptionService.createSubscription(request))
            .isInstanceOf(com.kitehub.subscription.exception.SubscriptionConflictException.class)
            .hasMessageContaining("pending subscription");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("GAP-1018 bug 4: downgrade to FREE is rejected (consistent with create per SUB-01)")
    void shouldRejectDowngradeToFree() {
        // Given — an ACTIVE BASIC subscription
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(LocalDateTime.now().plusDays(10));

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        // When & Then
        assertThatThrownBy(() -> subscriptionService.downgradeSubscription(subscriptionId, PricingTier.FREE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FREE");
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("GAP-1017: immediate cancel suspends the instance")
    void shouldSuspendInstanceOnImmediateCancel() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(LocalDateTime.now().plusDays(10));
        instance.setStatus(InstanceStatus.ACTIVE);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        subscriptionService.cancelSubscription(subscriptionId, true);

        // Then — instance suspended now
        assertThat(instance.getStatus()).isEqualTo(InstanceStatus.SUSPENDED);
        verify(instanceRepository).save(instance);
    }

    @Test
    @DisplayName("GAP-1017: end-of-cycle cancel does NOT suspend the instance now (scheduler handles at expiry)")
    void shouldNotSuspendInstanceOnEndOfCycleCancel() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(LocalDateTime.now().plusDays(10));

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        // When
        subscriptionService.cancelSubscription(subscriptionId, false);

        // Then — instance untouched at cancel time
        verify(instanceRepository, never()).findById(any());
        verify(instanceRepository, never()).save(any(Instance.class));
    }

    @Test
    @DisplayName("GAP-1016: confirmed renewal payment (no tier change) extends cycle + reactivates SUSPENDED instance")
    void shouldExtendCycleAndReactivateOnRenewalConfirm() {
        // Given — an EXPIRED subscription with a pending renewal payment (pendingTier null)
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        LocalDateTime oldExpiry = LocalDateTime.now().minusDays(1);
        subscription.setExpiresAt(oldExpiry);
        subscription.setPendingPaymentId(paymentId);
        instance.setStatus(InstanceStatus.SUSPENDED);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        subscriptionService.applyPendingUpgrade(subscriptionId, paymentId);

        // Then — cycle extended, status ACTIVE, instance reactivated, no tier change
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getTier()).isEqualTo(PricingTier.BASIC);
        assertThat(subscription.getExpiresAt()).isAfter(oldExpiry);
        assertThat(subscription.getPendingPaymentId()).isNull();
        assertThat(instance.getStatus()).isEqualTo(InstanceStatus.ACTIVE);
    }

    @Test
    @DisplayName("GAP-1018 bug 2: renewal confirm with scheduled downgrade applies downgrade + extends + syncs instance tier")
    void shouldApplyScheduledDowngradeOnRenewalConfirm() {
        // Given — PREMIUM ACTIVE with a scheduled downgrade to BASIC + a confirmed renewal payment
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.PREMIUM);
        subscription.setPendingTier(PricingTier.BASIC);     // scheduled downgrade
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        LocalDateTime oldExpiry = LocalDateTime.now().plusDays(2);
        subscription.setExpiresAt(oldExpiry);
        subscription.setPendingPaymentId(paymentId);
        instance.setStatus(InstanceStatus.ACTIVE);
        instance.setTier(PricingTier.PREMIUM);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        subscriptionService.applyPendingUpgrade(subscriptionId, paymentId);

        // Then — downgrade applied + cycle extended + instances.tier synced (GAP-1256 SUB-21)
        assertThat(subscription.getTier()).isEqualTo(PricingTier.BASIC);
        assertThat(subscription.getPendingTier()).isNull();
        assertThat(subscription.getPriceVnd()).isEqualTo(PricingTier.BASIC.getPrice(BillingCycle.MONTHLY));
        assertThat(subscription.getExpiresAt()).isAfter(oldExpiry);
        assertThat(subscription.getPendingPaymentId()).isNull();
        assertThat(instance.getTier()).isEqualTo(PricingTier.BASIC);
        verify(instanceRepository).save(any(Instance.class));
    }

    @Test
    @DisplayName("GAP-1471: cancel pending payment clears pending state + soft-cancels payment (upgrade-flow ACTIVE)")
    void shouldCancelPendingPayment() {
        // Given — an ACTIVE BASIC subscription with a pending PREMIUM upgrade payment in flight
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setPendingTier(PricingTier.PREMIUM);
        subscription.setPendingPaymentId(paymentId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setSubscriptionId(subscriptionId);
        payment.setStatus(PaymentStatus.PENDING);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SubscriptionResponse response = subscriptionService.cancelPendingPayment(subscriptionId);

        // Then — pending state cleared, current tier + ACTIVE status preserved
        assertThat(response.getPendingTier()).isNull();
        assertThat(response.getPendingPaymentId()).isNull();
        assertThat(response.getTier()).isEqualTo(PricingTier.BASIC);
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        // Payment soft-cancelled (status CANCELLED + deleted=true).
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.isDeleted()).isTrue();
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("GAP-1471: cancel pending payment on a create-flow PENDING subscription cancels it (owner can retry)")
    void shouldCancelCreateFlowSubscriptionOnPendingPaymentCancel() {
        // Given — a create-flow subscription (PENDING/FREE, pendingTier=BASIC) awaiting first payment
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.FREE);
        subscription.setPendingTier(PricingTier.BASIC);
        subscription.setPendingPaymentId(paymentId);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setAutoRenew(true);

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.PENDING);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SubscriptionResponse response = subscriptionService.cancelPendingPayment(subscriptionId);

        // Then — subscription CANCELLED so the GAP-1080 create-idempotency guard no longer re-blocks
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(response.getPendingTier()).isNull();
        assertThat(response.getPendingPaymentId()).isNull();
        assertThat(response.getAutoRenew()).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("GAP-1471: cancel pending payment rejects when no payment is pending")
    void shouldRejectCancelPendingPaymentWhenNoPending() {
        // Given — an ACTIVE subscription with no pending payment
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        // pendingPaymentId left null

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        // When & Then — 400 via IllegalArgumentException, nothing mutated
        assertThatThrownBy(() -> subscriptionService.cancelPendingPayment(subscriptionId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không có yêu cầu thanh toán");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("GAP-1471: cancel pending payment rejects when the payment is already confirmed (COMPLETED)")
    void shouldRejectCancelPendingPaymentWhenPaymentCompleted() {
        // Given — a confirmed payment (admin already reconciled) — owner cannot retract it
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setPendingTier(PricingTier.PREMIUM);
        subscription.setPendingPaymentId(paymentId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.COMPLETED);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When & Then — 400 via IllegalArgumentException, nothing mutated
        assertThatThrownBy(() -> subscriptionService.cancelPendingPayment(subscriptionId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã được xác nhận");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }
}
