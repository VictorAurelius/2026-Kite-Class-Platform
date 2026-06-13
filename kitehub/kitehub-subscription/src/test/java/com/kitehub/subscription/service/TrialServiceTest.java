package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.TrialConfig;
import com.kitehub.subscription.dto.TrialStatusResponse;
import com.kitehub.subscription.repository.InstanceRepository;
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
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for TrialService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrialService Unit Tests")
@SuppressWarnings("deprecation")  // tests legacy TrialService.convertTrialToSubscription intentionally — Phase 4c migrates callers
class TrialServiceTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private TrialConfig trialConfig;

    @Mock
    private InstanceTierSyncService instanceTierSyncService;

    @InjectMocks
    private TrialService trialService;

    private UUID instanceId;
    private Instance instance;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();

        instance = new Instance();
        instance.setId(instanceId);
        instance.setSubdomain("test-org");
        instance.setOrganizationName("Test Organization");
        instance.setOwnerId(UUID.randomUUID());
        instance.setTier(PricingTier.BASIC);
        instance.setStatus(InstanceStatus.TRIAL);

        lenient().when(trialConfig.getDurationDays()).thenReturn(14);
    }

    @Test
    @DisplayName("Should start trial successfully")
    void shouldStartTrialSuccessfully() {
        // Given
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(instance);

        // When
        trialService.startTrial(instanceId);

        // Then
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(instanceRepository).save(captor.capture());

        Instance saved = captor.getValue();
        assertThat(saved.getTrialStartedAt()).isNotNull();
        assertThat(saved.getTrialExpiresAt()).isNotNull();
        assertThat(saved.getTrialExpiresAt()).isAfter(saved.getTrialStartedAt());
    }

    @Test
    @DisplayName("Should not restart trial if already started")
    void shouldNotRestartTrialIfAlreadyStarted() {
        // Given
        instance.startTrial(14);
        LocalDateTime originalStart = instance.getTrialStartedAt();

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        trialService.startTrial(instanceId);

        // Then
        verify(instanceRepository, never()).save(any());
        assertThat(instance.getTrialStartedAt()).isEqualTo(originalStart);
    }

    @Test
    @DisplayName("Should throw exception when instance not found")
    void shouldThrowExceptionWhenInstanceNotFound() {
        // Given
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> trialService.startTrial(instanceId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Instance not found");
    }

    @Test
    @DisplayName("Should get trial status correctly")
    void shouldGetTrialStatusCorrectly() {
        // Given
        instance.startTrial(14);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        TrialStatusResponse response = trialService.getTrialStatus(instanceId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getInstanceId()).isEqualTo(instanceId);
        assertThat(response.getSubdomain()).isEqualTo("test-org");
        assertThat(response.getStatus()).isEqualTo(InstanceStatus.TRIAL);
        assertThat(response.getIsOnTrial()).isTrue();
        assertThat(response.getTrialStartedAt()).isNotNull();
        assertThat(response.getTrialExpiresAt()).isNotNull();
        assertThat(response.getDaysLeft()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should extend trial successfully")
    void shouldExtendTrialSuccessfully() {
        // Given
        instance.startTrial(14);
        LocalDateTime originalExpiry = instance.getTrialExpiresAt();

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(instance);

        // When
        trialService.extendTrial(instanceId, 7);

        // Then
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(instanceRepository).save(captor.capture());

        Instance saved = captor.getValue();
        assertThat(saved.getTrialExpiresAt()).isAfter(originalExpiry);
    }

    @Test
    @DisplayName("Should throw exception when extending non-trial instance")
    void shouldThrowExceptionWhenExtendingNonTrialInstance() {
        // Given
        instance.setStatus(InstanceStatus.ACTIVE);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When & Then
        assertThatThrownBy(() -> trialService.extendTrial(instanceId, 7))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not on trial");
    }

    @Test
    @DisplayName("Should throw exception when extension days invalid")
    void shouldThrowExceptionWhenExtensionDaysInvalid() {
        // Given - no setup needed, validation happens before repository call

        // When & Then
        assertThatThrownBy(() -> trialService.extendTrial(instanceId, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid extension days");

        assertThatThrownBy(() -> trialService.extendTrial(instanceId, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid extension days");
    }

    @Test
    @DisplayName("Should detect expired trial")
    void shouldDetectExpiredTrial() {
        // Given
        instance.startTrial(14);
        instance.setTrialExpiresAt(LocalDateTime.now().minusDays(1)); // Expired yesterday

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        boolean isExpired = trialService.isTrialExpired(instanceId);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("Should detect active trial")
    void shouldDetectActiveTrial() {
        // Given
        instance.startTrial(14);

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        boolean isExpired = trialService.isTrialExpired(instanceId);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should convert trial to subscription")
    void shouldConvertTrialToSubscription() {
        // Given
        instance.startTrial(14);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(instance);

        // When
        trialService.convertTrialToSubscription(instanceId, PricingTier.PREMIUM);

        // Then
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(instanceRepository).save(captor.capture());

        Instance saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(InstanceStatus.ACTIVE);
        // GAP-1095 — effective tier set through the canonical SUB-21 sync point.
        verify(instanceTierSyncService).syncInstanceTier(saved, PricingTier.PREMIUM);
    }

    @Test
    @DisplayName("Should suspend expired trial")
    void shouldSuspendExpiredTrial() {
        // Given
        instance.startTrial(14);
        instance.setTrialExpiresAt(LocalDateTime.now().minusDays(1)); // Expired

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(instance);

        // When
        trialService.suspendExpiredTrial(instanceId);

        // Then
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(instanceRepository).save(captor.capture());

        Instance saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(InstanceStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Should get warning level HIGH for 1 day left")
    void shouldGetWarningLevelHighFor1DayLeft() {
        // Given
        instance.startTrial(14);
        instance.setTrialExpiresAt(LocalDateTime.now().plusDays(1)); // 1 day left

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        TrialStatusResponse response = trialService.getTrialStatus(instanceId);

        // Then
        assertThat(response.getWarningLevel()).isEqualTo("HIGH");
        assertThat(response.getNeedsWarning()).isTrue();
    }

    @Test
    @DisplayName("Should get warning level MEDIUM for 2-3 days left")
    void shouldGetWarningLevelMediumFor2To3DaysLeft() {
        // Given
        instance.startTrial(14);
        instance.setTrialExpiresAt(LocalDateTime.now().plusDays(2)); // 2 days left

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        TrialStatusResponse response = trialService.getTrialStatus(instanceId);

        // Then
        assertThat(response.getWarningLevel()).isEqualTo("MEDIUM");
        assertThat(response.getNeedsWarning()).isTrue();
    }

    @Test
    @DisplayName("Should get warning level NONE for more than 3 days left")
    void shouldGetWarningLevelNoneForMoreThan3DaysLeft() {
        // Given
        instance.startTrial(14);
        // Trial just started (14 days left)

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        TrialStatusResponse response = trialService.getTrialStatus(instanceId);

        // Then
        assertThat(response.getWarningLevel()).isEqualTo("NONE");
        assertThat(response.getNeedsWarning()).isFalse();
    }

    @Test
    @DisplayName("Should get warning level EXPIRED for expired trial")
    void shouldGetWarningLevelExpiredForExpiredTrial() {
        // Given
        instance.startTrial(14);
        instance.setTrialExpiresAt(LocalDateTime.now().minusDays(1)); // Expired

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        TrialStatusResponse response = trialService.getTrialStatus(instanceId);

        // Then
        assertThat(response.getWarningLevel()).isEqualTo("EXPIRED");
        assertThat(response.getIsOnTrial()).isFalse();
    }
}
