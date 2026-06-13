package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.SubscriptionConfig;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import com.kitehub.subscription.service.SubscriptionRenewalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for SubscriptionExpirationChecker scheduler.
 * Validates renewal reminders and expired subscription processing.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionExpirationCheckerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRenewalService renewalService;

    @Mock
    private EmailServiceClient emailServiceClient;

    @Mock
    private SubscriptionConfig subscriptionConfig;

    @Mock
    private com.kitehub.subscription.notification.channel.OwnerNotificationDispatcher ownerNotificationDispatcher;

    @InjectMocks
    private SubscriptionExpirationChecker checker;

    private UUID instanceId;
    private UUID subscriptionId;
    private Instance instance;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();

        instance = new Instance();
        instance.setId(instanceId);
        instance.setOrganizationName("Test School");
        instance.setContactEmail("admin@test.edu.vn");

        subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setInstanceId(instanceId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(LocalDateTime.now().plusDays(3));
        subscription.setTier(PricingTier.BASIC);
        subscription.setPriceVnd(1_000_000L);
    }

    @Test
    void checkExpiringSubscriptions_sendsReminderAt7Days() {
        when(subscriptionConfig.getWarningDays()).thenReturn(List.of(7, 3, 1));
        when(subscriptionRepository.findExpiringBetween(any(), any(), eq(SubscriptionStatus.ACTIVE)))
            .thenReturn(List.of(subscription));
        when(renewalService.getDaysUntilExpiration(subscription)).thenReturn(7L);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        checker.checkExpiringSubscriptions();

        verify(emailServiceClient).sendRenewalReminder(
            anyString(), anyString(), eq(7L), anyString(), anyLong()
        );
    }

    @Test
    void checkExpiringSubscriptions_sendsReminderAt1Day() {
        when(subscriptionConfig.getWarningDays()).thenReturn(List.of(7, 3, 1));
        when(subscriptionRepository.findExpiringBetween(any(), any(), eq(SubscriptionStatus.ACTIVE)))
            .thenReturn(List.of(subscription));
        when(renewalService.getDaysUntilExpiration(subscription)).thenReturn(1L);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        checker.checkExpiringSubscriptions();

        verify(emailServiceClient).sendRenewalReminder(
            anyString(), anyString(), eq(1L), anyString(), anyLong()
        );
    }

    @Test
    void checkExpiringSubscriptions_skipsWhenDaysNotInWarningList() {
        when(subscriptionConfig.getWarningDays()).thenReturn(List.of(7, 3, 1));
        when(subscriptionRepository.findExpiringBetween(any(), any(), eq(SubscriptionStatus.ACTIVE)))
            .thenReturn(List.of(subscription));
        when(renewalService.getDaysUntilExpiration(subscription)).thenReturn(5L);

        checker.checkExpiringSubscriptions();

        verifyNoInteractions(emailServiceClient);
    }

    @Test
    void checkExpiringSubscriptions_skipsWhenInstanceNotFound() {
        when(subscriptionConfig.getWarningDays()).thenReturn(List.of(7, 3, 1));
        when(subscriptionRepository.findExpiringBetween(any(), any(), eq(SubscriptionStatus.ACTIVE)))
            .thenReturn(List.of(subscription));
        when(renewalService.getDaysUntilExpiration(subscription)).thenReturn(7L);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.empty());

        checker.checkExpiringSubscriptions();

        verifyNoInteractions(emailServiceClient);
    }

    @Test
    void processExpiredSubscriptions_marksActiveSubscriptionAsExpired() {
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findExpiredSubscriptions(any())).thenReturn(List.of(subscription));
        when(renewalService.isInGracePeriod(subscription)).thenReturn(true);

        checker.processExpiredSubscriptions();

        verify(subscriptionRepository).save(subscription);
        assert subscription.getStatus() == SubscriptionStatus.EXPIRED;
    }

    @Test
    void processExpiredSubscriptions_suspendsAfterGracePeriod() {
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findExpiredSubscriptions(any())).thenReturn(List.of(subscription));
        when(renewalService.isInGracePeriod(subscription)).thenReturn(false);

        checker.processExpiredSubscriptions();

        verify(renewalService).suspendExpiredSubscription(subscriptionId);
    }

    @Test
    void processExpiredSubscriptions_sendsWinBackAfterInvoluntarySuspend() {
        // GAP-1263: non-payment lapse → win-back outreach (voluntary=false).
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findExpiredSubscriptions(any())).thenReturn(List.of(subscription));
        when(renewalService.isInGracePeriod(subscription)).thenReturn(false);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        checker.processExpiredSubscriptions();

        verify(renewalService).suspendExpiredSubscription(subscriptionId);
        verify(ownerNotificationDispatcher).sendWinBack(instance, false);
    }

    @Test
    void processExpiredSubscriptions_sendsWinBackAfterCancelledExpiredSuspend() {
        // GAP-1263: voluntary cancel that has lapsed → win-back outreach (voluntary=true).
        when(subscriptionRepository.findExpiredSubscriptions(any())).thenReturn(List.of());
        when(subscriptionRepository.findCancelledExpiredSubscriptions(any())).thenReturn(List.of(subscription));
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        checker.processExpiredSubscriptions();

        verify(renewalService).suspendCancelledExpired(subscriptionId);
        verify(ownerNotificationDispatcher).sendWinBack(instance, true);
    }

    @Test
    void processExpiredSubscriptions_winBackFailureDoesNotBreakSweep() {
        // GAP-1263: notification miss must never abort the scheduler sweep.
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findExpiredSubscriptions(any())).thenReturn(List.of(subscription));
        when(renewalService.isInGracePeriod(subscription)).thenReturn(false);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        doThrow(new RuntimeException("dispatcher down"))
            .when(ownerNotificationDispatcher).sendWinBack(any(), anyBoolean());

        // Should not throw — win-back failure swallowed.
        checker.processExpiredSubscriptions();

        verify(renewalService).suspendExpiredSubscription(subscriptionId);
    }

    @Test
    void processExpiredSubscriptions_doesNotSuspendDuringGracePeriod() {
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findExpiredSubscriptions(any())).thenReturn(List.of(subscription));
        when(renewalService.isInGracePeriod(subscription)).thenReturn(true);

        checker.processExpiredSubscriptions();

        verify(renewalService, never()).suspendExpiredSubscription(any());
    }

    @Test
    void processExpiredSubscriptions_continuesOnError() {
        Subscription badSubscription = new Subscription();
        badSubscription.setId(UUID.randomUUID());
        badSubscription.setStatus(SubscriptionStatus.ACTIVE);
        badSubscription.setInstanceId(UUID.randomUUID());

        when(subscriptionRepository.findExpiredSubscriptions(any()))
            .thenReturn(List.of(badSubscription, subscription));
        when(renewalService.isInGracePeriod(any())).thenThrow(new RuntimeException("DB error"));

        // Should not throw — continues processing
        checker.processExpiredSubscriptions();
    }

    @Test
    void checkExpiringSubscriptions_noExpiringSubscriptions_doesNothing() {
        when(subscriptionRepository.findExpiringBetween(any(), any(), eq(SubscriptionStatus.ACTIVE)))
            .thenReturn(List.of());

        checker.checkExpiringSubscriptions();

        verifyNoInteractions(emailServiceClient);
    }

    // ---- GAP-1259 (SUB-23): grace dunning + pending-payment TTL ----

    @Test
    void processExpiredSubscriptions_emitsGraceDunningReminderDuringGrace() {
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscription.setExpiresAt(LocalDateTime.now().minusDays(1)); // expired, within 3d grace
        when(subscriptionRepository.findExpiredSubscriptions(any())).thenReturn(List.of(subscription));
        when(renewalService.isInGracePeriod(subscription)).thenReturn(true);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(subscriptionConfig.getGracePeriodDays()).thenReturn(3);

        checker.processExpiredSubscriptions();

        verify(renewalService, never()).suspendExpiredSubscription(any());
        verify(emailServiceClient).sendRenewalReminder(
            eq(instanceId), eq("admin@test.edu.vn"), eq("Test School"),
            anyLong(), eq("BASIC"), anyLong());
    }

    @Test
    void processStalePendingPayments_expiresOldPaymentAndReleasesSubscription() {
        Payment stale = new Payment();
        stale.setId(UUID.randomUUID());
        stale.setSubscriptionId(subscriptionId);
        stale.setStatus(PaymentStatus.PENDING);
        stale.setCreatedAt(LocalDateTime.now().minusDays(10)); // older than 7-day TTL
        subscription.setPendingPaymentId(stale.getId());

        when(subscriptionConfig.getPendingPaymentTtlDays()).thenReturn(7);
        when(paymentRepository.findPendingPayments()).thenReturn(List.of(stale));
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        checker.processStalePendingPayments();

        assertEquals(PaymentStatus.FAILED, stale.getStatus());
        verify(paymentRepository).save(stale);
        assertNull(subscription.getPendingPaymentId());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void processStalePendingPayments_keepsPaymentWithinTtl() {
        Payment fresh = new Payment();
        fresh.setId(UUID.randomUUID());
        fresh.setSubscriptionId(subscriptionId);
        fresh.setStatus(PaymentStatus.PENDING);
        fresh.setCreatedAt(LocalDateTime.now().minusDays(2)); // within 7-day TTL

        when(subscriptionConfig.getPendingPaymentTtlDays()).thenReturn(7);
        when(paymentRepository.findPendingPayments()).thenReturn(List.of(fresh));

        checker.processStalePendingPayments();

        assertEquals(PaymentStatus.PENDING, fresh.getStatus());
        verify(paymentRepository, never()).save(any());
    }

    // ---- GAP-1080 AC#2: orphan PENDING subscription cleanup ----

    @Test
    void processOrphanPendingSubscriptions_softDeletesStalePending() {
        Subscription orphan = new Subscription();
        orphan.setId(UUID.randomUUID());
        orphan.setStatus(SubscriptionStatus.PENDING);
        orphan.setCreatedAt(LocalDateTime.now().minusDays(10)); // older than 7-day TTL

        when(subscriptionConfig.getOrphanPendingSubscriptionTtlDays()).thenReturn(7);
        when(subscriptionRepository.findByStatus(SubscriptionStatus.PENDING))
            .thenReturn(List.of(orphan));

        checker.processOrphanPendingSubscriptions();

        assertTrue(orphan.isDeleted());
        verify(subscriptionRepository).save(orphan);
    }

    @Test
    void processOrphanPendingSubscriptions_keepsRecentPending() {
        Subscription recent = new Subscription();
        recent.setId(UUID.randomUUID());
        recent.setStatus(SubscriptionStatus.PENDING);
        recent.setCreatedAt(LocalDateTime.now().minusDays(1)); // within 7-day TTL

        when(subscriptionConfig.getOrphanPendingSubscriptionTtlDays()).thenReturn(7);
        when(subscriptionRepository.findByStatus(SubscriptionStatus.PENDING))
            .thenReturn(List.of(recent));

        checker.processOrphanPendingSubscriptions();

        assertFalse(recent.isDeleted());
        verify(subscriptionRepository, never()).save(any());
    }
}
