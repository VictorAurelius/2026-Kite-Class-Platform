package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.subscription.config.TrialToPaidConfig;
import com.kitehub.subscription.dto.RollbackResponse;
import com.kitehub.subscription.dto.UpgradeRequest;
import com.kitehub.subscription.dto.UpgradeResponse;
import com.kitehub.subscription.exception.MigrationException;
import com.kitehub.subscription.outbox.MigrationEventType;
import com.kitehub.subscription.outbox.MigrationOutboxEvent;
import com.kitehub.subscription.outbox.MigrationOutboxRepository;
import com.kitehub.subscription.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrator for the trial-to-paid migration state machine (GAP-192).
 *
 * <p>Implements the flow documented at
 * {@code documents/01-business/kitehub/trial-to-paid-migration/}:
 * <ul>
 *   <li>{@link #initiateUpgrade} — validate + move NONE → INITIATED → PAYMENT_PENDING</li>
 *   <li>{@link #handlePaymentCaptured} — gateway webhook / admin force-convert entry</li>
 *   <li>{@link #executeMigration} — sync MVP worker: MIGRATING → COMPLETED (status flip)</li>
 *   <li>{@link #rollback} — REVERSED within 24h (T2P-04), else 410</li>
 * </ul>
 *
 * <h3>Design-pattern notes</h3>
 * <ul>
 *   <li><b>State Machine:</b> transitions are validated via
 *       {@link MigrationPhase#canTransitionTo(MigrationPhase)} — no switch-cascades.</li>
 *   <li><b>Outbox pattern:</b> every state transition writes an outbox row inside the same
 *       JPA transaction as the {@code Instance} mutation — broker coupling deferred to Phase 4b.</li>
 *   <li><b>Facade:</b> delegates the final status flip to
 *       {@link TrialService#convertTrialToSubscription(UUID)} to keep existing behavior wiring.</li>
 * </ul>
 *
 * <p>Phase 4a scope: sync MVP. Async worker (Phase 4b) will pull PAYMENT_CAPTURED rows from
 * a scheduler; for MVP the service moves straight through in the calling thread.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialToPaidService {

    private static final String POLL_URL_TEMPLATE = "/api/platform/instances/%s/trial-status";

    private final InstanceRepository instanceRepository;
    private final MigrationOutboxRepository outboxRepository;
    private final TrialToPaidConfig config;
    private final TrialService trialService;

    /**
     * UC-T2P-01 step 2-5: user clicks upgrade → validate → INITIATED → PAYMENT_PENDING.
     *
     * @param instanceId target instance
     * @param request    upgrade parameters
     * @return 202-style response with initial phase + poll URL
     * @throws MigrationException on domain errors (see error code enum)
     */
    @Transactional
    public UpgradeResponse initiateUpgrade(UUID instanceId, UpgradeRequest request) {
        log.info("Initiating upgrade for instance {} to tier {}", instanceId, request.getTier());

        Instance instance = loadInstance(instanceId);

        assertCanStartMigration(instance);
        assertWithinRescueWindowOrStillTrial(instance);

        LocalDateTime now = LocalDateTime.now();
        transitionPhase(instance, MigrationPhase.INITIATED, now);
        instance.setMigrationStartedAt(now);
        instance.setMigrationFailureReason(null);

        emitEvent(instance, MigrationEventType.TRIAL_UPGRADE_INITIATED,
            MigrationEventType.TOPIC_MIGRATION,
            String.format("{\"instanceId\":\"%s\",\"tier\":\"%s\",\"timestamp\":\"%s\"}",
                instance.getId(), request.getTier(), now));

        // Move to PAYMENT_PENDING immediately — gateway submission is non-blocking from
        // the tenant's perspective. Real gateway integration = Phase 4b.
        transitionPhase(instance, MigrationPhase.PAYMENT_PENDING, now);
        instanceRepository.save(instance);

        return UpgradeResponse.builder()
            .instanceId(instance.getId())
            .migrationPhase(instance.getMigrationPhase())
            .startedAt(now)
            .estimatedCompletionSeconds(config.getBackendP95Seconds())
            .pollUrl(String.format(POLL_URL_TEMPLATE, instance.getId()))
            .build();
    }

    /**
     * UC-T2P-01 step 6: payment-gateway capture webhook or admin force-convert entry point.
     * Advances PAYMENT_PENDING → PAYMENT_CAPTURED and emits {@code payment.captured}.
     *
     * @param instanceId target instance
     * @param txnId      gateway transaction reference
     */
    @Transactional
    public void handlePaymentCaptured(UUID instanceId, String txnId) {
        log.info("Payment captured for instance {} (txn={})", instanceId, txnId);

        Instance instance = loadInstance(instanceId);

        // Idempotent — already captured: log + skip. Webhook duplicates are common.
        if (instance.getMigrationPhase() == MigrationPhase.PAYMENT_CAPTURED) {
            log.info("Phase already PAYMENT_CAPTURED — treating webhook as duplicate");
            return;
        }

        transitionPhase(instance, MigrationPhase.PAYMENT_CAPTURED, LocalDateTime.now());

        emitEvent(instance, MigrationEventType.PAYMENT_CAPTURED,
            MigrationEventType.TOPIC_MIGRATION,
            String.format("{\"instanceId\":\"%s\",\"txnId\":\"%s\"}",
                instance.getId(), txnId == null ? "" : txnId));

        instanceRepository.save(instance);
    }

    /**
     * UC-T2P-01 step 7-9: execute the actual migration — flip TRIAL → ACTIVE atomically
     * and mark the phase COMPLETED. Delegates the status flip to the existing
     * {@link TrialService#convertTrialToSubscription(UUID)} so the current billing handoff
     * wiring stays unchanged.
     *
     * @param instanceId target instance
     */
    @Transactional
    public void executeMigration(UUID instanceId) {
        log.info("Executing migration for instance {}", instanceId);

        Instance instance = loadInstance(instanceId);

        // Can only migrate from PAYMENT_CAPTURED
        if (instance.getMigrationPhase() != MigrationPhase.PAYMENT_CAPTURED) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Cannot execute migration from phase " + instance.getMigrationPhase());
        }

        transitionPhase(instance, MigrationPhase.MIGRATING, LocalDateTime.now());
        instanceRepository.save(instance);

        try {
            // Delegate the actual TRIAL → ACTIVE flip.
            trialService.convertTrialToSubscription(instanceId);

            // Re-load to observe the post-flip state.
            Instance refreshed = loadInstance(instanceId);
            LocalDateTime completedAt = LocalDateTime.now();
            transitionPhase(refreshed, MigrationPhase.COMPLETED, completedAt);
            refreshed.setMigrationCompletedAt(completedAt);

            emitEvent(refreshed, MigrationEventType.INSTANCE_MIGRATED,
                MigrationEventType.TOPIC_MIGRATION,
                String.format("{\"instanceId\":\"%s\",\"fromStatus\":\"TRIAL\",\"toStatus\":\"ACTIVE\",\"completedAt\":\"%s\"}",
                    refreshed.getId(), completedAt));

            emitEvent(refreshed, MigrationEventType.BRANDING_REFRESH_REQUIRED,
                MigrationEventType.TOPIC_BRANDING,
                String.format("{\"instanceId\":\"%s\",\"tier\":\"%s\"}",
                    refreshed.getId(), refreshed.getTier()));

            instanceRepository.save(refreshed);
            log.info("Migration COMPLETED for instance {}", instanceId);
        } catch (Exception ex) {
            // UC-T2P-03: dead-letter on failure. Phase 4b will add retry/backoff — for MVP
            // a single failure goes straight to MIGRATION_FAILED.
            log.error("Migration failed for instance {}", instanceId, ex);
            markMigrationFailed(instanceId, ex.getMessage());
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Migration failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * UC-T2P-02: rollback a COMPLETED migration back to TRIAL within the 24h window.
     *
     * @param instanceId target instance
     * @param reason     required free-form reason (gateway reversal code, admin note)
     * @return final state after rollback
     */
    @Transactional
    public RollbackResponse rollback(UUID instanceId, String reason) {
        log.info("Rollback requested for instance {}: {}", instanceId, reason);

        Instance instance = loadInstance(instanceId);

        if (instance.getStatus() != InstanceStatus.ACTIVE) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Cannot rollback instance in status " + instance.getStatus());
        }

        if (!isWithinReversalWindow(instance)) {
            throw new MigrationException(MigrationException.Code.REVERSAL_WINDOW_EXPIRED,
                "Rollback requested beyond " + config.getReversalWindowHours() + "h window");
        }

        LocalDateTime now = LocalDateTime.now();

        transitionPhase(instance, MigrationPhase.REVERSED, now);

        // Restore status + trial expiry. Preserve the original trialExpiresAt.
        instance.setStatus(InstanceStatus.TRIAL);
        instance.setSubscriptionExpiresAt(null);
        instance.setMigrationFailureReason(reason);

        emitEvent(instance, MigrationEventType.PAYMENT_REVERSED,
            MigrationEventType.TOPIC_MIGRATION,
            String.format("{\"instanceId\":\"%s\",\"reason\":\"%s\",\"reversedAt\":\"%s\"}",
                instance.getId(), escape(reason), now));

        emitEvent(instance, MigrationEventType.MIGRATION_ROLLED_BACK,
            MigrationEventType.TOPIC_MIGRATION,
            String.format("{\"instanceId\":\"%s\",\"fromStatus\":\"ACTIVE\",\"toStatus\":\"TRIAL\",\"rolledBackAt\":\"%s\"}",
                instance.getId(), now));

        emitEvent(instance, MigrationEventType.BRANDING_REFRESH_REQUIRED,
            MigrationEventType.TOPIC_BRANDING,
            String.format("{\"instanceId\":\"%s\",\"tier\":\"%s\"}",
                instance.getId(), instance.getTier()));

        instanceRepository.save(instance);

        return RollbackResponse.builder()
            .instanceId(instance.getId())
            .migrationPhase(instance.getMigrationPhase())
            .rolledBackAt(now)
            .newStatus(instance.getStatus())
            .trialExpiresAt(instance.getTrialExpiresAt())
            .build();
    }

    // --- helpers ---------------------------------------------------------

    private Instance loadInstance(UUID instanceId) {
        return instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));
    }

    private void assertCanStartMigration(Instance instance) {
        if (instance.getMigrationPhase() == MigrationPhase.MIGRATION_FAILED) {
            throw new MigrationException(MigrationException.Code.MIGRATION_FAILED_LOCKED,
                "Instance is in MIGRATION_FAILED; manual ops action required");
        }
        if (instance.getMigrationPhase() != MigrationPhase.NONE
            && instance.getMigrationPhase() != MigrationPhase.COMPLETED
            && instance.getMigrationPhase() != MigrationPhase.REVERSED) {
            throw new MigrationException(MigrationException.Code.MIGRATION_IN_FLIGHT,
                "Another migration already in flight: " + instance.getMigrationPhase());
        }
    }

    private void assertWithinRescueWindowOrStillTrial(Instance instance) {
        if (instance.getStatus() != InstanceStatus.TRIAL) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Instance is not on TRIAL (status=" + instance.getStatus() + ")");
        }
        if (instance.getTrialExpiresAt() != null
            && LocalDateTime.now().isAfter(
                instance.getTrialExpiresAt().plusHours(config.getRescueWindowHours()))) {
            throw new MigrationException(MigrationException.Code.RESCUE_WINDOW_EXPIRED,
                "Trial expired beyond rescue window of " + config.getRescueWindowHours() + "h");
        }
    }

    /**
     * Enforce state-machine transitions from {@link MigrationPhase#canTransitionTo}.
     * Callers must always use this helper — never set {@code migrationPhase} directly.
     */
    private void transitionPhase(Instance instance, MigrationPhase target, LocalDateTime ts) {
        MigrationPhase current = instance.getMigrationPhase();
        if (current == null) {
            current = MigrationPhase.NONE;
        }
        if (!current.canTransitionTo(target)) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Illegal transition " + current + " → " + target);
        }
        instance.setMigrationPhase(target);
        if (target == MigrationPhase.INITIATED) {
            instance.setMigrationStartedAt(ts);
        }
    }

    private boolean isWithinReversalWindow(Instance instance) {
        if (instance.getMigrationCompletedAt() == null) {
            return false;
        }
        LocalDateTime deadline = instance.getMigrationCompletedAt()
            .plusHours(config.getReversalWindowHours());
        return LocalDateTime.now().isBefore(deadline);
    }

    private void markMigrationFailed(UUID instanceId, String reason) {
        Instance instance = loadInstance(instanceId);
        instance.setMigrationPhase(MigrationPhase.MIGRATION_FAILED);
        instance.setMigrationFailureReason(reason);
        // Keep status TRIAL — do NOT flip despite earlier attempt.
        if (instance.getStatus() == InstanceStatus.ACTIVE) {
            instance.setStatus(InstanceStatus.TRIAL);
        }

        emitEvent(instance, MigrationEventType.MIGRATION_FAILED,
            MigrationEventType.TOPIC_MIGRATION_DLQ,
            String.format("{\"instanceId\":\"%s\",\"failureReason\":\"%s\",\"attempts\":1}",
                instance.getId(), escape(reason)));

        instanceRepository.save(instance);
    }

    private void emitEvent(Instance instance, String eventType, String topic, String payload) {
        MigrationOutboxEvent event = MigrationOutboxEvent.builder()
            .id(UUID.randomUUID())
            .instanceId(instance.getId())
            .eventType(eventType)
            .topic(topic)
            .payload(payload)
            .createdAt(LocalDateTime.now())
            .build();
        outboxRepository.save(event);
        log.debug("Outbox event queued: {} for instance {}", eventType, instance.getId());
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
