package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.domain.BackupRecord;
import com.kitehub.subscription.domain.BackupStatus;
import com.kitehub.subscription.dto.PurgeResult;
import com.kitehub.subscription.dto.PurgeStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.DatabaseBackupService;
import com.kitehub.subscription.service.InstancePurgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DatabaseBackupScheduler.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseBackupScheduler Unit Tests")
class DatabaseBackupSchedulerTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private DatabaseBackupService databaseBackupService;

    @Mock
    private InstancePurgeService instancePurgeService;

    private DatabaseBackupScheduler scheduler;

    private Instance activeInstance;

    @BeforeEach
    void setUp() {
        scheduler = new DatabaseBackupScheduler(instanceRepository, databaseBackupService, instancePurgeService);
        ReflectionTestUtils.setField(scheduler, "retentionCount", 7);

        activeInstance = new Instance();
        activeInstance.setId(UUID.randomUUID());
        activeInstance.setSubdomain("test-school");
        activeInstance.setOrganizationName("Test School");
        activeInstance.setOwnerId(UUID.randomUUID());
        activeInstance.setTier(PricingTier.BASIC);
        activeInstance.setStatus(InstanceStatus.ACTIVE);
        activeInstance.setDatabaseUrl("jdbc:postgresql://localhost:5432/kiteclass_abc12345");
        activeInstance.setDatabaseUsername("kiteclass_abc12345_user");
        activeInstance.setDatabasePassword("encrypted_password");
    }

    @Nested
    @DisplayName("backupAllDatabases")
    class BackupAllDatabases {

        @Test
        @DisplayName("should backup all active instances by delegating to DatabaseBackupService")
        void shouldBackupAllActiveInstances() {
            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.ACTIVE))
                .thenReturn(List.of(activeInstance));

            BackupRecord completedRecord = BackupRecord.builder()
                .instanceId(activeInstance.getId())
                .databaseName("kiteclass_abc12345")
                .s3Key("backups/test.dump")
                .status(BackupStatus.COMPLETED)
                .fileSizeBytes(1024L)
                .build();
            when(databaseBackupService.backupInstance(eq(activeInstance.getId()), eq("kiteclass_abc12345")))
                .thenReturn(completedRecord);

            scheduler.backupAllDatabases();

            verify(instanceRepository).findByStatusAndDeletedFalse(InstanceStatus.ACTIVE);
            verify(databaseBackupService).backupInstance(activeInstance.getId(), "kiteclass_abc12345");
            verify(databaseBackupService).cleanupOldBackups(activeInstance.getId(), 7);
        }

        @Test
        @DisplayName("should handle empty list of active instances")
        void shouldHandleEmptyActiveInstances() {
            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

            scheduler.backupAllDatabases();

            verify(instanceRepository).findByStatusAndDeletedFalse(InstanceStatus.ACTIVE);
            verify(databaseBackupService, never()).backupInstance(any(), anyString());
        }

        @Test
        @DisplayName("should continue backup when one instance fails")
        void shouldContinueOnFailure() {
            Instance badInstance = new Instance();
            badInstance.setId(UUID.randomUUID());
            badInstance.setSubdomain("bad-school");
            badInstance.setOrganizationName("Bad School");
            badInstance.setOwnerId(UUID.randomUUID());
            badInstance.setTier(PricingTier.FREE);
            badInstance.setStatus(InstanceStatus.ACTIVE);
            badInstance.setDatabaseUrl(null); // Will cause exception in extractDatabaseName
            badInstance.setDatabaseUsername("user");
            badInstance.setDatabasePassword("pass");

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.ACTIVE))
                .thenReturn(Arrays.asList(badInstance, activeInstance));

            BackupRecord completedRecord = BackupRecord.builder()
                .instanceId(activeInstance.getId())
                .databaseName("kiteclass_abc12345")
                .s3Key("backups/test.dump")
                .status(BackupStatus.COMPLETED)
                .fileSizeBytes(1024L)
                .build();
            when(databaseBackupService.backupInstance(eq(activeInstance.getId()), eq("kiteclass_abc12345")))
                .thenReturn(completedRecord);

            // Should not throw — continues past the failure
            scheduler.backupAllDatabases();

            verify(instanceRepository).findByStatusAndDeletedFalse(InstanceStatus.ACTIVE);
            // The good instance should still be backed up despite the first one failing
            verify(databaseBackupService).backupInstance(activeInstance.getId(), "kiteclass_abc12345");
        }

        @Test
        @DisplayName("should not cleanup when backup fails")
        void shouldNotCleanupWhenBackupFails() {
            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.ACTIVE))
                .thenReturn(List.of(activeInstance));

            BackupRecord failedRecord = BackupRecord.builder()
                .instanceId(activeInstance.getId())
                .databaseName("kiteclass_abc12345")
                .s3Key("backups/test.dump")
                .status(BackupStatus.FAILED)
                .build();
            when(databaseBackupService.backupInstance(eq(activeInstance.getId()), eq("kiteclass_abc12345")))
                .thenReturn(failedRecord);

            scheduler.backupAllDatabases();

            verify(databaseBackupService, never()).cleanupOldBackups(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("cleanupDeletedInstances")
    class CleanupDeletedInstances {

        @Test
        @DisplayName("should delegate to InstancePurgeService for eligible instances")
        void shouldDelegateToPurgeService() {
            Instance deletedInstance = new Instance();
            deletedInstance.setId(UUID.randomUUID());
            deletedInstance.setSubdomain("deleted-school");
            deletedInstance.setStatus(InstanceStatus.DELETED);

            when(instancePurgeService.findPurgeEligible())
                .thenReturn(List.of(deletedInstance));
            when(instancePurgeService.purgeInstance(deletedInstance.getId()))
                .thenReturn(PurgeResult.builder()
                    .instanceId(deletedInstance.getId())
                    .status(PurgeStatus.SUCCESS)
                    .build());

            scheduler.cleanupDeletedInstances();

            verify(instancePurgeService).findPurgeEligible();
            verify(instancePurgeService).purgeInstance(deletedInstance.getId());
        }

        @Test
        @DisplayName("should handle no deleted instances to clean up")
        void shouldHandleNoDeletedInstances() {
            when(instancePurgeService.findPurgeEligible())
                .thenReturn(Collections.emptyList());

            scheduler.cleanupDeletedInstances();

            verify(instancePurgeService).findPurgeEligible();
            verify(instancePurgeService, never()).purgeInstance(any());
        }

        @Test
        @DisplayName("should count skipped instances without backup")
        void shouldCountSkippedInstances() {
            Instance deletedInstance = new Instance();
            deletedInstance.setId(UUID.randomUUID());
            deletedInstance.setSubdomain("no-backup");
            deletedInstance.setStatus(InstanceStatus.DELETED);

            when(instancePurgeService.findPurgeEligible())
                .thenReturn(List.of(deletedInstance));
            when(instancePurgeService.purgeInstance(deletedInstance.getId()))
                .thenReturn(PurgeResult.builder()
                    .instanceId(deletedInstance.getId())
                    .status(PurgeStatus.SKIPPED_NO_BACKUP)
                    .build());

            // Should not throw
            scheduler.cleanupDeletedInstances();

            verify(instancePurgeService).purgeInstance(deletedInstance.getId());
        }
    }

    @Nested
    @DisplayName("extractDatabaseName")
    class ExtractDatabaseName {

        @Test
        @DisplayName("should extract database name from JDBC URL")
        void shouldExtractDbName() {
            String result = scheduler.extractDatabaseName("jdbc:postgresql://localhost:5432/kiteclass_abc12345");
            assertThat(result).isEqualTo("kiteclass_abc12345");
        }

        @Test
        @DisplayName("should extract database name from URL with query params")
        void shouldExtractDbNameWithQueryParams() {
            String result = scheduler.extractDatabaseName("jdbc:postgresql://localhost:5432/kiteclass_abc12345?sslmode=disable");
            assertThat(result).isEqualTo("kiteclass_abc12345");
        }

        @Test
        @DisplayName("should throw for null database URL")
        void shouldThrowForNullUrl() {
            assertThatThrownBy(() -> scheduler.extractDatabaseName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
        }

        @Test
        @DisplayName("should throw for empty database URL")
        void shouldThrowForEmptyUrl() {
            assertThatThrownBy(() -> scheduler.extractDatabaseName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
        }
    }
}
