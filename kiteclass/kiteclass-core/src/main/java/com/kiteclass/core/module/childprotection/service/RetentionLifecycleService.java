package com.kiteclass.core.module.childprotection.service;

/**
 * RetentionLifecycleService — daily lifecycle cron that secure-deletes
 * child-protection incidents whose 7-year retention window
 * ({@code retention_until}) has expired.
 *
 * <p>Per BR-CHILD-PROTECT-008 (Phase 1C v1.5, GAP-359 sub-task 359.1):
 * <ol>
 *   <li>{@code IncidentService.softDelete} BLOCKS while
 *       {@code retention_until &gt; now}.</li>
 *   <li>This cron runs daily at 02:00 to find incidents past retention,
 *       secure-deletes them (mark deleted + null-out sensitive fields), and
 *       appends an audit-log entry for compliance traceability.</li>
 * </ol>
 *
 * <p>Compliance: PDPL Decree 13/2023/NĐ-CP Art 16 + Luật Trẻ em 2016 Đ.51
 * follow-through; the audit-log entry establishes the chain of custody for
 * the lifecycle action.
 *
 * @since 5.x (Wave 24 Bucket A — GAP-359 sub-task 359.1)
 */
public interface RetentionLifecycleService {

    /**
     * Run a single retention sweep — process all incidents whose
     * {@code retention_until} is past {@code now}.
     *
     * <p>Returns the count of rows processed (deleted + audit-logged) so
     * callers / tests can assert behaviour deterministically.
     *
     * @return number of incidents secure-deleted in this run
     */
    int sweepExpiredIncidents();
}
