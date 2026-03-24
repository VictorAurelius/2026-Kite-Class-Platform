package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
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

    @InjectMocks
    private DatabaseBackupScheduler scheduler;

    private Instance activeInstance;
    private Instance deletedInstance;

    @BeforeEach
    void setUp() {
        activeInstance = new Instance();
        activeInstance.setSubdomain("test-school");
        activeInstance.setOrganizationName("Test School");
        activeInstance.setOwnerId(UUID.randomUUID());
        activeInstance.setTier(PricingTier.BASIC);
        activeInstance.setStatus(InstanceStatus.ACTIVE);
        activeInstance.setDatabaseUrl("jdbc:postgresql://localhost:5432/kiteclass_abc12345");
        activeInstance.setDatabaseUsername("kiteclass_abc12345_user");
        activeInstance.setDatabasePassword("encrypted_password");

        deletedInstance = new Instance();
        deletedInstance.setSubdomain("deleted-school");
        deletedInstance.setOrganizationName("Deleted School");
        deletedInstance.setOwnerId(UUID.randomUUID());
        deletedInstance.setTier(PricingTier.FREE);
        deletedInstance.setStatus(InstanceStatus.DELETED);
        deletedInstance.setDatabaseUrl("jdbc:postgresql://localhost:5432/kiteclass_def67890");
        deletedInstance.setDatabaseUsername("kiteclass_def67890_user");
        deletedInstance.setDatabasePassword("encrypted_password");
    }

    @Nested
    @DisplayName("backupAllDatabases")
    class BackupAllDatabases {

        @Test
        @DisplayName("should backup all active instances")
        void shouldBackupAllActiveInstances() {
            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.ACTIVE))
                .thenReturn(Arrays.asList(activeInstance));

            scheduler.backupAllDatabases();

            verify(instanceRepository).findByStatusAndDeletedFalse(InstanceStatus.ACTIVE);
        }

        @Test
        @DisplayName("should handle empty list of active instances")
        void shouldHandleEmptyActiveInstances() {
            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

            scheduler.backupAllDatabases();

            verify(instanceRepository).findByStatusAndDeletedFalse(InstanceStatus.ACTIVE);
        }

        @Test
        @DisplayName("should continue backup when one instance fails")
        void shouldContinueOnFailure() {
            Instance badInstance = new Instance();
            badInstance.setSubdomain("bad-school");
            badInstance.setOrganizationName("Bad School");
            badInstance.setOwnerId(UUID.randomUUID());
            badInstance.setTier(PricingTier.FREE);
            badInstance.setStatus(InstanceStatus.ACTIVE);
            badInstance.setDatabaseUrl(null); // Will cause NPE in extractDatabaseName
            badInstance.setDatabaseUsername("user");
            badInstance.setDatabasePassword("pass");

            when(instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.ACTIVE))
                .thenReturn(Arrays.asList(badInstance, activeInstance));

            // Should not throw — continues past the failure
            scheduler.backupAllDatabases();

            verify(instanceRepository).findByStatusAndDeletedFalse(InstanceStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("cleanupDeletedInstances")
    class CleanupDeletedInstances {

        @Test
        @DisplayName("should clean up instances deleted more than 30 days ago")
        void shouldCleanupOldDeletedInstances() {
            when(instanceRepository.findByStatusAndDeletedFalseAndUpdatedAtBefore(
                eq(InstanceStatus.DELETED), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(deletedInstance));

            scheduler.cleanupDeletedInstances();

            verify(instanceRepository).findByStatusAndDeletedFalseAndUpdatedAtBefore(
                eq(InstanceStatus.DELETED), any(LocalDateTime.class));
            verify(instanceRepository).save(deletedInstance);
        }

        @Test
        @DisplayName("should handle no deleted instances to clean up")
        void shouldHandleNoDeletedInstances() {
            when(instanceRepository.findByStatusAndDeletedFalseAndUpdatedAtBefore(
                eq(InstanceStatus.DELETED), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

            scheduler.cleanupDeletedInstances();

            verify(instanceRepository).findByStatusAndDeletedFalseAndUpdatedAtBefore(
                eq(InstanceStatus.DELETED), any(LocalDateTime.class));
            verify(instanceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("backupInstanceDatabase")
    class BackupInstanceDatabase {

        @Test
        @DisplayName("should log backup for instance without throwing")
        void shouldLogBackupForInstance() {
            // Should complete without error — backup is currently log-only
            scheduler.backupInstanceDatabase(activeInstance);
            // No exception = success
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
    }

    @Nested
    @DisplayName("generateBackupPath")
    class GenerateBackupPath {

        @Test
        @DisplayName("should generate S3 path with instance ID and date")
        void shouldGenerateS3Path() {
            UUID instanceId = UUID.randomUUID();
            String path = scheduler.generateBackupPath(instanceId, "kiteclass_abc12345");

            assertThat(path).startsWith("s3://kiteclass-backups/" + instanceId + "/");
            assertThat(path).endsWith("kiteclass_abc12345.sql.gz");
        }
    }
}
