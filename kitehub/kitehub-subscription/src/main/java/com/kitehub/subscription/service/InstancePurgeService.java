package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.config.PurgeQueueConfig;
import com.kitehub.subscription.domain.BackupRecord;
import com.kitehub.subscription.domain.BackupStatus;
import com.kitehub.subscription.dto.PurgeEvent;
import com.kitehub.subscription.dto.PurgeResult;
import com.kitehub.subscription.dto.PurgeStatus;
import com.kitehub.subscription.repository.BackupRecordRepository;
import com.kitehub.subscription.repository.EmailSentLogRepository;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating hard delete (purge) of instances.
 * Permanently removes all resources for soft-deleted instances after retention period.
 * <p>
 * Safety: Always verifies at least one COMPLETED backup exists before purging.
 * If no backup is found, the purge is SKIPPED and a warning is logged.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstancePurgeService {

    private final InstanceRepository instanceRepository;
    private final DatabaseProvisioningService databaseProvisioningService;
    private final BackupStorageService backupStorageService;
    private final BackupRecordRepository backupRecordRepository;
    private final EmailSentLogRepository emailSentLogRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SubscriptionEventEmitter eventEmitter;

    private static final int PURGE_RETENTION_DAYS = 30;

    /**
     * Purge a single instance — verify backup exists, then delete all resources.
     * Pre-condition: instance must be in DELETED status.
     *
     * @param instanceId instance UUID
     * @return PurgeResult with details of the purge operation
     */
    @Transactional
    public PurgeResult purgeInstance(UUID instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + instanceId));

        if (instance.getStatus() != InstanceStatus.DELETED) {
            return PurgeResult.builder()
                .instanceId(instanceId)
                .subdomain(instance.getSubdomain())
                .status(PurgeStatus.FAILED)
                .errorMessage("Instance is not in DELETED status (current: " + instance.getStatus() + ")")
                .build();
        }

        return executePurge(instance);
    }

    /**
     * Find all instances eligible for purge (DELETED > 30 days ago).
     *
     * @return list of purge-eligible instances
     */
    public List<Instance> findPurgeEligible() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(PURGE_RETENTION_DAYS);
        return instanceRepository.findByStatusAndUpdatedAtBefore(InstanceStatus.DELETED, cutoff);
    }

    /**
     * Admin manual purge — allows purging a specific instance immediately.
     * Still requires the instance to be in DELETED status and backup verification.
     *
     * @param instanceId instance UUID
     * @return PurgeResult with details
     */
    @Transactional
    public PurgeResult adminPurge(UUID instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + instanceId));

        if (instance.getStatus() != InstanceStatus.DELETED) {
            return PurgeResult.builder()
                .instanceId(instanceId)
                .subdomain(instance.getSubdomain())
                .status(PurgeStatus.FAILED)
                .errorMessage("Instance must be in DELETED status for purge (current: " + instance.getStatus() + ")")
                .build();
        }

        return executePurge(instance);
    }

    /**
     * Execute the actual purge operation on a DELETED instance.
     * Steps:
     * 1. Verify backup exists (SAFETY CHECK)
     * 2. Drop PostgreSQL database
     * 3. Delete all backup files from S3
     * 4. Mark all BackupRecords as DELETED
     * 5. Delete email logs
     * 6. Publish RabbitMQ event for cross-service cleanup
     * 7. Set instance status to PURGED
     *
     * @param instance the instance to purge
     * @return PurgeResult with details
     */
    private PurgeResult executePurge(Instance instance) {
        UUID instanceId = instance.getId();
        String subdomain = instance.getSubdomain();

        log.info("Starting purge for instance {} (subdomain: {})", instanceId, subdomain);

        // 1. SAFETY CHECK: Verify at least one COMPLETED backup exists
        boolean hasBackup = backupRecordRepository.existsByInstanceIdAndStatus(instanceId, BackupStatus.COMPLETED);
        if (!hasBackup) {
            log.warn("PURGE SKIPPED for instance {} (subdomain: {}): no COMPLETED backup found. "
                + "Cannot purge without backup verification.", instanceId, subdomain);
            return PurgeResult.builder()
                .instanceId(instanceId)
                .subdomain(subdomain)
                .status(PurgeStatus.SKIPPED_NO_BACKUP)
                .errorMessage("No COMPLETED backup found — purge requires at least one verified backup")
                .build();
        }

        boolean databaseDropped = false;
        int backupFilesDeleted = 0;
        int emailLogsDeleted = 0;
        boolean brandingCleanupPublished = false;

        try {
            // 2. Drop PostgreSQL database
            try {
                databaseProvisioningService.deleteDatabase(instanceId);
                databaseDropped = true;
                log.info("Database dropped for instance {}", instanceId);
            } catch (Exception e) {
                log.error("Failed to drop database for instance {}: {}", instanceId, e.getMessage());
                // Continue with other cleanup even if DB drop fails
            }

            // 3. Delete all backup files from S3 and mark records as DELETED
            List<BackupRecord> backupRecords = backupRecordRepository.findByInstanceId(instanceId);
            for (BackupRecord record : backupRecords) {
                if (record.getStatus() != BackupStatus.DELETED) {
                    try {
                        backupStorageService.deleteBackup(record.getS3Key());
                        record.setStatus(BackupStatus.DELETED);
                        backupRecordRepository.save(record);
                        backupFilesDeleted++;
                    } catch (Exception e) {
                        log.error("Failed to delete backup {} for instance {}: {}",
                            record.getId(), instanceId, e.getMessage());
                    }
                }
            }
            log.info("Deleted {} backup files for instance {}", backupFilesDeleted, instanceId);

            // 4. Delete email logs for this instance
            try {
                emailSentLogRepository.deleteByInstanceId(instanceId);
                log.info("Deleted email logs for instance {}", instanceId);
            } catch (Exception e) {
                log.error("Failed to delete email logs for instance {}: {}", instanceId, e.getMessage());
            }

            // 5. Outbox + best-effort fast-path publish (per design-patterns.md §3.5.1
            //    Exception A — outbox is the reliability net, direct send is latency optimization).
            //    The outbox row guarantees the cleanup event reaches consumers even if the broker is
            //    down right now; the direct convertAndSend below is a fast-path for current-online consumers.
            LocalDateTime purgedAt = LocalDateTime.now();
            PurgeEvent event = PurgeEvent.builder()
                .instanceId(instanceId)
                .subdomain(subdomain)
                .purgedAt(purgedAt)
                .build();
            String payload = String.format(
                "{\"instanceId\":\"%s\",\"subdomain\":\"%s\",\"purgedAt\":\"%s\"}",
                instanceId, SubscriptionEventEmitter.escape(subdomain), purgedAt);
            eventEmitter.emit(instanceId, PurgeQueueConfig.EVENT_TYPE_PURGE_REQUESTED,
                PurgeQueueConfig.PURGE_ROUTING_KEY, payload);

            try {
                // Best-effort fast-path — outbox is the reliability net.
                rabbitTemplate.convertAndSend(PurgeQueueConfig.PURGE_EXCHANGE, "", event);
                brandingCleanupPublished = true;
                log.info("Published purge event for instance {}", instanceId);
            } catch (Exception e) {
                log.warn("Direct purge publish failed for instance {} — outbox will retry: {}",
                    instanceId, e.getMessage());
            }

            // 6. Set instance status to PURGED
            instance.setStatus(InstanceStatus.PURGED);
            instanceRepository.save(instance);

            log.info("Purge completed for instance {} (subdomain: {}). DB dropped: {}, backups deleted: {}, event published: {}",
                instanceId, subdomain, databaseDropped, backupFilesDeleted, brandingCleanupPublished);

            return PurgeResult.builder()
                .instanceId(instanceId)
                .subdomain(subdomain)
                .status(PurgeStatus.SUCCESS)
                .databaseDropped(databaseDropped)
                .backupFilesDeleted(backupFilesDeleted)
                .emailLogsDeleted(emailLogsDeleted)
                .brandingCleanupPublished(brandingCleanupPublished)
                .purgedAt(purgedAt)
                .build();

        } catch (Exception e) {
            log.error("Purge failed for instance {} (subdomain: {}): {}", instanceId, subdomain, e.getMessage(), e);
            return PurgeResult.builder()
                .instanceId(instanceId)
                .subdomain(subdomain)
                .status(PurgeStatus.FAILED)
                .databaseDropped(databaseDropped)
                .backupFilesDeleted(backupFilesDeleted)
                .emailLogsDeleted(emailLogsDeleted)
                .brandingCleanupPublished(brandingCleanupPublished)
                .errorMessage(e.getMessage())
                .build();
        }
    }
}
