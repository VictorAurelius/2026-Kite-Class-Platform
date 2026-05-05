package com.kiteclass.core.module.childprotection.enums;

/**
 * IncidentVisibilityScope — controls which audiences may see an
 * {@link com.kiteclass.core.module.childprotection.entity.Incident} record.
 *
 * <p>Per BR-CHILD-PROTECT-005 (Phase 1C, GAP-322c, Wave 19 Bucket A) the
 * default for all newly-created and legacy incidents is {@link #STAFF_ONLY}.
 * The value is consumed by:
 * <ul>
 *   <li>Parent-portal conduct facet (Wave 18b3 follow-up
 *       {@code GAP-321b-1-conduct}) — only {@code PARENT_VISIBLE} +
 *       {@code PUBLIC} appear to parents</li>
 *   <li>Future state-machine + audit log filters (Phase 1C)</li>
 * </ul>
 *
 * <p>This enum is intentionally narrow (4 values) — broader audience
 * targeting (per-grade, per-homeroom) is deferred and would require a join
 * table rather than a single enum column.
 *
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
public enum IncidentVisibilityScope {

    /**
     * Visible to the subject student's parent(s) via the parent portal
     * conduct facet. Most BULLYING / OTHER cases that have been resolved
     * with the family typically end up here.
     */
    PARENT_VISIBLE,

    /**
     * Visible to all tenant users (anonymized broadcasts, school-wide
     * incident summaries). Rarely used; reserved for Phase 1C+ broadcast
     * channels.
     */
    PUBLIC,

    /**
     * <b>Default.</b> Visible only to safeguarding-officer / Hiệu trưởng /
     * counselor roles. Critical / abuse / grooming records default here so
     * that they are NOT exposed via the parent-portal conduct facet.
     */
    STAFF_ONLY,

    /**
     * Restricted to a named subset of safeguarding officers (e.g. when an
     * incident involves another staff member and ordinary officers must be
     * excluded). Phase 1C v1 persists the value but does not yet enforce
     * the named-subset filter — that ships with the audit-log RBAC layer.
     */
    RESTRICTED
}
