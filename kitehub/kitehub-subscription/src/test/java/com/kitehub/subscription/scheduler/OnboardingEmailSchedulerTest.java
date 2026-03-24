package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

/**
 * Unit tests for OnboardingEmailScheduler.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingEmailScheduler Unit Tests")
class OnboardingEmailSchedulerTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private EmailServiceClient emailServiceClient;

    @InjectMocks
    private OnboardingEmailScheduler scheduler;

    private Instance trialInstance;

    @BeforeEach
    void setUp() {
        trialInstance = createInstance("test-org", InstanceStatus.TRIAL);
        // trialStartedAt is about 24 hours ago (within the 23-25h window)
        trialInstance.setTrialStartedAt(LocalDateTime.now().minusHours(24));
    }

    @Nested
    @DisplayName("checkAndSendOnboardingEmails")
    class CheckAndSendOnboardingEmails {

        @Test
        @DisplayName("Should send onboarding email when instance is in 23-25h window")
        void shouldSendOnboardingEmailForInstanceIn23To25hWindow() {
            // Given: TRIAL instance with trialStartedAt 24h ago (within window)
            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
                .thenReturn(Collections.singletonList(trialInstance));

            // When
            scheduler.checkAndSendOnboardingEmails();

            // Then
            verify(emailServiceClient).sendOnboardingTipsEmail(
                eq(trialInstance.getId()),
                eq(trialInstance.getContactEmail()),
                eq(trialInstance.getSubdomain())
            );
        }

        @Test
        @DisplayName("Should not send onboarding email when instance started less than 23h ago")
        void shouldNotSendWhenInstanceStartedLessThan23hAgo() {
            // Given: TRIAL instance started 22h ago (too early)
            trialInstance.setTrialStartedAt(LocalDateTime.now().minusHours(22));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
                .thenReturn(Collections.singletonList(trialInstance));

            // When
            scheduler.checkAndSendOnboardingEmails();

            // Then
            verify(emailServiceClient, never()).sendOnboardingTipsEmail(
                any(), any(), any()
            );
        }

        @Test
        @DisplayName("Should not send onboarding email when instance started more than 25h ago")
        void shouldNotSendWhenInstanceStartedMoreThan25hAgo() {
            // Given: TRIAL instance started 26h ago (too late)
            trialInstance.setTrialStartedAt(LocalDateTime.now().minusHours(26));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
                .thenReturn(Collections.singletonList(trialInstance));

            // When
            scheduler.checkAndSendOnboardingEmails();

            // Then
            verify(emailServiceClient, never()).sendOnboardingTipsEmail(
                any(), any(), any()
            );
        }

        @Test
        @DisplayName("Should not send when trialStartedAt is null")
        void shouldNotSendWhenTrialStartedAtIsNull() {
            // Given
            trialInstance.setTrialStartedAt(null);

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
                .thenReturn(Collections.singletonList(trialInstance));

            // When
            scheduler.checkAndSendOnboardingEmails();

            // Then
            verify(emailServiceClient, never()).sendOnboardingTipsEmail(
                any(), any(), any()
            );
        }

        @Test
        @DisplayName("Should not send when contactEmail is null")
        void shouldNotSendWhenContactEmailIsNull() {
            // Given
            trialInstance.setContactEmail(null);

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
                .thenReturn(Collections.singletonList(trialInstance));

            // When
            scheduler.checkAndSendOnboardingEmails();

            // Then
            verify(emailServiceClient, never()).sendOnboardingTipsEmail(
                any(), any(), any()
            );
        }

        @Test
        @DisplayName("Should handle empty list gracefully")
        void shouldHandleEmptyListGracefully() {
            // Given
            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
                .thenReturn(Collections.emptyList());

            // When
            scheduler.checkAndSendOnboardingEmails();

            // Then
            verify(emailServiceClient, never()).sendOnboardingTipsEmail(
                any(), any(), any()
            );
        }

        @Test
        @DisplayName("Should send to all qualifying instances")
        void shouldSendToAllQualifyingInstances() {
            // Given: 2 instances in the window
            Instance instance2 = createInstance("org-2", InstanceStatus.TRIAL);
            instance2.setTrialStartedAt(LocalDateTime.now().minusHours(23).minusMinutes(30));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
                .thenReturn(Arrays.asList(trialInstance, instance2));

            // When
            scheduler.checkAndSendOnboardingEmails();

            // Then: both instances should receive onboarding email
            verify(emailServiceClient, times(2)).sendOnboardingTipsEmail(
                any(), any(), any()
            );
        }

        @Test
        @DisplayName("Should handle exception for one instance without stopping others")
        void shouldHandleExceptionGracefully() {
            // Given: 2 instances, first one throws exception
            Instance instance2 = createInstance("org-2", InstanceStatus.TRIAL);
            instance2.setTrialStartedAt(LocalDateTime.now().minusHours(23).minusMinutes(30));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
                .thenReturn(Arrays.asList(trialInstance, instance2));

            org.mockito.Mockito.doThrow(new RuntimeException("Email service error"))
                .when(emailServiceClient).sendOnboardingTipsEmail(
                    eq(trialInstance.getId()), any(), any()
                );

            // When - should not throw
            scheduler.checkAndSendOnboardingEmails();

            // Then: second instance should still receive email
            verify(emailServiceClient).sendOnboardingTipsEmail(
                eq(instance2.getId()), any(), any()
            );
        }
    }

    /**
     * Helper method to create test instance.
     */
    private Instance createInstance(String subdomain, InstanceStatus status) {
        Instance instance = new Instance();
        instance.setId(UUID.randomUUID());
        instance.setSubdomain(subdomain);
        instance.setOrganizationName("Test Organization");
        instance.setOwnerId(UUID.randomUUID());
        instance.setContactEmail("test@example.com");
        instance.setTier(PricingTier.BASIC);
        instance.setStatus(status);
        instance.setTrialStartedAt(LocalDateTime.now().minusHours(24));
        return instance;
    }
}
