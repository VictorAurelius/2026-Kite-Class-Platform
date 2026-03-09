package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.dto.TrialStatusResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service for managing trial periods.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialService {

    private final InstanceRepository instanceRepository;
    private static final int DEFAULT_TRIAL_DAYS = 14;

    /**
     * Start trial period for instance.
     * Sets trial_started_at to now and trial_expires_at to 14 days from now.
     *
     * @param instanceId UUID of the instance
     * @throws IllegalArgumentException if instance not found
     */
    @Transactional
    public void startTrial(UUID instanceId) {
        log.info("Starting trial for instance: {}", instanceId);

        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (instance.getTrialStartedAt() != null) {
            log.warn("Trial already started for instance: {}", instanceId);
            return;
        }

        instance.startTrial();
        instanceRepository.save(instance);

        log.info("Trial started for instance: {} (expires: {})", instanceId, instance.getTrialExpiresAt());
    }

    /**
     * Get trial status for instance.
     *
     * @param instanceId UUID of the instance
     * @return Trial status information
     * @throws IllegalArgumentException if instance not found
     */
    @Transactional(readOnly = true)
    public TrialStatusResponse getTrialStatus(UUID instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        return TrialStatusResponse.builder()
            .instanceId(instance.getId())
            .subdomain(instance.getSubdomain())
            .status(instance.getStatus())
            .isOnTrial(instance.isOnTrial())
            .trialStartedAt(instance.getTrialStartedAt())
            .trialExpiresAt(instance.getTrialExpiresAt())
            .daysLeft(instance.getTrialDaysLeft())
            .needsWarning(shouldSendWarning(instance))
            .warningLevel(getWarningLevel(instance))
            .build();
    }

    /**
     * Extend trial period by specified number of days.
     * Admin-only operation.
     *
     * @param instanceId UUID of the instance
     * @param days Number of days to extend
     * @throws IllegalArgumentException if instance not found or not on trial
     */
    @Transactional
    public void extendTrial(UUID instanceId, int days) {
        log.info("Extending trial for instance {} by {} days", instanceId, days);

        if (days <= 0 || days > 90) {
            throw new IllegalArgumentException("Invalid extension days: " + days + " (must be 1-90)");
        }

        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (instance.getStatus() != InstanceStatus.TRIAL) {
            throw new IllegalArgumentException("Instance is not on trial: " + instanceId);
        }

        LocalDateTime newExpiresAt = instance.getTrialExpiresAt().plusDays(days);
        instance.setTrialExpiresAt(newExpiresAt);
        instanceRepository.save(instance);

        log.info("Trial extended for instance: {} (new expiry: {})", instanceId, newExpiresAt);
    }

    /**
     * Check if instance trial has expired.
     *
     * @param instanceId UUID of the instance
     * @return true if expired, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isTrialExpired(UUID instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (instance.getStatus() != InstanceStatus.TRIAL) {
            return false;
        }

        return !instance.isOnTrial(); // isOnTrial() checks if current time is before expiry
    }

    /**
     * Check if instance should receive trial warning notification.
     *
     * @param instance Instance entity
     * @return true if warning should be sent
     */
    private boolean shouldSendWarning(Instance instance) {
        if (instance.getStatus() != InstanceStatus.TRIAL) {
            return false;
        }

        long daysLeft = instance.getTrialDaysLeft();
        return daysLeft <= 3 && daysLeft >= 0; // Warn at 3 days, 2 days, 1 day, 0 days
    }

    /**
     * Get warning level based on days left.
     *
     * @param instance Instance entity
     * @return Warning level: HIGH (0-1 days), MEDIUM (2-3 days), NONE
     */
    private String getWarningLevel(Instance instance) {
        if (instance.getStatus() != InstanceStatus.TRIAL) {
            return "NONE";
        }

        long daysLeft = instance.getTrialDaysLeft();

        if (daysLeft <= 0) {
            return "EXPIRED";
        } else if (daysLeft == 1) {
            return "HIGH"; // Last day
        } else if (daysLeft <= 3) {
            return "MEDIUM"; // 2-3 days left
        } else {
            return "NONE";
        }
    }

    /**
     * Convert trial to paid subscription.
     * Updates instance status to ACTIVE.
     *
     * @param instanceId UUID of the instance
     * @throws IllegalArgumentException if instance not found or not on trial
     */
    @Transactional
    public void convertTrialToSubscription(UUID instanceId) {
        log.info("Converting trial to subscription for instance: {}", instanceId);

        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (instance.getStatus() != InstanceStatus.TRIAL) {
            throw new IllegalArgumentException("Instance is not on trial: " + instanceId);
        }

        instance.setStatus(InstanceStatus.ACTIVE);
        instanceRepository.save(instance);

        log.info("Trial converted to subscription for instance: {}", instanceId);
    }

    /**
     * Suspend instance when trial expires without subscription.
     *
     * @param instanceId UUID of the instance
     */
    @Transactional
    public void suspendExpiredTrial(UUID instanceId) {
        log.info("Suspending expired trial for instance: {}", instanceId);

        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (instance.getStatus() != InstanceStatus.TRIAL) {
            log.warn("Cannot suspend non-trial instance: {}", instanceId);
            return;
        }

        instance.suspend();
        instanceRepository.save(instance);

        log.info("Expired trial suspended for instance: {}", instanceId);
    }
}
