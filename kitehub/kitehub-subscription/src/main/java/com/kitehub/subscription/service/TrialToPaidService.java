package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.TrialToPaidConfig;
import com.kitehub.subscription.dto.RollbackResponse;
import com.kitehub.subscription.dto.UpgradeRequest;
import com.kitehub.subscription.dto.UpgradeResponse;
import com.kitehub.subscription.exception.MigrationException;
import com.kitehub.subscription.idempotency.MigrationIdempotencyKeyService;
import com.kitehub.subscription.outbox.MigrationEventType;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.migration.MigrationRetryRunner;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import com.kitehub.subscription.service.migration.MigrationStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
 *       {@link TrialService#convertTrialToSubscription(UUID, PricingTier)} to keep existing behavior wiring.</li>
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
public class TrialToPaidService {

    private static final String POLL_URL_TEMPLATE = "/api/platform/instances/%s/trial-status";

    private final InstanceRepository instanceRepository;
    private final TrialToPaidConfig config;
    private final TrialService trialService;
    private final MigrationIdempotencyKeyService idempotencyService;
    private final MigrationStateMachine stateMachine;
    private final SubscriptionEventEmitter eventEmitter;
    private final MigrationRetryRunner retryRunner;
    private final InstanceTierSyncService tierSyncService;

    public TrialToPaidService(InstanceRepository instanceRepository,
                              SubscriptionEventEmitter eventEmitter,
                              TrialToPaidConfig config,
                              TrialService trialService,
                              MigrationIdempotencyKeyService idempotencyService,
                              InstanceTierSyncService tierSyncService,
                              MigrationRetryRunner retryRunner) {
        this.instanceRepository = instanceRepository;
        this.config = config;
        this.trialService = trialService;
        this.idempotencyService = idempotencyService;
        this.stateMachine = new MigrationStateMachine(config);
        this.eventEmitter = eventEmitter;
        this.tierSyncService = tierSyncService;
        // GAP-1254 — MigrationRetryRunner is now a Spring bean (injected) so its
        // @Transactional per-attempt methods run through the proxy instead of inert
        // self-invocation on a hand-built instance.
        this.retryRunner = retryRunner;
    }

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

        // GAP-192 Phase 4b-i — idempotency-key short-circuit. Duplicate retries from the
        // same client within 10 minutes get the original 202 envelope back (see
        // api-contract.md: "duplicate request within 10 minutes returns original 202").
        if (idempotencyService != null && request.getIdempotencyKey() != null
            && !request.getIdempotencyKey().isBlank()) {
            Optional<UpgradeResponse> cached =
                idempotencyService.findExisting(request.getIdempotencyKey(), instanceId);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        // GAP-1253 (T2P-08) — pessimistic-write lock so two concurrent upgrade initiations
        // can't both pass the can-start guard and both start a migration.
        Instance instance = loadInstanceForUpdate(instanceId);

        stateMachine.assertCanStartMigration(instance);
        stateMachine.assertWithinRescueWindowOrStillTrial(instance);

        LocalDateTime now = LocalDateTime.now();
        stateMachine.transitionPhase(instance, MigrationPhase.INITIATED, now);
        instance.setMigrationStartedAt(now);
        instance.setMigrationFailureReason(null);

        // GAP-1095 — persist the requested paid tier through the canonical SUB-21 sync point
        // now so it is carried to the (async, request-less) completion step. The trial
        // registered as FREE; without this carry the completion flip would leave the paid
        // instance on FREE. Rollback (GAP-1256) resets it to FREE if the migration reverses.
        if (request.getTier() != null) {
            tierSyncService.syncInstanceTier(instance, request.getTier());
        }

        eventEmitter.emit(instance, MigrationEventType.TRIAL_UPGRADE_INITIATED,
            MigrationEventType.TOPIC_MIGRATION,
            String.format("{\"instanceId\":\"%s\",\"tier\":\"%s\",\"timestamp\":\"%s\"}",
                instance.getId(), request.getTier(), now));

        // Move to PAYMENT_PENDING immediately — gateway submission is non-blocking from
        // the tenant's perspective. Real gateway integration = Phase 4b.
        stateMachine.transitionPhase(instance, MigrationPhase.PAYMENT_PENDING, now);
        instanceRepository.save(instance);

        UpgradeResponse response = UpgradeResponse.builder()
            .instanceId(instance.getId())
            .migrationPhase(instance.getMigrationPhase())
            .startedAt(now)
            .estimatedCompletionSeconds(config.getBackendP95Seconds())
            .pollUrl(String.format(POLL_URL_TEMPLATE, instance.getId()))
            .build();

        // Persist idempotency row inside the same txn so duplicate client retries see it.
        if (idempotencyService != null) {
            idempotencyService.persist(request.getIdempotencyKey(), response);
        }

        return response;
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

        Instance instance = loadInstanceForUpdate(instanceId);

        // Idempotent — already captured: log + skip. Webhook duplicates are common.
        if (instance.getMigrationPhase() == MigrationPhase.PAYMENT_CAPTURED) {
            log.info("Phase already PAYMENT_CAPTURED — treating webhook as duplicate");
            return;
        }

        stateMachine.transitionPhase(instance, MigrationPhase.PAYMENT_CAPTURED, LocalDateTime.now());

        eventEmitter.emit(instance, MigrationEventType.PAYMENT_CAPTURED,
            MigrationEventType.TOPIC_MIGRATION,
            String.format("{\"instanceId\":\"%s\",\"txnId\":\"%s\"}",
                instance.getId(), txnId == null ? "" : txnId));

        instanceRepository.save(instance);
    }

    /**
     * UC-T2P-01 step 7-9: execute the actual migration — flip TRIAL → ACTIVE atomically
     * and mark the phase COMPLETED. Delegates the status flip to the existing
     * {@link TrialService#convertTrialToSubscription(UUID, PricingTier)} so the current billing handoff
     * wiring stays unchanged.
     *
     * @param instanceId target instance
     */
    @Transactional
    @SuppressWarnings("deprecation")  // intentional delegation to TrialService legacy flip during Phase 4b-i transition
    public void executeMigration(UUID instanceId) {
        log.info("Executing migration for instance {}", instanceId);

        Instance instance = loadInstanceForUpdate(instanceId);

        // Can only migrate from PAYMENT_CAPTURED
        if (instance.getMigrationPhase() != MigrationPhase.PAYMENT_CAPTURED) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Cannot execute migration from phase " + instance.getMigrationPhase());
        }

        stateMachine.transitionPhase(instance, MigrationPhase.MIGRATING, LocalDateTime.now());
        instanceRepository.save(instance);

        try {
            // Delegate the actual TRIAL → ACTIVE flip, carrying the requested paid tier
            // (persisted at initiateUpgrade) so instances.tier is synced, not left FREE (GAP-1095).
            trialService.convertTrialToSubscription(instanceId, instance.getTier());

            // Re-load to observe the post-flip state (lock already held within this txn).
            Instance refreshed = loadInstanceForUpdate(instanceId);
            LocalDateTime completedAt = LocalDateTime.now();
            stateMachine.transitionPhase(refreshed, MigrationPhase.COMPLETED, completedAt);
            refreshed.setMigrationCompletedAt(completedAt);

            eventEmitter.emit(refreshed, MigrationEventType.INSTANCE_MIGRATED,
                MigrationEventType.TOPIC_MIGRATION,
                String.format("{\"instanceId\":\"%s\",\"fromStatus\":\"TRIAL\",\"toStatus\":\"ACTIVE\",\"completedAt\":\"%s\"}",
                    refreshed.getId(), completedAt));

            eventEmitter.emit(refreshed, MigrationEventType.BRANDING_REFRESH_REQUIRED,
                MigrationEventType.TOPIC_BRANDING,
                String.format("{\"instanceId\":\"%s\",\"tier\":\"%s\"}",
                    refreshed.getId(), refreshed.getTier()));

            instanceRepository.save(refreshed);
            log.info("Migration COMPLETED for instance {}", instanceId);
        } catch (Exception ex) {
            // UC-T2P-03: dead-letter on failure. The retry runner owns the canonical
            // markMigrationFailed (REQUIRES_NEW) so the MIGRATION_FAILED marking survives
            // this method's rollback (GAP-1254).
            log.error("Migration failed for instance {}", instanceId, ex);
            retryRunner.markMigrationFailed(instanceId, ex.getMessage());
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

        Instance instance = loadInstanceForUpdate(instanceId);

        if (instance.getStatus() != InstanceStatus.ACTIVE) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Cannot rollback instance in status " + instance.getStatus());
        }

        if (!stateMachine.isWithinReversalWindow(instance)) {
            throw new MigrationException(MigrationException.Code.REVERSAL_WINDOW_EXPIRED,
                "Rollback requested beyond " + config.getReversalWindowHours() + "h window");
        }

        LocalDateTime now = LocalDateTime.now();

        stateMachine.transitionPhase(instance, MigrationPhase.REVERSED, now);

        // Restore status + trial expiry. Preserve the original trialExpiresAt.
        instance.setStatus(InstanceStatus.TRIAL);
        instance.setSubscriptionExpiresAt(null);
        instance.setMigrationFailureReason(reason);

        // GAP-1256 — reset the denormalized tier to the pre-migration FREE trial tier through
        // the canonical SUB-21 sync point. Without this a reverted non-payer kept the paid
        // tier (PREMIUM/BASIC) that GAP-1095 set at initiate — connection-pool size,
        // custom-domain eligibility, and data-retention all read instances.tier.
        tierSyncService.syncInstanceTier(instance, PricingTier.FREE);

        eventEmitter.emit(instance, MigrationEventType.PAYMENT_REVERSED,
            MigrationEventType.TOPIC_MIGRATION,
            String.format("{\"instanceId\":\"%s\",\"reason\":\"%s\",\"reversedAt\":\"%s\"}",
                instance.getId(), SubscriptionEventEmitter.escape(reason), now));

        eventEmitter.emit(instance, MigrationEventType.MIGRATION_ROLLED_BACK,
            MigrationEventType.TOPIC_MIGRATION,
            String.format("{\"instanceId\":\"%s\",\"fromStatus\":\"ACTIVE\",\"toStatus\":\"TRIAL\",\"rolledBackAt\":\"%s\"}",
                instance.getId(), now));

        eventEmitter.emit(instance, MigrationEventType.BRANDING_REFRESH_REQUIRED,
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

    /**
     * UC-T2P-05 — admin force-convert. Bypasses gateway capture: runs
     * INITIATED → PAYMENT_PENDING → PAYMENT_CAPTURED all inside one transaction,
     * tagged {@code manual=true} on the capture event so downstream reconciliation
     * knows the payment happened out-of-band. The {@code MigrationScheduler} picks
     * up the resulting PAYMENT_CAPTURED instance on the next tick.
     *
     * @param instanceId  target instance
     * @param invoiceRef  accounting ref — persisted on the capture event
     * @param auditReason free-form note from ops
     * @return the initial 202 envelope (same shape as {@link #initiateUpgrade})
     */
    @Transactional
    public UpgradeResponse forceConvert(UUID instanceId, UpgradeRequest request,
                                        String invoiceRef, String auditReason) {
        log.info("Admin force-convert for instance {} tier={} invoice={}",
            instanceId, request.getTier(), invoiceRef);

        UpgradeResponse response = initiateUpgrade(instanceId, request);

        // Immediately short-circuit PAYMENT_PENDING → PAYMENT_CAPTURED with a manual tag.
        Instance instance = loadInstanceForUpdate(instanceId);
        stateMachine.transitionPhase(instance, MigrationPhase.PAYMENT_CAPTURED, LocalDateTime.now());
        eventEmitter.emit(instance, MigrationEventType.PAYMENT_CAPTURED,
            MigrationEventType.TOPIC_MIGRATION,
            String.format("{\"instanceId\":\"%s\",\"manual\":true,\"invoiceRef\":\"%s\",\"reason\":\"%s\"}",
                instance.getId(), SubscriptionEventEmitter.escape(invoiceRef), SubscriptionEventEmitter.escape(auditReason)));
        instanceRepository.save(instance);

        return response.toBuilder()
            .migrationPhase(MigrationPhase.PAYMENT_CAPTURED)
            .build();
    }

    /**
     * Retry-wrapped migration (T2P-09): up to {@code maxAttempts} attempts with the
     * backoff schedule from {@link TrialToPaidConfig#getRetryBackoffSeconds()}. On
     * exhaustion the instance is marked {@link MigrationPhase#MIGRATION_FAILED} (already
     * done inside {@link #executeMigration}) and a {@link MigrationException} is thrown
     * to the caller — the scheduler logs + moves on.
     *
     * <p>Backoff is a simple in-thread sleep — acceptable for the MVP worker (single
     * instance, low concurrency). A transactional retry library (Resilience4j / Spring
     * Retry) can be dropped in later without changing the surface.</p>
     */
    public void executeMigrationWithRetry(UUID instanceId) {
        retryRunner.executeMigrationWithRetry(instanceId);
    }

    /**
     * Scheduler hook — returns all instances currently in {@code PAYMENT_CAPTURED}
     * waiting for the async worker to pick them up.
     */
    @Transactional(readOnly = true)
    public List<Instance> findInstancesReadyForMigration() {
        return instanceRepository.findByMigrationPhase(MigrationPhase.PAYMENT_CAPTURED);
    }

    /**
     * Webhook entry for payment reversal (UC-T2P-02, webhook variant). Mirrors
     * {@link #rollback(UUID, String)} but is idempotent for duplicate gateway
     * deliveries — if the phase is already REVERSED, no-op.
     */
    @Transactional
    public Optional<RollbackResponse> handlePaymentReversed(UUID instanceId, String reason) {
        Instance instance = loadInstanceForUpdate(instanceId);
        if (instance.getMigrationPhase() == MigrationPhase.REVERSED) {
            log.info("Instance {} already REVERSED — ignoring duplicate webhook", instanceId);
            return Optional.empty();
        }
        return Optional.of(rollback(instanceId, reason));
    }

    // --- helpers ---------------------------------------------------------

    /**
     * Pessimistic-write load for every mutating path (GAP-1253, T2P-08). Serializes
     * concurrent migrations on the same instance so two threads can't both pass a
     * guard then race the state transition. Read-only scans use repository finders directly.
     */
    private Instance loadInstanceForUpdate(UUID instanceId) {
        return instanceRepository.findByIdForUpdate(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));
    }

}
