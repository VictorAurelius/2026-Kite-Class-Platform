package com.kiteclass.core.module.student.bulkimport.entity;

/**
 * Status of a bulk-import job.
 *
 * <p>Transitions:
 * <pre>
 *   PENDING → IN_PROGRESS → COMPLETED
 *                         ↘ FAILED
 * </pre>
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
public enum BulkImportStatus {
    /** Job accepted but not started. */
    PENDING,

    /** Parsing + validating + creating rows. */
    IN_PROGRESS,

    /** All processable rows handled (some may have errored). */
    COMPLETED,

    /** Terminal failure — parsing crashed or invariant broken. */
    FAILED
}
