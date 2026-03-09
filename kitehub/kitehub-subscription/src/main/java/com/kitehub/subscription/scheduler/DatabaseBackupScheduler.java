package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    /**
     * Daily backup all active instance databases.
     * Runs at 2:00 AM every day.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void backupAllDatabases() {
        log.info("Starting daily database backup job");

        List<Instance> activeInstances = instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.ACTIVE);
        log.info("Found {} active instances to backup", activeInstances.size());

        int successCount = 0;
        int failureCount = 0;

        for (Instance instance : activeInstances) {
            try {
                backupInstanceDatabase(instance);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to backup database for instance: {}", instance.getId(), e);
                failureCount++;
            }
        }

        log.info("Daily backup job completed. Success: {}, Failed: {}", successCount, failureCount);
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

        // TODO: Add query to find deleted instances older than 30 days
        // List<Instance> deletedInstances = instanceRepository.findDeletedBefore(thirtyDaysAgo);

        log.info("Weekly cleanup job completed");
    }

    /**
     * Backup single instance database.
     * TODO: Implement actual backup to S3 using pg_dump
     *
     * @param instance Instance to backup
     */
    private void backupInstanceDatabase(Instance instance) {
        String dbName = extractDatabaseName(instance.getDatabaseUrl());
        String backupPath = generateBackupPath(instance.getId(), dbName);

        log.debug("Backing up database {} to {}", dbName, backupPath);

        // TODO: Implement actual backup
        // 1. Run pg_dump to create SQL dump
        // 2. Compress with gzip
        // 3. Upload to S3: s3://kiteclass-backups/{instance-id}/{date}.sql.gz
        // 4. Delete local dump file

        log.debug("Backup completed for database: {}", dbName);
    }

    /**
     * Extract database name from JDBC URL.
     *
     * @param databaseUrl JDBC URL
     * @return Database name
     */
    private String extractDatabaseName(String databaseUrl) {
        String[] parts = databaseUrl.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Generate S3 backup path.
     *
     * @param instanceId Instance UUID
     * @param dbName Database name
     * @return S3 path
     */
    private String generateBackupPath(java.util.UUID instanceId, String dbName) {
        String date = LocalDateTime.now().toString().split("T")[0]; // YYYY-MM-DD
        return String.format("s3://kiteclass-backups/%s/%s-%s.sql.gz",
            instanceId, date, dbName);
    }
}
