package com.kiteclass.core.module.childprotection.enums;

/**
 * Incident lifecycle status (state machine).
 *
 * <p>Phase 1A allows arbitrary transitions via service for skeleton testing.
 * Phase 1B locks transitions to:
 * <pre>
 * REPORTED → INVESTIGATING → (ESCALATED) → RESOLVED → CLOSED
 *      ↘─────────── CLOSED (false-positive) ──────────↗
 * </pre>
 *
 * <p>{@code CLOSED} entries are retained 7 years (financial-record-class
 * retention per ND-13/2023 Art 16) and CANNOT be deleted by tenant —
 * non-repudiation hash-chained audit log enforced in Phase 1B (GAP-322c).
 *
 * @since 5.x (Wave 18b1 Bucket E — GAP-322 Phase 1A)
 */
public enum IncidentStatus {

    /** Initial submission by reporter (PH/HS/GV). */
    REPORTED,

    /** Safeguarding officer has acknowledged + opened investigation. */
    INVESTIGATING,

    /**
     * Escalated to external authority (Tổng đài 111 / công an / MOLISA).
     * Phase 1B: triggers mandatory-reporting auto-suggest banner.
     */
    ESCALATED,

    /** Investigation concluded with findings + remediation actions. */
    RESOLVED,

    /**
     * Case closed (resolved or dismissed as false-positive). Remains in DB
     * with 7-year retention; cannot be deleted.
     */
    CLOSED
}
