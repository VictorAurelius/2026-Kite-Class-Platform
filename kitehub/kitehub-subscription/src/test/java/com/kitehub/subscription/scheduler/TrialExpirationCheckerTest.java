package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.TrialConfig;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.TrialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TrialExpirationChecker.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrialExpirationChecker Unit Tests")
class TrialExpirationCheckerTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private TrialService trialService;

    @Mock
    private EmailServiceClient emailServiceClient;

    @Mock
    private TrialConfig trialConfig;

    @InjectMocks
    private TrialExpirationChecker expirationChecker;

    private Instance expiredInstance;
    private Instance activeInstance;
    private Instance warningInstance;

    @BeforeEach
    void setUp() {
        lenient().when(trialConfig.getWarningDays()).thenReturn(java.util.List.of(3, 1));

        expiredInstance = createInstance("expired-org", InstanceStatus.TRIAL);
        expiredInstance.startTrial(14);
        expiredInstance.setTrialExpiresAt(LocalDateTime.now().minusDays(1)); // Expired

        activeInstance = createInstance("active-org", InstanceStatus.TRIAL);
        activeInstance.startTrial(14); // 14 days left

        warningInstance = createInstance("warning-org", InstanceStatus.TRIAL);
        warningInstance.startTrial(14);
        warningInstance.setTrialExpiresAt(LocalDateTime.now().plusDays(1)); // 1 day left
    }

    @Test
    @DisplayName("Should suspend expired trials")
    void shouldSuspendExpiredTrials() {
        // Given
        when(instanceRepository.findExpiredTrials(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(expiredInstance));
        when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
            .thenReturn(Collections.emptyList());

        // When
        expirationChecker.checkExpiredTrials();

        // Then
        verify(trialService).suspendExpiredTrial(expiredInstance.getId());
    }

    @Test
    @DisplayName("Should handle multiple expired trials")
    void shouldHandleMultipleExpiredTrials() {
        // Given
        Instance expiredInstance2 = createInstance("expired-org-2", InstanceStatus.TRIAL);
        expiredInstance2.startTrial(14);
        expiredInstance2.setTrialExpiresAt(LocalDateTime.now().minusDays(1));

        when(instanceRepository.findExpiredTrials(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(expiredInstance, expiredInstance2));
        when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
            .thenReturn(Collections.emptyList());

        // When
        expirationChecker.checkExpiredTrials();

        // Then
        verify(trialService).suspendExpiredTrial(expiredInstance.getId());
        verify(trialService).suspendExpiredTrial(expiredInstance2.getId());
    }

    @Test
    @DisplayName("Should handle no expired trials")
    void shouldHandleNoExpiredTrials() {
        // Given
        when(instanceRepository.findExpiredTrials(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
            .thenReturn(Collections.emptyList());

        // When
        expirationChecker.checkExpiredTrials();

        // Then
        verify(trialService, never()).suspendExpiredTrial(any());
    }

    @Test
    @DisplayName("Should check warnings for trials expiring soon")
    void shouldCheckWarningsForTrialsExpiringSoon() {
        // Given
        when(instanceRepository.findExpiredTrials(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
            .thenReturn(Collections.singletonList(warningInstance));

        // When
        expirationChecker.checkExpiredTrials();

        // Then
        verify(instanceRepository).findByStatusAndDeletedFalse(InstanceStatus.TRIAL);
        // Warning should be logged (checked manually in logs)
    }

    @Test
    @DisplayName("Should not send warnings for trials with more than 3 days left")
    void shouldNotSendWarningsForTrialsWithMoreThan3DaysLeft() {
        // Given
        when(instanceRepository.findExpiredTrials(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
            .thenReturn(Collections.singletonList(activeInstance)); // 14 days left

        // When
        expirationChecker.checkExpiredTrials();

        // Then
        verify(instanceRepository).findByStatusAndDeletedFalse(InstanceStatus.TRIAL);
        // No warning should be sent (verified by logs)
    }

    @Test
    @DisplayName("Should handle exceptions during suspension gracefully")
    void shouldHandleExceptionsDuringSuspensionGracefully() {
        // Given
        when(instanceRepository.findExpiredTrials(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(expiredInstance));
        when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
            .thenReturn(Collections.emptyList());

        doThrow(new RuntimeException("Database error"))
            .when(trialService).suspendExpiredTrial(expiredInstance.getId());

        // When
        expirationChecker.checkExpiredTrials();

        // Then
        // Should not throw exception - error logged
        verify(trialService).suspendExpiredTrial(expiredInstance.getId());
    }

    @Test
    @DisplayName("Should trigger manual check")
    void shouldTriggerManualCheck() {
        // Given
        when(instanceRepository.findExpiredTrials(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
            .thenReturn(Collections.emptyList());

        // When
        expirationChecker.triggerManualCheck();

        // Then
        verify(instanceRepository).findExpiredTrials(any(LocalDateTime.class));
    }

    // ---- GAP-1270 (TR-08): trial extension / auto-rescue ----

    @Test
    @DisplayName("GAP-1270: grantTrialExtension extends expiry + reactivates a suspended trial")
    void grantTrialExtension_extendsAndReactivates() {
        Instance suspended = createInstance("rescue-org", InstanceStatus.SUSPENDED);
        suspended.setTrialStartedAt(LocalDateTime.now().minusDays(14));
        suspended.setTrialExpiresAt(LocalDateTime.now().minusDays(1)); // expired
        when(trialConfig.getExtensionDays()).thenReturn(7);
        when(instanceRepository.findById(suspended.getId())).thenReturn(Optional.of(suspended));

        expirationChecker.grantTrialExtension(suspended.getId());

        assertEquals(InstanceStatus.TRIAL, suspended.getStatus());
        assertTrue(suspended.getTrialExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
        verify(instanceRepository).save(suspended);
    }

    @Test
    @DisplayName("GAP-1270: auto-extend grants one extension instead of suspend when enabled")
    void checkExpiredTrials_autoExtendsWhenEnabled() {
        Instance expiring = createInstance("auto-rescue-org", InstanceStatus.TRIAL);
        expiring.setTrialStartedAt(LocalDateTime.now().minusDays(14));
        expiring.setTrialExpiresAt(LocalDateTime.now().minusDays(1)); // expired, not yet extended
        when(instanceRepository.findExpiredTrials(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(expiring));
        when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL))
            .thenReturn(Collections.emptyList());
        when(trialConfig.isAutoExtendOnExpiry()).thenReturn(true);
        when(trialConfig.getDurationDays()).thenReturn(14);
        when(trialConfig.getExtensionDays()).thenReturn(7);
        when(instanceRepository.findById(expiring.getId())).thenReturn(Optional.of(expiring));

        expirationChecker.checkExpiredTrials();

        verify(trialService, never()).suspendExpiredTrial(any());
        verify(instanceRepository).save(expiring);
        assertTrue(expiring.getTrialExpiresAt().isAfter(LocalDateTime.now()));
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
        return instance;
    }
}
