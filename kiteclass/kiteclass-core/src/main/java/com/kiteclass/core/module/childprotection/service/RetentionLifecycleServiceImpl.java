package com.kiteclass.core.module.childprotection.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.repository.IncidentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default {@link RetentionLifecycleService} implementation.
 *
 * <p>Runs once per day at 02:00 by default; the {@link AuditChainVerificationCron}
 * runs at 02:30 so retention-driven appends from this job land in the chain
 * BEFORE integrity verification reads it.
 *
 * @since Wave 24 Bucket A — GAP-359 sub-task 359.1
 */
@Service
@Slf4j
public class RetentionLifecycleServiceImpl implements RetentionLifecycleService {

    /**
     * Audit action recorded against every secure-delete this cron performs.
     * Distinct from operator-initiated soft-delete actions so forensic
     * timelines surface lifecycle vs human deletes separately.
     */
    public static final String ACTION_RETENTION_EXPIRED_DELETE =
            "INCIDENT_RETENTION_EXPIRED_DELETE";

    private static final String ENTITY_TYPE_INCIDENT = "Incident";

    private final IncidentRepository incidentRepository;
    private final ChildProtectionAuditService auditService;

    public RetentionLifecycleServiceImpl(
            IncidentRepository incidentRepository,
            ChildProtectionAuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
    }

    /**
     * Daily cron entry point. Cron expression {@code "0 0 2 * * *"} = 02:00
     * every day. Configurable via Spring property {@code childprotection.retention.cron}
     * if dev/staging needs a faster cadence; production keeps the default.
     */
    @Scheduled(cron = "${childprotection.retention.cron:0 0 2 * * *}")
    public void scheduledSweep() {
        try {
            int processed = sweepExpiredIncidents();
            log.info("Retention lifecycle sweep complete: {} incidents secure-deleted", processed);
        } catch (Exception ex) {
            // Cron must NEVER propagate — rely on logging + Micrometer alerting.
            log.error("Retention lifecycle sweep failed", ex);
        }
    }

    @Override
    @Transactional
    public int sweepExpiredIncidents() {
        Instant now = Instant.now();
        List<Incident> expired = incidentRepository.findExpiredRetention(now);
        if (expired.isEmpty()) {
            log.debug("Retention sweep at {}: no expired incidents", now);
            return 0;
        }

        int processed = 0;
        for (Incident incident : expired) {
            try {
                secureDelete(incident);
                processed++;
            } catch (Exception ex) {
                // Per-row isolation — one tenant's audit append failure must
                // not abort the whole sweep. Logged + counted as not-processed.
                log.error("Retention sweep failed for incident id={}",
                        incident.getId(), ex);
            }
        }
        return processed;
    }

    /**
     * Mark deleted + null-out sensitive fields + append audit entry.
     *
     * <p>Audit append requires a {@link TenantContext} — populate from the
     * incident's own {@code instanceId}, then clear the context to avoid
     * leaking state into the next loop iteration.
     */
    private void secureDelete(Incident incident) {
        UUID tenantId = incident.getInstanceId();
        Long incidentId = incident.getId();
        Instant retentionDeadline = incident.getRetentionUntil();

        // Capture audit payload BEFORE we mutate the row — we want the
        // payload to record final-state metadata + the deadline that fired.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("severity", String.valueOf(incident.getSeverity()));
        payload.put("category", String.valueOf(incident.getCategory()));
        payload.put("status", String.valueOf(incident.getStatus()));
        payload.put("retentionUntil",
                retentionDeadline == null ? null : retentionDeadline.toString());
        payload.put("trigger", "scheduled");

        // Mutate: soft-delete + null-out sensitive fields. Title stays
        // (non-sensitive plaintext per BR-CHILD-PROT-005) so audit list
        // surfaces still have a label after secure-delete.
        incident.markAsDeleted();
        incident.setDescription(null);
        incident.setEvidencePaths(null);
        incidentRepository.save(incident);

        // Audit append runs in a TenantContext set to the incident's own
        // instance — the cron itself is system-wide.
        boolean previouslySet = TenantContext.isSet();
        UUID previous = previouslySet ? TenantContext.getCurrentTenant() : null;
        try {
            TenantContext.setCurrentTenant(tenantId);
            auditService.append(
                    ENTITY_TYPE_INCIDENT,
                    incidentId,
                    ACTION_RETENTION_EXPIRED_DELETE,
                    null,
                    payload);
        } finally {
            if (previouslySet) {
                TenantContext.setCurrentTenant(previous);
            } else {
                TenantContext.clear();
            }
        }

        log.info("Incident id={} secure-deleted (retention expired at {})",
                incidentId, retentionDeadline);
    }
}
