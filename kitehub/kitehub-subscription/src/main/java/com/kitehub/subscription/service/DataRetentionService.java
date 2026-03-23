package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.DataRetentionConfig;
import com.kitehub.subscription.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing data retention policies.
 * Handles warning notifications and data deletion for suspended instances.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataRetentionService {

    private final InstanceRepository instanceRepository;
    private final DataRetentionConfig retentionConfig;
    private final EmailServiceClient emailServiceClient;

    /**
     * Get retention days for a specific instance based on its tier.
     *
     * @param instanceId instance ID
     * @return retention days, or default FREE tier days if not found
     */
    public int getRetentionDays(UUID instanceId) {
        Instance instance = instanceRepository.findById(instanceId).orElse(null);
        if (instance == null) {
            return retentionConfig.getFree();
        }
        return retentionConfig.getRetentionDays(
            instance.getTier() != null ? instance.getTier().name() : "FREE");
    }

    /**
     * Process retention warnings for suspended instances.
     * Sends warning emails at configured intervals before data deletion.
     * Warnings are sent at 50% and 80% of the retention period.
     *
     * @return number of warnings sent
     */
    @Transactional(readOnly = true)
    public int processRetentionWarnings() {
        log.info("Processing data retention warnings");

        List<Instance> suspendedInstances = instanceRepository
            .findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED);

        int warningsSent = 0;

        for (Instance instance : suspendedInstances) {
            try {
                int retentionDays = retentionConfig.getRetentionDays(
                    instance.getTier() != null ? instance.getTier().name() : "FREE");

                LocalDateTime suspendedAt = instance.getUpdatedAt();
                if (suspendedAt == null) {
                    continue;
                }

                long daysSuspended = ChronoUnit.DAYS.between(suspendedAt, LocalDateTime.now());
                long daysLeft = retentionDays - daysSuspended;

                if (daysLeft <= 0) {
                    // Will be handled by processExpiredRetention
                    continue;
                }

                // Send warnings at ~50% and ~80% of retention period
                boolean shouldWarn = shouldSendWarning(daysSuspended, retentionDays);

                if (shouldWarn && instance.getContactEmail() != null) {
                    emailServiceClient.sendRetentionWarning(
                        instance.getId(),
                        instance.getContactEmail(),
                        instance.getOrganizationName(),
                        daysLeft
                    );
                    warningsSent++;
                    log.info("Retention warning sent for instance {} (subdomain: {}, {} days left)",
                        instance.getId(), instance.getSubdomain(), daysLeft);
                }
            } catch (Exception e) {
                log.error("Failed to process retention warning for instance: {}",
                    instance.getId(), e);
            }
        }

        log.info("Sent {} retention warnings", warningsSent);
        return warningsSent;
    }

    /**
     * Process expired retention for suspended instances.
     * Marks instances as DELETED when their retention period has passed.
     *
     * @return number of instances deleted
     */
    @Transactional
    public int processExpiredRetention() {
        log.info("Processing expired data retention");

        List<Instance> suspendedInstances = instanceRepository
            .findByStatusAndDeletedFalse(InstanceStatus.SUSPENDED);

        int deletedCount = 0;

        for (Instance instance : suspendedInstances) {
            try {
                int retentionDays = retentionConfig.getRetentionDays(
                    instance.getTier() != null ? instance.getTier().name() : "FREE");

                LocalDateTime suspendedAt = instance.getUpdatedAt();
                if (suspendedAt == null) {
                    continue;
                }

                LocalDateTime retentionExpiry = suspendedAt.plusDays(retentionDays);
                LocalDateTime now = LocalDateTime.now();

                // Send final warning 1 day before deletion
                long daysUntilExpiry = ChronoUnit.DAYS.between(now, retentionExpiry);
                if (daysUntilExpiry == 1 && instance.getContactEmail() != null) {
                    emailServiceClient.sendDataRetentionFinalWarning(
                        instance.getId(),
                        instance.getContactEmail(),
                        instance.getSubdomain()
                    );
                    log.info("Data retention final warning sent for instance {} (subdomain: {})",
                        instance.getId(), instance.getSubdomain());
                }

                if (now.isAfter(retentionExpiry)) {
                    // Log for future backup implementation (pg_dump)
                    log.info("Retention expired for instance {} (subdomain: {}). "
                        + "FUTURE: backup data via pg_dump before deletion.",
                        instance.getId(), instance.getSubdomain());

                    // Mark as DELETED
                    instance.setStatus(InstanceStatus.DELETED);
                    instance.softDelete();
                    instanceRepository.save(instance);
                    deletedCount++;

                    // Send notification
                    if (instance.getContactEmail() != null) {
                        emailServiceClient.sendDataDeletedNotification(
                            instance.getId(),
                            instance.getContactEmail(),
                            instance.getOrganizationName()
                        );
                    }

                    log.info("Instance {} marked as DELETED after retention period expired",
                        instance.getId());
                }
            } catch (Exception e) {
                log.error("Failed to process expired retention for instance: {}",
                    instance.getId(), e);
            }
        }

        log.info("Deleted {} instances with expired retention", deletedCount);
        return deletedCount;
    }

    /**
     * Determine if a warning should be sent based on days suspended and retention period.
     * Sends warnings at approximately 50% and 80% of the retention period.
     *
     * @param daysSuspended number of days since suspension
     * @param retentionDays total retention days for the tier
     * @return true if a warning should be sent
     */
    boolean shouldSendWarning(long daysSuspended, int retentionDays) {
        if (retentionDays <= 0) {
            return false;
        }

        // Warning at ~50% of retention period
        long firstWarningDay = retentionDays / 2;
        // Warning at ~80% of retention period
        long secondWarningDay = (long) (retentionDays * 0.8);

        return daysSuspended == firstWarningDay || daysSuspended == secondWarningDay;
    }
}
