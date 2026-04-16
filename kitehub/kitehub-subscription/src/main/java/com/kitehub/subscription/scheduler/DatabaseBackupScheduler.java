package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.domain.BackupRecord;
import com.kitehub.subscription.domain.BackupStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.DatabaseBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
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
     * Weekly cleanup of deleted instances.
     * Removes instances deleted more than 30 days ago.
     * Runs at 3:00 AM every Sunday.
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanupDeletedInstances() {
        log.info("Starting weekly cleanup of deleted instances");

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Instance> deletedInstances = instanceRepository
            .findByStatusAndDeletedFalseAndUpdatedAtBefore(InstanceStatus.DELETED, thirtyDaysAgo);

        int cleanedCount = 0;
        for (Instance instance : deletedInstances) {
            try {
                instance.softDelete();
                instanceRepository.save(instance);
                cleanedCount++;
                log.info("Cleaned up deleted instance: {} (subdomain: {})",
                    instance.getId(), instance.getSubdomain());
            } catch (Exception e) {
                log.error("Failed to clean up instance: {}", instance.getId(), e);
            }
        }

        log.info("Weekly cleanup job completed. Cleaned: {} instances", cleanedCount);
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
