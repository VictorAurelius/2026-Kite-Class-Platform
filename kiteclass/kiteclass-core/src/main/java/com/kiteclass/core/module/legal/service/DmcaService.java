package com.kiteclass.core.module.legal.service;

import com.kiteclass.core.common.audit.AuditLogWriter;
import com.kiteclass.core.common.audit.AuditLogWriter.AuditLogEvent;
import com.kiteclass.core.module.legal.entity.DmcaStatus;
import com.kiteclass.core.module.legal.entity.DmcaTakedownRequest;
import com.kiteclass.core.module.legal.repository.DmcaTakedownRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns {@link DmcaTakedownRequest} lifecycle (ADR-012 Track 2).
 *
 * <p>Design decisions:
 * <ul>
 *   <li><b>State Pattern</b> — transitions delegated to
 *       {@link DmcaTakedownRequest#transitionTo(DmcaStatus)}. Controllers MUST NOT mutate status
 *       directly.</li>
 *   <li><b>Audit every transition</b> — each @Transactional method writes one
 *       {@link AuditLogEvent} row so the {@code audit_log} table is the single source of truth
 *       for DMCA paper trail (§512 safe-harbor requirement).</li>
 *   <li><b>Asset flagging / revert are stubs</b> — logging-only for this sub-PR. Real branding
 *       asset revert wires in during Wave 4 follow-up; counter-notice email dispatch is
 *       deferred (see javadoc on {@link #contest}).</li>
 * </ul>
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.3, GAP-042)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DmcaService {

    private static final String AGGREGATE_TYPE = "DmcaTakedownRequest";

    private final DmcaTakedownRepository repository;
    private final AuditLogWriter auditLog;

    /**
     * Public intake — anyone can submit a takedown notice. Creates a PENDING row and writes
     * the first audit entry.
     */
    @Transactional
    public DmcaTakedownRequest receiveTakedown(DmcaTakedownRequest request) {
        request.setStatus(DmcaStatus.PENDING);
        DmcaTakedownRequest saved = repository.save(request);
        auditLog.record(AuditLogEvent.builder()
                .actionType("dmca.takedown.received")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saved.getId()))
                .reason("DMCA notice received from " + saved.getReporterEmail())
                .build());
        log.info("[dmca] received id={} reporter={}", saved.getId(), saved.getReporterEmail());
        return saved;
    }

    /** Admin picks up a PENDING notice to review. PENDING → REVIEWING. */
    @Transactional
    public DmcaTakedownRequest markReviewing(Long id, Long reviewerUserId) {
        DmcaTakedownRequest request = load(id);
        request.transitionTo(DmcaStatus.REVIEWING);
        request.setReviewerUserId(reviewerUserId);
        DmcaTakedownRequest saved = repository.save(request);
        auditLog.record(AuditLogEvent.builder()
                .actionType("dmca.takedown.reviewing")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saved.getId()))
                .actorUserId(reviewerUserId)
                .build());
        log.info("[dmca] reviewing id={} reviewer={}", saved.getId(), reviewerUserId);
        return saved;
    }

    /**
     * Notice deemed legitimate. REVIEWING → VALID. Affected branding asset SHOULD be flagged for
     * revert — intentionally a log-only stub here; actual asset flagging integration is deferred
     * until Wave 4 follow-up.
     */
    @Transactional
    public DmcaTakedownRequest markValid(Long id, Long reviewerUserId) {
        DmcaTakedownRequest request = load(id);
        request.transitionTo(DmcaStatus.VALID);
        request.setReviewerUserId(reviewerUserId);
        DmcaTakedownRequest saved = repository.save(request);
        auditLog.record(AuditLogEvent.builder()
                .actionType("dmca.takedown.valid")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saved.getId()))
                .actorUserId(reviewerUserId)
                .reason("Notice deemed legitimate; asset flagged for revert")
                .build());
        // STUB — asset flagging integration deferred.
        log.warn("[dmca] VALID id={} — asset flagging stub (url={})",
                saved.getId(), saved.getAllegedInfringingUrl());
        return saved;
    }

    /** Notice rejected as frivolous/invalid. REVIEWING → INVALID (terminal). */
    @Transactional
    public DmcaTakedownRequest markInvalid(Long id, Long reviewerUserId, String reason) {
        DmcaTakedownRequest request = load(id);
        request.transitionTo(DmcaStatus.INVALID);
        request.setReviewerUserId(reviewerUserId);
        request.setRejectionReason(reason);
        DmcaTakedownRequest saved = repository.save(request);
        auditLog.record(AuditLogEvent.builder()
                .actionType("dmca.takedown.invalid")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saved.getId()))
                .actorUserId(reviewerUserId)
                .reason(reason)
                .build());
        log.info("[dmca] INVALID id={} by={} reason={}", saved.getId(), reviewerUserId, reason);
        return saved;
    }

    /**
     * VALID → EXECUTED. Should actually revert the branding asset to TEMPLATE — stubbed as a
     * log entry here pending branding-module integration.
     */
    @Transactional
    public DmcaTakedownRequest execute(Long id) {
        DmcaTakedownRequest request = load(id);
        request.transitionTo(DmcaStatus.EXECUTED);
        DmcaTakedownRequest saved = repository.save(request);
        auditLog.record(AuditLogEvent.builder()
                .actionType("dmca.takedown.executed")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saved.getId()))
                .reason("Asset revert executed")
                .build());
        // STUB — real branding asset revert deferred.
        log.warn("[dmca] EXECUTED id={} — branding asset revert stub (url={})",
                saved.getId(), saved.getAllegedInfringingUrl());
        return saved;
    }

    /**
     * Counter-notice received from affected tenant. VALID → CONTESTED.
     *
     * <p>Counter-notice email dispatch to the original reporter is deferred — this method only
     * records the counter-notice address. Operations team notifies reporter manually for now.
     */
    @Transactional
    public DmcaTakedownRequest contest(Long id, String counterNoticeEmail) {
        DmcaTakedownRequest request = load(id);
        request.transitionTo(DmcaStatus.CONTESTED);
        request.setCounterNoticeEmail(counterNoticeEmail);
        DmcaTakedownRequest saved = repository.save(request);
        auditLog.record(AuditLogEvent.builder()
                .actionType("dmca.takedown.contested")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saved.getId()))
                .reason("Counter-notice received from " + counterNoticeEmail)
                .build());
        log.info("[dmca] CONTESTED id={} counterEmail={}", saved.getId(), counterNoticeEmail);
        return saved;
    }

    private DmcaTakedownRequest load(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "DmcaTakedownRequest not found: id=" + id));
    }
}
