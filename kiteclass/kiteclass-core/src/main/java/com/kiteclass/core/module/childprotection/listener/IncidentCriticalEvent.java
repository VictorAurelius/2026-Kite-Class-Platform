package com.kiteclass.core.module.childprotection.listener;

import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;

/**
 * IncidentCriticalEvent — published when an Incident transitions to
 * {@code severity=CRITICAL} AND
 * {@code category ∈ {ABUSE, GROOMING, CSAM}}.
 *
 * <p>Per BR-CHILD-PROTECT-006 the {@link IncidentTransitionListener}
 * consumes this event and appends a hash-chain audit log entry. UI banner
 * trigger lives at the FE layer ({@code IncidentBanner.tsx}) on the same
 * condition; the audit trail is the back-end-side proof.
 *
 * @param incidentId required
 * @param severity   non-null
 * @param category   non-null
 * @param actorId    nullable (system transitions allowed)
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
public record IncidentCriticalEvent(
        Long incidentId,
        IncidentSeverity severity,
        IncidentCategory category,
        Long actorId) {

    /**
     * Human-friendly compact summary for log lines.
     */
    public String summary() {
        return "Incident#" + incidentId + " " + severity + "/" + category;
    }
}
