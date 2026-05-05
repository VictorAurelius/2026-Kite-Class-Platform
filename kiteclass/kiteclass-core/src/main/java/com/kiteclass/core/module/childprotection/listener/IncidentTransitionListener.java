package com.kiteclass.core.module.childprotection.listener;

import com.kiteclass.core.module.childprotection.service.ChildProtectionAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Spring {@link TransactionalEventListener} that fires after an
 * {@link IncidentCriticalEvent} is published — i.e. the originating
 * transaction has committed. Appends a hash-chain audit log entry per
 * BR-CHILD-PROTECT-006 (Đ.51 mandatory reporting trigger) and
 * BR-CHILD-PROTECT-007 (audit log).
 *
 * <p>Phase 1C v1 wiring: the listener runs on
 * {@link TransactionPhase#AFTER_COMMIT} so the audit chain only grows
 * with actually-persisted incidents. Failure to append is logged but
 * NOT re-thrown — the source transaction has committed and we never want
 * to roll back a successful incident persist because of an audit-log hiccup.
 *
 * <p>Out-of-scope for v1 (deferred to Phase 1C remainder follow-up gap):
 * <ul>
 *   <li>Outbound notification to safeguarding officer (in-app + email)</li>
 *   <li>Tổng đài 111 webhook delivery</li>
 *   <li>Daily hash-chain integrity verification cron</li>
 * </ul>
 *
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IncidentTransitionListener {

    /**
     * Audit-log entity-type constant. Kept here (and not on
     * {@code Incident}) so non-listener call sites can reuse it without
     * pulling in the entity class.
     */
    public static final String INCIDENT_ENTITY_TYPE = "Incident";

    /**
     * Audit action recorded on a CRITICAL+abuse-category transition.
     */
    public static final String ACTION_CRITICAL_TRANSITION = "INCIDENT_TRANSITION_CRITICAL";

    private final ChildProtectionAuditService auditService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIncidentCritical(IncidentCriticalEvent event) {
        try {
            auditService.append(
                    INCIDENT_ENTITY_TYPE,
                    event.incidentId(),
                    ACTION_CRITICAL_TRANSITION,
                    event.actorId(),
                    Map.of(
                            "severity", event.severity().name(),
                            "category", event.category().name()));
            log.info("Audit-logged CRITICAL transition: {}", event.summary());
        } catch (RuntimeException ex) {
            // Source transaction already committed — never propagate.
            log.error("Failed to append audit log for {}: {}",
                    event.summary(), ex.getMessage(), ex);
        }
    }
}
