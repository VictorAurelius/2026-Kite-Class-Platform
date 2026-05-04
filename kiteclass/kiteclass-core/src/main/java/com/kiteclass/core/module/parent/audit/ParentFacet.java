package com.kiteclass.core.module.parent.audit;

/**
 * Enumeration of parent portal facets that emit a {@link ParentReadAuditLog}
 * row when read.
 *
 * <p>Phase 1A (GAP-321) shipped {@link #TRANSCRIPT}; Wave 18b2 Bucket C
 * (GAP-321b Phase 1B foundation) extends with the four sister facets below.
 * The {@code DISCIPLINE} (kỷ luật) facet is intentionally deferred to
 * GAP-321c; do not add it here until that gap lands.
 *
 * <p>Values are mirrored in the {@code chk_parent_read_audit_facet} CHECK
 * constraint in {@code V53__add_parent_read_audit_log.sql}; updating either
 * side requires a migration + corresponding enum patch.
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
public enum ParentFacet {
    TRANSCRIPT,
    ATTENDANCE,
    FEES,
    CONDUCT,
    NOTIFICATIONS
}
