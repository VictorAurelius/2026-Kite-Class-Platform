package com.kiteclass.core.module.retention;

import com.kiteclass.core.common.audit.AuditLogWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Owns the {@link DeletionRequest} lifecycle for GDPR Art. 17 erasure (ADR-013, GAP-073).
 *
 * <p>Every state transition records an {@link com.kiteclass.core.common.audit.AuditLog}
 * row in the same transaction — so the audit trail and the state change commit atomically.
 *
 * <p>Scheduler entrypoint: {@link #expirePastGrace()} should be invoked by a Spring
 * {@code @Scheduled} job (wired in a future Sub-PR) to promote PENDING rows whose
 * {@code graceEndsAt} has passed into PROCESSING.
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.4, ADR-013, GAP-073)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeletionService {

    private static final String AGGREGATE_TYPE = "DeletionRequest";

    private final DeletionRequestRepository repository;
    private final AuditLogWriter auditLog;

    @Value("${retention.deletion.grace-period-days:7}")
    private long gracePeriodDays;

    /**
     * Create a PENDING deletion request for the given user+tenant with a
     * {@code graceEndsAt} set to {@code now + gracePeriodDays}. Reversible via
     * {@link #cancelDeletion(Long, String)} until the scheduler promotes it.
     *
     * @throws IllegalStateException if a non-terminal request already exists for the user
     */
    @Transactional
    public DeletionRequest requestDeletion(Long userId, UUID tenantId) {
        repository.findFirstByUserIdAndStatusAndDeletedFalse(userId, DeletionStatus.PENDING)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "A deletion request is already pending for user " + userId
                                    + " (id=" + existing.getId() + ")");
                });
        repository.findFirstByUserIdAndStatusAndDeletedFalse(userId, DeletionStatus.GRACE_PERIOD)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "A deletion request is already in grace for user " + userId
                                    + " (id=" + existing.getId() + ")");
                });

        Instant now = Instant.now();
        Instant graceEndsAt = now.plus(Duration.ofDays(gracePeriodDays));
        DeletionRequest request = DeletionRequest.builder()
                .userId(userId)
                .tenantId(tenantId)
                .status(DeletionStatus.PENDING)
                .requestedAt(now)
                .graceStartsAt(now)
                .graceEndsAt(graceEndsAt)
                .build();
        request.setInstanceId(tenantId);
        DeletionRequest saved = repository.save(request);

        auditLog.record(AuditLogWriter.AuditLogEvent.builder()
                .actionType("deletion.requested")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saved.getId()))
                .actorUserId(userId)
                .payload(renderPayload(saved))
                .build());

        log.info("[deletion] requested id={} user={} tenant={} graceEndsAt={}",
                saved.getId(), userId, tenantId, graceEndsAt);
        return saved;
    }

    /**
     * Cancel a PENDING or GRACE_PERIOD request. Terminal states throw.
     */
    @Transactional
    public DeletionRequest cancelDeletion(Long deletionId, String reason) {
        DeletionRequest request = load(deletionId);
        request.transitionTo(DeletionStatus.CANCELLED);
        request.setCancellationReason(reason);
        DeletionRequest saved = repository.save(request);

        auditLog.record(AuditLogWriter.AuditLogEvent.builder()
                .actionType("deletion.cancelled")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saved.getId()))
                .actorUserId(saved.getUserId())
                .reason(reason)
                .payload(renderPayload(saved))
                .build());

        log.info("[deletion] cancelled id={} reason={}", saved.getId(), reason);
        return saved;
    }

    /**
     * Promote a PENDING request to PROCESSING. Called by scheduler or an admin tool.
     */
    @Transactional
    public DeletionRequest startProcessing(Long deletionId) {
        DeletionRequest request = load(deletionId);
        request.transitionTo(DeletionStatus.PROCESSING);
        DeletionRequest saved = repository.save(request);

        auditLog.record(AuditLogWriter.AuditLogEvent.builder()
                .actionType("deletion.processing_started")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saved.getId()))
                .actorUserId(saved.getUserId())
                .payload(renderPayload(saved))
                .build());

        log.info("[deletion] processing started id={}", saved.getId());
        return saved;
    }

    /**
     * Mark a PROCESSING request as COMPLETED (all purge / pseudonymize actions applied).
     */
    @Transactional
    public DeletionRequest markCompleted(Long deletionId) {
        DeletionRequest request = load(deletionId);
        request.transitionTo(DeletionStatus.COMPLETED);
        DeletionRequest saved = repository.save(request);

        auditLog.record(AuditLogWriter.AuditLogEvent.builder()
                .actionType("deletion.completed")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saved.getId()))
                .actorUserId(saved.getUserId())
                .payload(renderPayload(saved))
                .build());

        log.info("[deletion] completed id={}", saved.getId());
        return saved;
    }

    /**
     * Scheduler entrypoint — promote all PENDING rows whose {@code graceEndsAt} has
     * passed to PROCESSING. Returns the count of transitioned rows.
     */
    @Transactional
    public int expirePastGrace() {
        List<DeletionRequest> due = repository.findByStatusAndGraceEndsAtBeforeAndDeletedFalse(
                DeletionStatus.PENDING, Instant.now());
        for (DeletionRequest request : due) {
            request.transitionTo(DeletionStatus.PROCESSING);
            repository.save(request);
            auditLog.record(AuditLogWriter.AuditLogEvent.builder()
                    .actionType("deletion.processing_started")
                    .aggregateType(AGGREGATE_TYPE)
                    .aggregateId(String.valueOf(request.getId()))
                    .actorUserId(request.getUserId())
                    .reason("grace period expired")
                    .payload(renderPayload(request))
                    .build());
        }
        if (!due.isEmpty()) {
            log.info("[deletion] promoted {} grace-expired requests to PROCESSING", due.size());
        }
        return due.size();
    }

    private DeletionRequest load(Long deletionId) {
        return repository.findById(deletionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "DeletionRequest not found: id=" + deletionId));
    }

    private static String renderPayload(DeletionRequest r) {
        return String.format(
                "{\"deletionId\":%d,\"userId\":%d,\"tenantId\":\"%s\",\"status\":\"%s\"}",
                r.getId(), r.getUserId(), r.getTenantId(), r.getStatus().name());
    }
}
