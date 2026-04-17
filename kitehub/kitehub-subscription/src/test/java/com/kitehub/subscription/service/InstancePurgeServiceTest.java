package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.domain.BackupRecord;
import com.kitehub.subscription.domain.BackupStatus;
import com.kitehub.subscription.dto.PurgeResult;
import com.kitehub.subscription.dto.PurgeStatus;
import com.kitehub.subscription.repository.BackupRecordRepository;
import com.kitehub.subscription.repository.EmailSentLogRepository;
import com.kitehub.subscription.repository.InstanceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InstancePurgeService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InstancePurgeService Unit Tests")
class InstancePurgeServiceTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private DatabaseProvisioningService databaseProvisioningService;

    @Mock
    private BackupStorageService backupStorageService;

    @Mock
    private BackupRecordRepository backupRecordRepository;

    @Mock
    private EmailSentLogRepository emailSentLogRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private InstancePurgeService instancePurgeService;

    private Instance deletedInstance;
    private UUID instanceId;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();
        deletedInstance = new Instance();
        deletedInstance.setId(instanceId);
        deletedInstance.setSubdomain("deleted-school");
        deletedInstance.setOrganizationName("Deleted School");
        deletedInstance.setOwnerId(UUID.randomUUID());
        deletedInstance.setTier(PricingTier.FREE);
        deletedInstance.setStatus(InstanceStatus.DELETED);
        deletedInstance.setDatabaseUrl("jdbc:postgresql://localhost:5432/kiteclass_test");
        deletedInstance.setDatabaseUsername("user");
        deletedInstance.setDatabasePassword("pass");
    }

    @Nested
    @DisplayName("purgeInstance - happy path")
    class PurgeInstanceHappyPath {

        @Test
        @DisplayName("should return SUCCESS when backup exists and all steps complete")
        void shouldReturnSuccessWhenBackupExists() {
            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(deletedInstance));
            when(backupRecordRepository.existsByInstanceIdAndStatus(instanceId, BackupStatus.COMPLETED))
                .thenReturn(true);
            when(backupRecordRepository.findByInstanceId(instanceId))
                .thenReturn(List.of(
                    BackupRecord.builder()
                        .id(UUID.randomUUID())
                        .instanceId(instanceId)
                        .s3Key("backups/test.dump")
                        .status(BackupStatus.COMPLETED)
                        .build()
                ));

            PurgeResult result = instancePurgeService.purgeInstance(instanceId);

            assertThat(result.getStatus()).isEqualTo(PurgeStatus.SUCCESS);
            assertThat(result.getInstanceId()).isEqualTo(instanceId);
            assertThat(result.getSubdomain()).isEqualTo("deleted-school");

            // Verify all cleanup steps called
            verify(databaseProvisioningService).deleteDatabase(instanceId);
            verify(backupStorageService).deleteBackup("backups/test.dump");
            verify(emailSentLogRepository).deleteByInstanceId(instanceId);
            verify(rabbitTemplate).convertAndSend(anyString(), eq(""), any(Object.class));
            verify(instanceRepository).save(deletedInstance);
            assertThat(deletedInstance.getStatus()).isEqualTo(InstanceStatus.PURGED);
        }
    }

    @Nested
    @DisplayName("purgeInstance - SKIPPED_NO_BACKUP (safety)")
    class PurgeInstanceNoBackup {

        @Test
        @DisplayName("should return SKIPPED_NO_BACKUP when no COMPLETED backup exists")
        void shouldReturnSkippedWhenNoBackup() {
            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(deletedInstance));
            when(backupRecordRepository.existsByInstanceIdAndStatus(instanceId, BackupStatus.COMPLETED))
                .thenReturn(false);

            PurgeResult result = instancePurgeService.purgeInstance(instanceId);

            assertThat(result.getStatus()).isEqualTo(PurgeStatus.SKIPPED_NO_BACKUP);
            assertThat(result.getErrorMessage()).contains("No COMPLETED backup found");

            // CRITICAL: Verify NO destructive operations were executed
            verify(databaseProvisioningService, never()).deleteDatabase(any());
            verify(backupStorageService, never()).deleteBackup(anyString());
            verify(emailSentLogRepository, never()).deleteByInstanceId(any());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
            verify(instanceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("purgeInstance - wrong status")
    class PurgeInstanceWrongStatus {

        @Test
        @DisplayName("should return FAILED when instance is not in DELETED status")
        void shouldFailWhenNotDeleted() {
            Instance activeInstance = new Instance();
            activeInstance.setId(instanceId);
            activeInstance.setSubdomain("active-school");
            activeInstance.setStatus(InstanceStatus.ACTIVE);

            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(activeInstance));

            PurgeResult result = instancePurgeService.purgeInstance(instanceId);

            assertThat(result.getStatus()).isEqualTo(PurgeStatus.FAILED);
            assertThat(result.getErrorMessage()).contains("not in DELETED status");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when instance not found")
        void shouldThrowWhenInstanceNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(instanceRepository.findById(unknownId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> instancePurgeService.purgeInstance(unknownId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Instance not found");
        }
    }

    @Nested
    @DisplayName("findPurgeEligible")
    class FindPurgeEligible {

        @Test
        @DisplayName("should delegate to repository with correct cutoff date")
        void shouldDelegateToRepository() {
            when(instanceRepository.findByStatusAndUpdatedAtBefore(eq(InstanceStatus.DELETED), any()))
                .thenReturn(List.of(deletedInstance));

            List<Instance> result = instancePurgeService.findPurgeEligible();

            assertThat(result).hasSize(1);
            verify(instanceRepository).findByStatusAndUpdatedAtBefore(eq(InstanceStatus.DELETED), any());
        }

        @Test
        @DisplayName("should return empty list when no eligible instances")
        void shouldReturnEmptyWhenNoneEligible() {
            when(instanceRepository.findByStatusAndUpdatedAtBefore(eq(InstanceStatus.DELETED), any()))
                .thenReturn(Collections.emptyList());

            List<Instance> result = instancePurgeService.findPurgeEligible();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("adminPurge")
    class AdminPurge {

        @Test
        @DisplayName("should work same as purgeInstance for DELETED instances with backup")
        void shouldWorkForDeletedInstances() {
            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(deletedInstance));
            when(backupRecordRepository.existsByInstanceIdAndStatus(instanceId, BackupStatus.COMPLETED))
                .thenReturn(true);
            when(backupRecordRepository.findByInstanceId(instanceId))
                .thenReturn(Collections.emptyList());

            PurgeResult result = instancePurgeService.adminPurge(instanceId);

            assertThat(result.getStatus()).isEqualTo(PurgeStatus.SUCCESS);
        }

        @Test
        @DisplayName("should fail for non-DELETED instance even from admin")
        void shouldFailForNonDeletedEvenAdmin() {
            Instance activeInstance = new Instance();
            activeInstance.setId(instanceId);
            activeInstance.setSubdomain("active-school");
            activeInstance.setStatus(InstanceStatus.ACTIVE);

            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(activeInstance));

            PurgeResult result = instancePurgeService.adminPurge(instanceId);

            assertThat(result.getStatus()).isEqualTo(PurgeStatus.FAILED);
            assertThat(result.getErrorMessage()).contains("DELETED status");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when instance not found via admin")
        void shouldThrowWhenInstanceNotFoundAdmin() {
            UUID unknownId = UUID.randomUUID();
            when(instanceRepository.findById(unknownId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> instancePurgeService.adminPurge(unknownId))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
