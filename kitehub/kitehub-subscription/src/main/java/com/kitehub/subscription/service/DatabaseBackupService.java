package com.kitehub.subscription.service;

import com.kitehub.subscription.domain.BackupRecord;
import com.kitehub.subscription.domain.BackupStatus;
import com.kitehub.subscription.repository.BackupRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating database backup operations.
 * Executes pg_dump, calculates checksums, and uploads to S3/MinIO.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseBackupService {

    private final BackupRecordRepository backupRecordRepository;
    private final BackupStorageService backupStorageService;

    @Value("${database.master.host:localhost}")
    private String dbHost;

    @Value("${database.master.port:5433}")
    private int dbPort;

    @Value("${database.admin.username:postgres}")
    private String dbUser;

    @Value("${database.admin.password:}")
    private String dbPassword;

    @Value("${backup.retention-count:7}")
    private int retentionCount;

    @Value("${backup.pg-dump-path:pg_dump}")
    private String pgDumpPath;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Execute a full backup for an instance database.
     * Steps: create record -> pg_dump -> checksum -> upload to S3 -> update record.
     *
     * @param instanceId instance UUID
     * @param databaseName name of the database to backup
     * @return the completed BackupRecord
     */
    @Transactional
    public BackupRecord backupInstance(UUID instanceId, String databaseName) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String s3Key = String.format("backups/%s/%s-%s.dump", instanceId, databaseName, timestamp);

        BackupRecord record = BackupRecord.builder()
            .instanceId(instanceId)
            .databaseName(databaseName)
            .s3Key(s3Key)
            .status(BackupStatus.IN_PROGRESS)
            .startedAt(LocalDateTime.now())
            .build();
        record = backupRecordRepository.save(record);

        try {
            File tempFile = executePgDump(databaseName);
            try {
                String checksum = calculateSha256(tempFile);
                long fileSize = tempFile.length();

                try (InputStream is = new FileInputStream(tempFile)) {
                    backupStorageService.uploadBackup(s3Key, is, fileSize);
                }

                record.setStatus(BackupStatus.COMPLETED);
                record.setCompletedAt(LocalDateTime.now());
                record.setFileSizeBytes(fileSize);
                record.setChecksumSha256(checksum);

                log.info("Backup completed for instance {} database {} -> {} ({} bytes, sha256={})",
                    instanceId, databaseName, s3Key, fileSize, checksum);
            } finally {
                if (!tempFile.delete()) {
                    log.warn("Failed to delete temp backup file: {}", tempFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            record.setStatus(BackupStatus.FAILED);
            record.setCompletedAt(LocalDateTime.now());
            record.setErrorMessage(truncateMessage(e.getMessage(), 1000));
            log.error("Backup failed for instance {} database {}: {}", instanceId, databaseName, e.getMessage(), e);
        }

        return backupRecordRepository.save(record);
    }

    /**
     * Verify a backup by downloading and checking its SHA-256 checksum.
     *
     * @param backupId backup record UUID
     * @return true if checksum matches
     */
    public boolean verifyBackup(UUID backupId) {
        BackupRecord record = backupRecordRepository.findById(backupId)
            .orElseThrow(() -> new IllegalArgumentException("Backup record not found: " + backupId));

        if (record.getStatus() != BackupStatus.COMPLETED) {
            throw new IllegalStateException("Cannot verify non-completed backup: " + record.getStatus());
        }

        try (InputStream is = backupStorageService.downloadBackup(record.getS3Key())) {
            String downloadedChecksum = calculateSha256(is);
            boolean matches = downloadedChecksum.equals(record.getChecksumSha256());

            if (!matches) {
                log.error("Checksum mismatch for backup {}: expected={}, actual={}",
                    backupId, record.getChecksumSha256(), downloadedChecksum);
            } else {
                log.info("Backup {} verified successfully (sha256={})", backupId, downloadedChecksum);
            }

            return matches;
        } catch (Exception e) {
            log.error("Failed to verify backup {}: {}", backupId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get all backups for an instance, most recent first.
     *
     * @param instanceId instance UUID
     * @return list of backup records
     */
    @Transactional(readOnly = true)
    public List<BackupRecord> getBackupsForInstance(UUID instanceId) {
        return backupRecordRepository.findByInstanceIdOrderByCreatedAtDesc(instanceId);
    }

    /**
     * Get the latest successful backup for an instance.
     *
     * @param instanceId instance UUID
     * @return optional backup record
     */
    @Transactional(readOnly = true)
    public Optional<BackupRecord> getLatestBackup(UUID instanceId) {
        return backupRecordRepository.findTopByInstanceIdAndStatusOrderByCreatedAtDesc(
            instanceId, BackupStatus.COMPLETED);
    }

    /**
     * Clean up old backups, keeping only the N most recent completed backups.
     * Deletes both the S3 object and marks the record as DELETED.
     *
     * @param instanceId instance UUID
     * @param retainCount number of backups to keep
     * @return number of backups deleted
     */
    @Transactional
    public int cleanupOldBackups(UUID instanceId, int retainCount) {
        List<BackupRecord> allBackups = backupRecordRepository
            .findByInstanceIdOrderByCreatedAtDesc(instanceId);

        int deletedCount = 0;
        int keptCount = 0;

        for (BackupRecord backup : allBackups) {
            if (backup.getStatus() == BackupStatus.COMPLETED) {
                keptCount++;
                if (keptCount > retainCount) {
                    try {
                        backupStorageService.deleteBackup(backup.getS3Key());
                        backup.setStatus(BackupStatus.DELETED);
                        backupRecordRepository.save(backup);
                        deletedCount++;
                        log.info("Cleaned up old backup {} for instance {}", backup.getId(), instanceId);
                    } catch (Exception e) {
                        log.error("Failed to cleanup backup {} for instance {}: {}",
                            backup.getId(), instanceId, e.getMessage());
                    }
                }
            } else if (backup.getStatus() == BackupStatus.FAILED) {
                // Clean up failed backup records older than the retention window
                backup.setStatus(BackupStatus.DELETED);
                backupRecordRepository.save(backup);
                deletedCount++;
            }
        }

        if (deletedCount > 0) {
            log.info("Cleaned up {} old backups for instance {} (retained {})",
                deletedCount, instanceId, retainCount);
        }

        return deletedCount;
    }

    /**
     * Execute pg_dump and write output to a temp file.
     *
     * @param databaseName name of the database
     * @return temp file containing the dump
     * @throws IOException if pg_dump fails
     * @throws InterruptedException if process is interrupted
     */
    File executePgDump(String databaseName) throws IOException, InterruptedException {
        File tempFile = File.createTempFile("kite-backup-", ".dump");

        ProcessBuilder pb = new ProcessBuilder(
            pgDumpPath,
            "-h", dbHost,
            "-p", String.valueOf(dbPort),
            "-U", dbUser,
            "-d", databaseName,
            "--format=custom",
            "--compress=6"
        );
        pb.environment().put("PGPASSWORD", dbPassword);
        pb.redirectOutput(tempFile);
        pb.redirectErrorStream(false);

        Process process = pb.start();

        // Capture stderr for error reporting
        String stderr;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            stderr = String.join("\n", reader.lines().toList());
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            if (!tempFile.delete()) {
                log.warn("Failed to delete temp file after pg_dump failure: {}", tempFile.getAbsolutePath());
            }
            throw new IOException("pg_dump failed with exit code " + exitCode + ": " + stderr);
        }

        log.debug("pg_dump completed for database {}, temp file: {} ({} bytes)",
            databaseName, tempFile.getAbsolutePath(), tempFile.length());

        return tempFile;
    }

    /**
     * Calculate SHA-256 checksum of a file.
     *
     * @param file file to checksum
     * @return hex-encoded SHA-256
     */
    String calculateSha256(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return calculateSha256(is);
        }
    }

    /**
     * Calculate SHA-256 checksum of an input stream.
     *
     * @param inputStream data stream
     * @return hex-encoded SHA-256
     */
    String calculateSha256(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Truncate error message to avoid DB column overflow.
     */
    private String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return null;
        }
        return message.length() > maxLength ? message.substring(0, maxLength) : message;
    }
}
