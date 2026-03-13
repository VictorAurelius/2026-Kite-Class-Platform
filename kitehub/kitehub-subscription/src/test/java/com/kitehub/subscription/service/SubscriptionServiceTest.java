package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.InstanceStatus;
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
    @DisplayName("Should create subscription successfully")
    void shouldCreateSubscriptionSuccessfully() {
        // Given
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
            .instanceId(instanceId)
            .tier(PricingTier.BASIC)
            .billingCycle(BillingCycle.MONTHLY)
            .autoRenew(true)
            .build();

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(subscriptionRepository.findActiveByInstanceId(instanceId)).thenReturn(Optional.empty());

        Subscription savedSubscription = new Subscription();
        savedSubscription.setId(UUID.randomUUID());
        savedSubscription.setInstanceId(instanceId);
        savedSubscription.setTier(PricingTier.BASIC);
        savedSubscription.setBillingCycle(BillingCycle.MONTHLY);
        savedSubscription.setPriceVnd(500_000L);
        savedSubscription.setStatus(SubscriptionStatus.ACTIVE);
        savedSubscription.setStartedAt(java.time.LocalDateTime.now());
        savedSubscription.setExpiresAt(java.time.LocalDateTime.now().plusMonths(1));
        
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(savedSubscription);
        when(instanceRepository.save(any(Instance.class))).thenReturn(instance);

        // When
        SubscriptionResponse response = subscriptionService.createSubscription(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTier()).isEqualTo(PricingTier.BASIC);
        assertThat(response.getPriceVnd()).isEqualTo(500_000L);
        verify(instanceRepository).save(any(Instance.class));
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
    @DisplayName("Should upgrade subscription with prorated charge")
    void shouldUpgradeSubscription() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(java.time.LocalDateTime.now().plusDays(15));

        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setAmountVnd(500_000L);
        savedPayment.setStatus(PaymentStatus.PENDING);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        // When
        SubscriptionResponse response = subscriptionService.upgradeSubscription(subscriptionId, PricingTier.PREMIUM);

        // Then
        assertThat(response.getTier()).isEqualTo(PricingTier.PREMIUM);
        verify(paymentRepository).save(any(Payment.class));
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should create payment record for tier upgrade")
    void shouldCreatePaymentForUpgrade() {
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
        savedPayment.setAmountVnd(500_000L);
        savedPayment.setStatus(PaymentStatus.PENDING);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        // When
        subscriptionService.upgradeSubscription(subscriptionId, PricingTier.PREMIUM);

        // Then
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment capturedPayment = paymentCaptor.getValue();
        assertThat(capturedPayment.getSubscriptionId()).isEqualTo(subscriptionId);
        // Prorated charge for 14 days: (1.5M - 500k) / 30 * 14 = 466,667 VNĐ
        assertThat(capturedPayment.getAmountVnd()).isCloseTo(466_667L, org.assertj.core.data.Offset.offset(100L));
        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(capturedPayment.getPaymentContent()).contains("Prorated charge");
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
