package com.kiteclass.core.module.instance.approval;

import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Pre-rebrand approval gate (GAP-070). Owns {@link RebrandApproval} lifecycle.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Optimistic locking: {@link #request} checks the caller-supplied
 *       {@code expectedVersion} against the current {@link FrontendInstance} {@code @Version} —
 *       stale UI clicks (the form was opened before a concurrent mutation) → {@link
 *       ConcurrentRebrandException} (maps to HTTP 409).</li>
 *   <li>Tier gating is NOT in this service — the caller (REST controller) decides whether
 *       to call {@link #request} based on tenant tier. Enterprise: required; others: skip
 *       the gate and call {@code InstanceLifecycleService.rebrand} directly. This keeps
 *       the service pattern-pure and easy to unit-test.</li>
 *   <li>Two-person rule (BR-APRV-002) enforced in {@link #approve} — approver must
 *       differ from initiator.</li>
 * </ul>
 *
 * <p>Every state transition emits an outbox event so downstream (email notifications,
 * audit trail, ops dashboard) can subscribe.
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5, GAP-070)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RebrandApprovalService {

    public static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final String AGGREGATE_TYPE = "RebrandApproval";

    private final RebrandApprovalRepository repository;
    private final FrontendInstanceRepository instanceRepository;
    private final OutboxEventWriter outbox;

    @Transactional
    public RebrandApproval request(
            Long instanceId, Long initiatorUserId, Long expectedVersion, String reason) {
        FrontendInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "FrontendInstance not found: id=" + instanceId));
        if (!instance.getVersion().equals(expectedVersion)) {
            throw new ConcurrentRebrandException(
                    "Instance version changed since you opened the rebrand form — refresh and retry");
        }

        repository.findFirstByTargetInstanceIdAndStatusAndDeletedFalse(
                        instanceId, ApprovalStatus.PENDING)
                .ifPresent(pending -> {
                    throw new ConcurrentRebrandException(
                            "A rebrand approval is already pending for instance " + instanceId
                                    + " (id=" + pending.getId() + ")");
                });

        RebrandApproval approval = RebrandApproval.builder()
                .targetInstanceId(instanceId)
                .initiatorUserId(initiatorUserId)
                .reason(reason)
                .status(ApprovalStatus.PENDING)
                .requestedAt(Instant.now())
                .expiresAt(Instant.now().plus(DEFAULT_TTL))
                .build();
        RebrandApproval saved = repository.save(approval);
        emit("rebrand.requested", saved);
        log.info("[approval] requested id={} target={} by={}",
                saved.getId(), instanceId, initiatorUserId);
        return saved;
    }

    @Transactional
    public RebrandApproval approve(Long approvalId, Long approverUserId) {
        RebrandApproval approval = load(approvalId);
        if (approverUserId == null || approverUserId.equals(approval.getInitiatorUserId())) {
            throw new ConcurrentRebrandException(
                    "Approver must be different from initiator (BR-APRV-002)");
        }
        approval.transitionTo(ApprovalStatus.APPROVED);
        approval.setApproverUserId(approverUserId);
        RebrandApproval saved = repository.save(approval);
        emit("rebrand.approved", saved);
        log.info("[approval] approved id={} by={}", approvalId, approverUserId);
        return saved;
    }

    @Transactional
    public RebrandApproval reject(Long approvalId, Long approverUserId, String rejectionReason) {
        RebrandApproval approval = load(approvalId);
        if (approverUserId == null || approverUserId.equals(approval.getInitiatorUserId())) {
            throw new ConcurrentRebrandException(
                    "Approver must be different from initiator (BR-APRV-002)");
        }
        approval.transitionTo(ApprovalStatus.REJECTED);
        approval.setApproverUserId(approverUserId);
        approval.setRejectionReason(rejectionReason);
        RebrandApproval saved = repository.save(approval);
        emit("rebrand.rejected", saved);
        log.info("[approval] rejected id={} by={} reason={}",
                approvalId, approverUserId, rejectionReason);
        return saved;
    }

    @Transactional
    public int expireDueApprovals() {
        var due = repository.findByStatusAndExpiresAtBeforeAndDeletedFalse(
                ApprovalStatus.PENDING, Instant.now());
        for (RebrandApproval approval : due) {
            approval.transitionTo(ApprovalStatus.EXPIRED);
            repository.save(approval);
            emit("rebrand.expired", approval);
        }
        if (!due.isEmpty()) {
            log.info("[approval] expired {} stale pending approvals", due.size());
        }
        return due.size();
    }

    private RebrandApproval load(Long approvalId) {
        return repository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "RebrandApproval not found: id=" + approvalId));
    }

    private void emit(String eventType, RebrandApproval approval) {
        String payload = String.format(
                "{\"approvalId\":%d,\"targetInstanceId\":%d,\"status\":\"%s\","
                        + "\"initiatorUserId\":%d,\"approverUserId\":%s}",
                approval.getId(), approval.getTargetInstanceId(), approval.getStatus().name(),
                approval.getInitiatorUserId(),
                approval.getApproverUserId() == null ? "null" : approval.getApproverUserId());
        outbox.enqueue(eventType, AGGREGATE_TYPE,
                String.valueOf(approval.getId()), payload);
    }
}
