package com.kiteclass.core.module.parent.audit;

/**
 * Records one append-only audit row per parent-side facet read.
 *
 * <p>Each facet service (attendance / fees / conduct / notifications, plus
 * existing transcript when wired) calls {@link #logRead(Long, Long,
 * ParentFacet)} after the scope guard accepts. The write is best-effort
 * (logged but never throws on failure) so an audit-store outage cannot
 * silently break the parent's experience — a decision aligned with
 * {@code logs-format-standard.md} (Phase 1B v1 only persists in DB; richer
 * fields land later).
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
public interface ParentReadAuditLogService {

    /**
     * Persists one audit row.
     *
     * @param parentId authenticated parent id (never null in production —
     *                 controllers guard before calling)
     * @param childId  child id whose data was read
     * @param facet    which facet was read
     */
    void logRead(Long parentId, Long childId, ParentFacet facet);
}
