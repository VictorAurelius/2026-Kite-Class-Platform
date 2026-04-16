package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.domain.BackupRecord;
import com.kitehub.subscription.domain.BackupStatus;
import com.kitehub.subscription.dto.PurgeResult;
import com.kitehub.subscription.dto.PurgeStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.DatabaseBackupService;
import com.kitehub.subscription.service.InstancePurgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Scheduler for automated database backups and cleanup tasks.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseBackupScheduler {

    private final InstanceRepository instanceRepository;
    private final DatabaseBackupService databaseBackupService;
    private final InstancePurgeService instancePurgeService;

    @Value("${backup.retention-count:7}")
    private int retentionCount;

    /**
     * Daily backup all active instance databases.
     * Runs at 2:00 AM every day.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void backupAllDatabases() {
        Instant start = Instant.now();
        log.info("Starting daily database backup job");

        List<Instance> activeInstances = instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.ACTIVE);
        log.info("Found {} active instances to backup", activeInstances.size());

        int successCount = 0;
        int failureCount = 0;
        long totalSizeBytes = 0;

        for (Instance instance : activeInstances) {
            try {
                String dbName = extractDatabaseName(instance.getDatabaseUrl());
                BackupRecord record = databaseBackupService.backupInstance(instance.getId(), dbName);

                if (record.getStatus() == BackupStatus.COMPLETED) {
                    successCount++;
                    if (record.getFileSizeBytes() != null) {
                        totalSizeBytes += record.getFileSizeBytes();
                    }
                } else {
                    failureCount++;
                }

                // Cleanup old backups after successful backup
                if (record.getStatus() == BackupStatus.COMPLETED) {
                    databaseBackupService.cleanupOldBackups(instance.getId(), retentionCount);
                }
            } catch (Exception e) {
                log.error("Failed to backup database for instance: {}", instance.getId(), e);
                failureCount++;
            }
        }

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("Daily backup job completed in {}s. Success: {}, Failed: {}, Total size: {} MB",
            elapsed.getSeconds(), successCount, failureCount,
            String.format("%.2f", totalSizeBytes / (1024.0 * 1024.0)));
    }

    /**
     * Weekly purge of deleted instances.
     * Permanently removes instances deleted more than 30 days ago,
     * including dropping their databases, deleting backups from S3,
     * and publishing cross-service cleanup events.
     * <p>
     * Safety: instances without a COMPLETED backup are SKIPPED.
     * Runs at 3:00 AM every Sunday.
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanupDeletedInstances() {
        Instant start = Instant.now();
        log.info("Starting weekly purge of deleted instances");

        List<Instance> eligibleInstances = instancePurgeService.findPurgeEligible();
        log.info("Found {} instances eligible for purge", eligibleInstances.size());

        int purgedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (Instance instance : eligibleInstances) {
            try {
                PurgeResult result = instancePurgeService.purgeInstance(instance.getId());
                if (result.getStatus() == PurgeStatus.SUCCESS) {
                    purgedCount++;
                } else if (result.getStatus() == PurgeStatus.SKIPPED_NO_BACKUP) {
                    skippedCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to purge instance: {}", instance.getId(), e);
                failedCount++;
            }
        }

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("Weekly purge job completed in {}s. Purged: {}, Skipped (no backup): {}, Failed: {}",
            elapsed.getSeconds(), purgedCount, skippedCount, failedCount);
    }

    /**
     * Extract database name from JDBC URL.
     *
     * @param databaseUrl JDBC URL
     * @return Database name
     */
    String extractDatabaseName(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            throw new IllegalArgumentException("Database URL is null or empty");
        }
        // Handle JDBC URL like jdbc:postgresql://host:port/dbname or just host/dbname
        String cleanUrl = databaseUrl;
        // Remove query params if present
        int queryIdx = cleanUrl.indexOf('?');
        if (queryIdx >= 0) {
            cleanUrl = cleanUrl.substring(0, queryIdx);
        }
        String[] parts = cleanUrl.split("/");
        return parts[parts.length - 1];
    }
}
