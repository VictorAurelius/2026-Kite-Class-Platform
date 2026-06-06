package com.kiteclass.core.module.tenantsettings.entity;

/**
 * Type of education organization a tenant (trường học) represents.
 *
 * <p>Drives downstream defaults (grading scale, term structure, report-card template).
 * Phase 1 BETA targets {@link #CENTER} (P2 trung tâm); {@link #K12} + {@link #UNIVERSITY}
 * are Phase 1.5+ / Phase 3 scope but enumerated now so the column is stable.
 *
 * @since Wave provisioning-1 (GAP-947)
 */
public enum SchoolType {

    /** Trung tâm (language / tutoring / skill center) — Phase 1 BETA default. */
    CENTER,

    /** Trường K-12 (mầm non → THPT). */
    K12,

    /** Trường đại học / cao đẳng. */
    UNIVERSITY,

    /** Loại hình khác chưa phân loại. */
    OTHER
}
