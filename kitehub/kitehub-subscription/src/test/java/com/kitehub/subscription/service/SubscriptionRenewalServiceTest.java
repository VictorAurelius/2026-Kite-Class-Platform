package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.config.SubscriptionConfig;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubscriptionRenewalService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionRenewalService Unit Tests")
class SubscriptionRenewalServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionConfig subscriptionConfig;

    @Mock
    private VietQRService vietQRService;

    @InjectMocks
    private SubscriptionRenewalService renewalService;

    private UUID subscriptionId;
    private UUID instanceId;
    private Subscription subscription;
    private Instance instance;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        instanceId = UUID.randomUUID();

        subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setPriceVnd(500_000L);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(LocalDateTime.now().plusDays(1));
        subscription.setAutoRenew(true);

        instance = new Instance();
        instance.setId(instanceId);
        instance.setStatus(InstanceStatus.ACTIVE);

        lenient().when(subscriptionConfig.getGracePeriodDays()).thenReturn(3);

        // VietQR mocks — GAP-939: SubscriptionRenewalService now snapshots bank/account info
        lenient().when(vietQRService.generateQRCode(any(UUID.class), any(Long.class), any(UUID.class)))
            .thenReturn("https://img.vietqr.io/image/VCB-1234567890-compact.png");
        lenient().when(vietQRService.generatePaymentContent(any(UUID.class)))
            .thenReturn("KITECLASS RENEWAL");
        lenient().when(vietQRService.getBankCode()).thenReturn("VCB");
        lenient().when(vietQRService.getAccountNumber()).thenReturn("1234567890");
        lenient().when(vietQRService.getAccountName()).thenReturn("CONG TY KITECLASS");
    }

    @Test
    @DisplayName("Should process renewal and create payment invoice")
    void shouldProcessRenewalAndCreatePayment() {
        // Given
        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setAmountVnd(500_000L);
        savedPayment.setStatus(PaymentStatus.PENDING);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        // When
        boolean result = renewalService.processRenewal(subscriptionId);

        // Then
        assertThat(result).isTrue();
        verify(paymentRepository).save(any(Payment.class));
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should create correct payment invoice for renewal")
    void shouldCreateCorrectPaymentInvoice() {
        // Given
        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        // When
        renewalService.processRenewal(subscriptionId);

        // Then
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment capturedPayment = paymentCaptor.getValue();
        assertThat(capturedPayment.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(capturedPayment.getAmountVnd()).isEqualTo(500_000L);
        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(capturedPayment.getPaymentContent()).contains("renewal");
    }

    @Test
    @DisplayName("Should apply pending tier during renewal")
    void shouldApplyPendingTierDuringRenewal() {
        // Given
        subscription.setPendingTier(PricingTier.PREMIUM);

        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        // When
        renewalService.processRenewal(subscriptionId);

        // Then
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());

        Subscription saved = captor.getValue();
        assertThat(saved.getTier()).isEqualTo(PricingTier.PREMIUM);
        assertThat(saved.getPendingTier()).isNull();
    }

    @Test
    @DisplayName("Should not renew when auto-renew is disabled")
    void shouldNotRenewWhenAutoRenewDisabled() {
        // Given
        subscription.setAutoRenew(false);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        // When
        boolean result = renewalService.processRenewal(subscriptionId);

        // Then
        assertThat(result).isFalse();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
        // Given
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> renewalService.processRenewal(subscriptionId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Subscription not found");
    }

    @Test
    @DisplayName("Should manually renew subscription")
    void shouldManuallyRenewSubscription() {
        // Given
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

        // When
        renewalService.manualRenewal(subscriptionId);

        // Then
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("KH-5 FM-2: should reject manual renew of a PENDING subscription (null expiry) with 400 not 500")
    void shouldRejectManualRenewOfPendingSubscription() {
        // Given — a freshly created subscription is PENDING with null expiresAt until activation
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setExpiresAt(null);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        // When & Then — must surface IllegalArgumentException (→ 400), not NPE (→ 500)
        assertThatThrownBy(() -> renewalService.manualRenewal(subscriptionId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has not been activated");
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should check if subscription is in grace period")
    void shouldCheckIfInGracePeriod() {
        // Given
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscription.setExpiresAt(LocalDateTime.now().minusDays(1));

        // When
        boolean isInGracePeriod = renewalService.isInGracePeriod(subscription);

        // Then
        assertThat(isInGracePeriod).isTrue();
    }

    @Test
    @DisplayName("Should not be in grace period when not expired")
    void shouldNotBeInGracePeriodWhenNotExpired() {
        // Given
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        // When
        boolean isInGracePeriod = renewalService.isInGracePeriod(subscription);

        // Then
        assertThat(isInGracePeriod).isFalse();
    }

    @Test
    @DisplayName("Should calculate days until expiration correctly")
    void shouldCalculateDaysUntilExpiration() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        subscription.setExpiresAt(now.plusDays(10));

        // When
        long days = renewalService.getDaysUntilExpiration(subscription);

        // Then
        // ChronoUnit.DAYS.between truncates partial days, so allow 9 or 10
        assertThat(days).isIn(9L, 10L);
    }
}
