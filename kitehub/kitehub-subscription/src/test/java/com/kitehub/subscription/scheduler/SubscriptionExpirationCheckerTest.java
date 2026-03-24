package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.SubscriptionConfig;
import com.kitehub.subscription.repository.InstanceRepository;
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

import static org.mockito.ArgumentMatchers.any;
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
    private SubscriptionRenewalService renewalService;

    @Mock
    private EmailServiceClient emailServiceClient;

    @Mock
    private SubscriptionConfig subscriptionConfig;

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
            eq("admin@test.edu.vn"),
            eq("Test School"),
            eq(7L),
            any(),
            any()
        );
    }

    @Test
    void checkExpiringSubscriptions_sendsReminderAt3Days() {
        when(subscriptionConfig.getWarningDays()).thenReturn(List.of(7, 3, 1));
        when(subscriptionRepository.findExpiringBetween(any(), any(), eq(SubscriptionStatus.ACTIVE)))
            .thenReturn(List.of(subscription));
        when(renewalService.getDaysUntilExpiration(subscription)).thenReturn(3L);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        checker.checkExpiringSubscriptions();

        verify(emailServiceClient).sendRenewalReminder(
            eq("admin@test.edu.vn"), eq("Test School"), eq(3L), any(), any()
        );
    }

    @Test
    void checkExpiringSubscriptions_skipsWhenDaysNotInWarningList() {
        when(subscriptionConfig.getWarningDays()).thenReturn(List.of(7, 3, 1));
        when(subscriptionRepository.findExpiringBetween(any(), any(), eq(SubscriptionStatus.ACTIVE)))
            .thenReturn(List.of(subscription));
        when(renewalService.getDaysUntilExpiration(subscription)).thenReturn(5L);

        checker.checkExpiringSubscriptions();

        verify(emailServiceClient, never()).sendRenewalReminder(any(), any(), any(), any(), any());
    }

    @Test
    void checkExpiringSubscriptions_skipsWhenInstanceNotFound() {
        when(subscriptionConfig.getWarningDays()).thenReturn(List.of(7, 3, 1));
        when(subscriptionRepository.findExpiringBetween(any(), any(), eq(SubscriptionStatus.ACTIVE)))
            .thenReturn(List.of(subscription));
        when(renewalService.getDaysUntilExpiration(subscription)).thenReturn(7L);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.empty());

        checker.checkExpiringSubscriptions();

        verify(emailServiceClient, never()).sendRenewalReminder(any(), any(), any(), any(), any());
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

        when(subscriptionRepository.findExpiredSubscriptions(any()))
            .thenReturn(List.of(badSubscription, subscription));
        when(renewalService.isInGracePeriod(any())).thenThrow(new RuntimeException("DB error"));

        // Should not throw — continues processing other subscriptions
        checker.processExpiredSubscriptions();
    }

    @Test
    void checkExpiringSubscriptions_noExpiringSubscriptions_doesNothing() {
        when(subscriptionConfig.getWarningDays()).thenReturn(List.of(7, 3, 1));
        when(subscriptionRepository.findExpiringBetween(any(), any(), eq(SubscriptionStatus.ACTIVE)))
            .thenReturn(List.of());

        checker.checkExpiringSubscriptions();

        verify(emailServiceClient, never()).sendRenewalReminder(any(), any(), any(), any(), any());
    }
}
