package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.DataRetentionConfig;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DataRetentionService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataRetentionService Unit Tests")
class DataRetentionServiceTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private DataRetentionConfig retentionConfig;

    @Mock
    private EmailServiceClient emailServiceClient;

    @InjectMocks
    private DataRetentionService dataRetentionService;

    private Instance suspendedInstance;
    private UUID instanceId;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();

        suspendedInstance = new Instance();
        suspendedInstance.setId(instanceId);
        suspendedInstance.setSubdomain("test-org");
        suspendedInstance.setOrganizationName("Test Organization");
        suspendedInstance.setOwnerId(UUID.randomUUID());
        suspendedInstance.setTier(PricingTier.BASIC);
        suspendedInstance.setStatus(InstanceStatus.SUSPENDED);
        suspendedInstance.setContactEmail("admin@test.org");
        suspendedInstance.setDatabaseUrl("jdbc:postgresql://localhost/test");
        suspendedInstance.setDatabaseUsername("test");
        suspendedInstance.setDatabasePassword("encrypted");
    }

    @Nested
    @DisplayName("getRetentionDays")
    class GetRetentionDays {

        @Test
        @DisplayName("Should return tier-specific retention days")
        void shouldReturnTierSpecificRetentionDays() {
            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(suspendedInstance));
            when(retentionConfig.getRetentionDays("BASIC")).thenReturn(30);

            int days = dataRetentionService.getRetentionDays(instanceId);

            assertThat(days).isEqualTo(30);
        }

        @Test
        @DisplayName("Should return FREE tier days when instance not found")
        void shouldReturnFreeWhenInstanceNotFound() {
            when(instanceRepository.findById(instanceId)).thenReturn(Optional.empty());
            when(retentionConfig.getFree()).thenReturn(7);

            int days = dataRetentionService.getRetentionDays(instanceId);

            assertThat(days).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("processRetentionWarnings")
    class ProcessRetentionWarnings {

        @Test
        @DisplayName("Should send warning at 50% of retention period")
        void shouldSendWarningAtHalfRetention() {
            // Suspended 15 days ago, retention is 30 days → 50% → should warn
            suspendedInstance.setSuspendedAt(LocalDateTime.now().minusDays(15));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED))
                .thenReturn(List.of(suspendedInstance));
            when(retentionConfig.getRetentionDays("BASIC")).thenReturn(30);

            int warnings = dataRetentionService.processRetentionWarnings();

            assertThat(warnings).isEqualTo(1);
            verify(emailServiceClient).sendRetentionWarning(
                eq(instanceId), eq("admin@test.org"),
                eq("Test Organization"), eq(15L));
        }

        @Test
        @DisplayName("Should send warning at 80% of retention period")
        void shouldSendWarningAt80PercentRetention() {
            // Suspended 24 days ago, retention is 30 days → 80% → should warn
            suspendedInstance.setSuspendedAt(LocalDateTime.now().minusDays(24));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED))
                .thenReturn(List.of(suspendedInstance));
            when(retentionConfig.getRetentionDays("BASIC")).thenReturn(30);

            int warnings = dataRetentionService.processRetentionWarnings();

            assertThat(warnings).isEqualTo(1);
            verify(emailServiceClient).sendRetentionWarning(
                eq(instanceId), eq("admin@test.org"),
                eq("Test Organization"), eq(6L));
        }

        @Test
        @DisplayName("Should not send warning at non-trigger days")
        void shouldNotSendWarningAtNonTriggerDays() {
            // Suspended 10 days ago, retention is 30 days → not at 50% or 80%
            suspendedInstance.setSuspendedAt(LocalDateTime.now().minusDays(10));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED))
                .thenReturn(List.of(suspendedInstance));
            when(retentionConfig.getRetentionDays("BASIC")).thenReturn(30);

            int warnings = dataRetentionService.processRetentionWarnings();

            assertThat(warnings).isEqualTo(0);
            verify(emailServiceClient, never()).sendRetentionWarning(
                any(), anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("Should handle no suspended instances")
        void shouldHandleNoSuspendedInstances() {
            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED))
                .thenReturn(Collections.emptyList());

            int warnings = dataRetentionService.processRetentionWarnings();

            assertThat(warnings).isEqualTo(0);
        }

        @Test
        @DisplayName("Should skip instances without contact email")
        void shouldSkipInstancesWithoutContactEmail() {
            suspendedInstance.setContactEmail(null);
            suspendedInstance.setSuspendedAt(LocalDateTime.now().minusDays(15));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED))
                .thenReturn(List.of(suspendedInstance));
            when(retentionConfig.getRetentionDays("BASIC")).thenReturn(30);

            int warnings = dataRetentionService.processRetentionWarnings();

            assertThat(warnings).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("processExpiredRetention")
    class ProcessExpiredRetention {

        @Test
        @DisplayName("Should delete instance when retention expired")
        void shouldDeleteInstanceWhenRetentionExpired() {
            // Suspended 31 days ago, retention is 30 days → expired
            suspendedInstance.setSuspendedAt(LocalDateTime.now().minusDays(31));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED))
                .thenReturn(List.of(suspendedInstance));
            when(retentionConfig.getRetentionDays("BASIC")).thenReturn(30);
            when(instanceRepository.save(any(Instance.class))).thenReturn(suspendedInstance);

            int deleted = dataRetentionService.processExpiredRetention();

            assertThat(deleted).isEqualTo(1);
            assertThat(suspendedInstance.getStatus()).isEqualTo(InstanceStatus.DELETED);
            assertThat(suspendedInstance.isDeleted()).isTrue();
            verify(instanceRepository).save(suspendedInstance);
            verify(emailServiceClient).sendDataDeletedNotification(
                eq(instanceId), eq("admin@test.org"), eq("Test Organization"));
        }

        @Test
        @DisplayName("Should not delete instance within retention period")
        void shouldNotDeleteInstanceWithinRetentionPeriod() {
            // Suspended 20 days ago, retention is 30 days → still within retention
            suspendedInstance.setSuspendedAt(LocalDateTime.now().minusDays(20));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED))
                .thenReturn(List.of(suspendedInstance));
            when(retentionConfig.getRetentionDays("BASIC")).thenReturn(30);

            int deleted = dataRetentionService.processExpiredRetention();

            assertThat(deleted).isEqualTo(0);
            assertThat(suspendedInstance.getStatus()).isEqualTo(InstanceStatus.SUSPENDED);
            verify(instanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip instance with no retention-clock anchor (suspendedAt + updatedAt both null)")
        void shouldHandleInstanceWithoutClockAnchor() {
            suspendedInstance.setSuspendedAt(null);
            suspendedInstance.setUpdatedAt(null);

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED))
                .thenReturn(List.of(suspendedInstance));
            when(retentionConfig.getRetentionDays("BASIC")).thenReturn(30);

            int deleted = dataRetentionService.processExpiredRetention();

            assertThat(deleted).isEqualTo(0);
        }

        @Test
        @DisplayName("GAP-1264: legacy row (suspendedAt null) falls back to updatedAt for the clock")
        void shouldFallBackToUpdatedAtForLegacyRow() {
            // Legacy SUSPENDED row from before V73 — no suspended_at stamp.
            suspendedInstance.setSuspendedAt(null);
            suspendedInstance.setUpdatedAt(LocalDateTime.now().minusDays(31));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED))
                .thenReturn(List.of(suspendedInstance));
            when(retentionConfig.getRetentionDays("BASIC")).thenReturn(30);
            when(instanceRepository.save(any(Instance.class))).thenReturn(suspendedInstance);

            int deleted = dataRetentionService.processExpiredRetention();

            assertThat(deleted).isEqualTo(1);
            assertThat(suspendedInstance.getStatus()).isEqualTo(InstanceStatus.DELETED);
        }

        @Test
        @DisplayName("GAP-1264: suspendedAt takes precedence over updatedAt (clock not reset by later update)")
        void shouldPreferSuspendedAtOverUpdatedAt() {
            // suspended 31 days ago, but a later unrelated row update bumped updatedAt to now.
            // Clock MUST follow suspendedAt → still expired → deleted.
            suspendedInstance.setSuspendedAt(LocalDateTime.now().minusDays(31));
            suspendedInstance.setUpdatedAt(LocalDateTime.now());

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED))
                .thenReturn(List.of(suspendedInstance));
            when(retentionConfig.getRetentionDays("BASIC")).thenReturn(30);
            when(instanceRepository.save(any(Instance.class))).thenReturn(suspendedInstance);

            int deleted = dataRetentionService.processExpiredRetention();

            assertThat(deleted).isEqualTo(1);
            assertThat(suspendedInstance.getStatus()).isEqualTo(InstanceStatus.DELETED);
        }

        @Test
        @DisplayName("GAP-1026: final warning fires range-based within lead window (not exact ==1)")
        void shouldSendFinalWarningWithinLeadWindow() {
            // finalWarningLeadDays default 1; suspended 29 days ago, retention 30 → 1 day left.
            suspendedInstance.setSuspendedAt(LocalDateTime.now().minusDays(29));

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED))
                .thenReturn(List.of(suspendedInstance));
            when(retentionConfig.getRetentionDays("BASIC")).thenReturn(30);
            when(retentionConfig.getFinalWarningLeadDays()).thenReturn(1);

            int deleted = dataRetentionService.processExpiredRetention();

            assertThat(deleted).isEqualTo(0);
            verify(emailServiceClient).sendDataRetentionFinalWarning(
                eq(instanceId), eq("admin@test.org"), eq("test-org"));
        }
    }

    @Nested
    @DisplayName("shouldSendWarning")
    class ShouldSendWarning {

        @Test
        @DisplayName("Should return true at 50% of retention")
        void shouldReturnTrueAtHalf() {
            assertThat(dataRetentionService.shouldSendWarning(15, 30)).isTrue();
        }

        @Test
        @DisplayName("Should return true at 80% of retention")
        void shouldReturnTrueAt80Percent() {
            // 30 * 0.8 = 24
            assertThat(dataRetentionService.shouldSendWarning(24, 30)).isTrue();
        }

        @Test
        @DisplayName("Should return false at other days")
        void shouldReturnFalseAtOtherDays() {
            assertThat(dataRetentionService.shouldSendWarning(10, 30)).isFalse();
            assertThat(dataRetentionService.shouldSendWarning(20, 30)).isFalse();
        }

        @Test
        @DisplayName("Should return false for zero retention days")
        void shouldReturnFalseForZeroRetention() {
            assertThat(dataRetentionService.shouldSendWarning(0, 0)).isFalse();
        }

        @Test
        @DisplayName("Should handle small retention periods")
        void shouldHandleSmallRetentionPeriods() {
            // 7 days: 50% = 3, 80% = 5
            assertThat(dataRetentionService.shouldSendWarning(3, 7)).isTrue();
            assertThat(dataRetentionService.shouldSendWarning(5, 7)).isTrue();
            assertThat(dataRetentionService.shouldSendWarning(4, 7)).isFalse();
        }
    }
}
