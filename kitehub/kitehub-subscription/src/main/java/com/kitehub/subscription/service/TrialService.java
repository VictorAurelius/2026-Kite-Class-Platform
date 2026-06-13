package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.TrialConfig;
import com.kitehub.subscription.dto.TrialStatusResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final TrialConfig trialConfig;
    private final InstanceTierSyncService instanceTierSyncService;

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

        instance.startTrial(trialConfig.getDurationDays());
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
            .migrationPhase(instance.getMigrationPhase())
            .migrationStartedAt(instance.getMigrationStartedAt())
            .migrationCompletedAt(instance.getMigrationCompletedAt())
            .migrationFailureReason(instance.getMigrationFailureReason())
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
     * Convert trial to paid subscription — simple TRIAL → ACTIVE flip.
     *
     * <p><b>Internal use only:</b> this method is the terminal step inside
     * {@link TrialToPaidService#executeMigration} and does not run the full
     * migration state-machine (INITIATED → PAYMENT_PENDING → ... → COMPLETED)
     * on its own. External callers must go through
     * {@link TrialToPaidService#initiateUpgrade} so that outbox events,
     * idempotency, retry, and the migration-phase column are all handled
     * correctly (GAP-192 Phase 4a + 4b-i).</p>
     *
     * @param instanceId UUID of the instance
     * @param targetTier the paid tier the upgrade selected — the denormalized
     *     {@code instances.tier} is synced to this through the canonical SUB-21
     *     sync point so it never drifts from the new ACTIVE state (GAP-1095). A
     *     trial registers as {@link PricingTier#FREE} ({@code InstanceService} trial
     *     path), so flipping to ACTIVE without setting the tier left a paid instance
     *     stuck on FREE — the desync this param closes.
     * @throws IllegalArgumentException if instance not found or not on trial
     * @deprecated Prefer {@link TrialToPaidService#initiateUpgrade} for new call
     *     sites. Retained because the migration orchestrator delegates the final
     *     status flip here.
     */
    @Deprecated(since = "1.0.0 (GAP-192 Phase 4b-i)")
    @Transactional
    public void convertTrialToSubscription(UUID instanceId, PricingTier targetTier) {
        log.info("Converting trial to subscription for instance: {} (tier={})", instanceId, targetTier);

        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (instance.getStatus() != InstanceStatus.TRIAL) {
            throw new IllegalArgumentException("Instance is not on trial: " + instanceId);
        }

        instance.setStatus(InstanceStatus.ACTIVE);
        // GAP-1095 — route the effective-tier set through the single SUB-21 sync point so
        // connection-pool size / custom-domain eligibility / data-retention readers (all of
        // which read instances.tier) see the paid tier, not the FREE trial tier.
        if (targetTier != null) {
            instanceTierSyncService.syncInstanceTier(instance, targetTier);
        }
        instanceRepository.save(instance);

        log.info("Trial converted to subscription for instance: {} (tier={})", instanceId, instance.getTier());
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
