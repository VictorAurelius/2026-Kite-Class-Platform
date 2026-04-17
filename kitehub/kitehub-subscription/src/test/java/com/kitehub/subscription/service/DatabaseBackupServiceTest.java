package com.kitehub.subscription.service;

import com.kitehub.subscription.domain.BackupRecord;
import com.kitehub.subscription.domain.BackupStatus;
import com.kitehub.subscription.repository.BackupRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DatabaseBackupService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseBackupService Unit Tests")
class DatabaseBackupServiceTest {

    @Mock
    private BackupRecordRepository backupRecordRepository;

    @Mock
    private BackupStorageService backupStorageService;

    private DatabaseBackupService databaseBackupService;

    private UUID instanceId;

    @BeforeEach
    void setUp() {
        databaseBackupService = new DatabaseBackupService(backupRecordRepository, backupStorageService);
        ReflectionTestUtils.setField(databaseBackupService, "dbHost", "localhost");
        ReflectionTestUtils.setField(databaseBackupService, "dbPort", 5433);
        ReflectionTestUtils.setField(databaseBackupService, "dbUser", "postgres");
        ReflectionTestUtils.setField(databaseBackupService, "dbPassword", "password");
        ReflectionTestUtils.setField(databaseBackupService, "retentionCount", 7);
        ReflectionTestUtils.setField(databaseBackupService, "pgDumpPath", "pg_dump");
        instanceId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("backupInstance")
    class BackupInstance {

        @Test
        @DisplayName("should reject invalid database name (SQL injection prevention)")
        void shouldRejectInvalidDatabaseName() {
            assertThatThrownBy(() -> databaseBackupService.backupInstance(instanceId, "db; DROP TABLE users"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid database name");
        }

        @Test
        @DisplayName("should reject null database name")
        void shouldRejectNullDatabaseName() {
            assertThatThrownBy(() -> databaseBackupService.backupInstance(instanceId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should accept valid database name")
        void shouldAcceptValidDatabaseName() {
            // Valid name like kiteclass_abc12345
            // Will fail at pg_dump (not available in test) but passes validation
            when(backupRecordRepository.save(any())).thenAnswer(inv -> {
                BackupRecord r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            BackupRecord result = databaseBackupService.backupInstance(instanceId, "kiteclass_abc12345");
            assertThat(result).isNotNull();
            // Will be FAILED because pg_dump not available, but validation passed
        }

        @Test
        @DisplayName("should create IN_PROGRESS record before starting backup")
        void shouldCreateInProgressRecord() {
            // backupInstance calls executePgDump which requires pg_dump binary.
            // In unit tests, pg_dump is not available, so it will fail.
            // We verify the record is created with IN_PROGRESS status initially,
            // then ends as FAILED (since pg_dump is not available in test).
            BackupRecord savedRecord = BackupRecord.builder()
                .id(UUID.randomUUID())
                .instanceId(instanceId)
                .databaseName("test_db")
                .s3Key("backups/" + instanceId + "/test_db-2026.dump")
                .status(BackupStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();

            when(backupRecordRepository.save(any(BackupRecord.class)))
                .thenReturn(savedRecord);

            BackupRecord result = databaseBackupService.backupInstance(instanceId, "test_db");

            // First save = IN_PROGRESS, second save = FAILED (pg_dump not available)
            ArgumentCaptor<BackupRecord> captor = ArgumentCaptor.forClass(BackupRecord.class);
            verify(backupRecordRepository, times(2)).save(captor.capture());

            BackupRecord firstSave = captor.getAllValues().get(0);
            assertThat(firstSave.getStatus()).isEqualTo(BackupStatus.IN_PROGRESS);
            assertThat(firstSave.getInstanceId()).isEqualTo(instanceId);
            assertThat(firstSave.getDatabaseName()).isEqualTo("test_db");
            assertThat(firstSave.getS3Key()).contains(instanceId.toString());
        }

        @Test
        @DisplayName("should set FAILED status when pg_dump fails")
        void shouldSetFailedOnPgDumpError() {
            BackupRecord savedRecord = BackupRecord.builder()
                .id(UUID.randomUUID())
                .instanceId(instanceId)
                .databaseName("test_db")
                .s3Key("backups/test.dump")
                .status(BackupStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();

            when(backupRecordRepository.save(any(BackupRecord.class)))
                .thenReturn(savedRecord);

            BackupRecord result = databaseBackupService.backupInstance(instanceId, "test_db");

            // Second save should have FAILED status
            ArgumentCaptor<BackupRecord> captor = ArgumentCaptor.forClass(BackupRecord.class);
            verify(backupRecordRepository, times(2)).save(captor.capture());

            BackupRecord finalSave = captor.getAllValues().get(1);
            assertThat(finalSave.getStatus()).isEqualTo(BackupStatus.FAILED);
            assertThat(finalSave.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("verifyBackup")
    class VerifyBackup {

        @Test
        @DisplayName("should return true when checksum matches")
        void shouldReturnTrueWhenChecksumMatches() throws Exception {
            byte[] testData = "test backup data".getBytes();
            // Calculate expected checksum
            String expectedChecksum = databaseBackupService.calculateSha256(
                new ByteArrayInputStream(testData));

            BackupRecord record = BackupRecord.builder()
                .id(UUID.randomUUID())
                .instanceId(instanceId)
                .s3Key("backups/test.dump")
                .status(BackupStatus.COMPLETED)
                .checksumSha256(expectedChecksum)
                .build();

            when(backupRecordRepository.findById(record.getId()))
                .thenReturn(Optional.of(record));
            when(backupStorageService.downloadBackup("backups/test.dump"))
                .thenReturn(new ByteArrayInputStream(testData));

            boolean result = databaseBackupService.verifyBackup(record.getId());

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when checksum does not match")
        void shouldReturnFalseWhenChecksumMismatch() {
            BackupRecord record = BackupRecord.builder()
                .id(UUID.randomUUID())
                .instanceId(instanceId)
                .s3Key("backups/test.dump")
                .status(BackupStatus.COMPLETED)
                .checksumSha256("wrong_checksum_abc123")
                .build();

            when(backupRecordRepository.findById(record.getId()))
                .thenReturn(Optional.of(record));
            when(backupStorageService.downloadBackup("backups/test.dump"))
                .thenReturn(new ByteArrayInputStream("test data".getBytes()));

            boolean result = databaseBackupService.verifyBackup(record.getId());

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should throw when backup not found")
        void shouldThrowWhenBackupNotFound() {
            UUID backupId = UUID.randomUUID();
            when(backupRecordRepository.findById(backupId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> databaseBackupService.verifyBackup(backupId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Backup record not found");
        }

        @Test
        @DisplayName("should throw when backup is not COMPLETED")
        void shouldThrowWhenBackupNotCompleted() {
            BackupRecord record = BackupRecord.builder()
                .id(UUID.randomUUID())
                .instanceId(instanceId)
                .s3Key("backups/test.dump")
                .status(BackupStatus.IN_PROGRESS)
                .build();

            when(backupRecordRepository.findById(record.getId()))
                .thenReturn(Optional.of(record));

            assertThatThrownBy(() -> databaseBackupService.verifyBackup(record.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot verify non-completed backup");
        }

        @Test
        @DisplayName("should return false when download throws exception")
        void shouldReturnFalseWhenDownloadFails() {
            BackupRecord record = BackupRecord.builder()
                .id(UUID.randomUUID())
                .instanceId(instanceId)
                .s3Key("backups/test.dump")
                .status(BackupStatus.COMPLETED)
                .checksumSha256("some_checksum")
                .build();

            when(backupRecordRepository.findById(record.getId()))
                .thenReturn(Optional.of(record));
            when(backupStorageService.downloadBackup("backups/test.dump"))
                .thenThrow(new RuntimeException("S3 connection error"));

            boolean result = databaseBackupService.verifyBackup(record.getId());

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getBackupsForInstance")
    class GetBackupsForInstance {

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            List<BackupRecord> expectedRecords = List.of(
                BackupRecord.builder().instanceId(instanceId).status(BackupStatus.COMPLETED).build()
            );
            when(backupRecordRepository.findByInstanceIdOrderByCreatedAtDesc(instanceId))
                .thenReturn(expectedRecords);

            List<BackupRecord> result = databaseBackupService.getBackupsForInstance(instanceId);

            assertThat(result).hasSize(1);
            verify(backupRecordRepository).findByInstanceIdOrderByCreatedAtDesc(instanceId);
        }

        @Test
        @DisplayName("should return empty list when no backups exist")
        void shouldReturnEmptyListWhenNoBackups() {
            when(backupRecordRepository.findByInstanceIdOrderByCreatedAtDesc(instanceId))
                .thenReturn(Collections.emptyList());

            List<BackupRecord> result = databaseBackupService.getBackupsForInstance(instanceId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("cleanupOldBackups")
    class CleanupOldBackups {

        @Test
        @DisplayName("should keep N completed backups and delete the rest")
        void shouldKeepNAndDeleteRest() {
            BackupRecord keep1 = BackupRecord.builder()
                .id(UUID.randomUUID()).instanceId(instanceId)
                .s3Key("backup1.dump").status(BackupStatus.COMPLETED).build();
            BackupRecord keep2 = BackupRecord.builder()
                .id(UUID.randomUUID()).instanceId(instanceId)
                .s3Key("backup2.dump").status(BackupStatus.COMPLETED).build();
            BackupRecord delete1 = BackupRecord.builder()
                .id(UUID.randomUUID()).instanceId(instanceId)
                .s3Key("backup3.dump").status(BackupStatus.COMPLETED).build();

            when(backupRecordRepository.findByInstanceIdOrderByCreatedAtDesc(instanceId))
                .thenReturn(List.of(keep1, keep2, delete1));

            int deleted = databaseBackupService.cleanupOldBackups(instanceId, 2);

            assertThat(deleted).isEqualTo(1);
            verify(backupStorageService).deleteBackup("backup3.dump");
            verify(backupRecordRepository).save(delete1);
            assertThat(delete1.getStatus()).isEqualTo(BackupStatus.DELETED);
        }

        @Test
        @DisplayName("should also mark FAILED records as DELETED")
        void shouldMarkFailedAsDeleted() {
            BackupRecord completed = BackupRecord.builder()
                .id(UUID.randomUUID()).instanceId(instanceId)
                .s3Key("backup1.dump").status(BackupStatus.COMPLETED).build();
            BackupRecord failed = BackupRecord.builder()
                .id(UUID.randomUUID()).instanceId(instanceId)
                .s3Key("backup2.dump").status(BackupStatus.FAILED).build();

            when(backupRecordRepository.findByInstanceIdOrderByCreatedAtDesc(instanceId))
                .thenReturn(List.of(completed, failed));

            int deleted = databaseBackupService.cleanupOldBackups(instanceId, 5);

            assertThat(deleted).isEqualTo(1); // only failed
            assertThat(failed.getStatus()).isEqualTo(BackupStatus.DELETED);
            verify(backupRecordRepository).save(failed);
        }

        @Test
        @DisplayName("should return 0 when fewer backups than retention count")
        void shouldReturnZeroWhenFewerBackups() {
            BackupRecord only = BackupRecord.builder()
                .id(UUID.randomUUID()).instanceId(instanceId)
                .s3Key("backup1.dump").status(BackupStatus.COMPLETED).build();

            when(backupRecordRepository.findByInstanceIdOrderByCreatedAtDesc(instanceId))
                .thenReturn(List.of(only));

            int deleted = databaseBackupService.cleanupOldBackups(instanceId, 5);

            assertThat(deleted).isEqualTo(0);
            verify(backupStorageService, never()).deleteBackup(anyString());
        }
    }

    @Nested
    @DisplayName("getLatestBackup")
    class GetLatestBackup {

        @Test
        @DisplayName("should return latest completed backup")
        void shouldReturnLatestCompleted() {
            BackupRecord record = BackupRecord.builder()
                .instanceId(instanceId).status(BackupStatus.COMPLETED).build();
            when(backupRecordRepository.findTopByInstanceIdAndStatusOrderByCreatedAtDesc(
                instanceId, BackupStatus.COMPLETED))
                .thenReturn(Optional.of(record));

            Optional<BackupRecord> result = databaseBackupService.getLatestBackup(instanceId);

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(BackupStatus.COMPLETED);
        }

        @Test
        @DisplayName("should return empty when no completed backup exists")
        void shouldReturnEmptyWhenNoBackup() {
            when(backupRecordRepository.findTopByInstanceIdAndStatusOrderByCreatedAtDesc(
                instanceId, BackupStatus.COMPLETED))
                .thenReturn(Optional.empty());

            Optional<BackupRecord> result = databaseBackupService.getLatestBackup(instanceId);

            assertThat(result).isEmpty();
        }
    }
}
