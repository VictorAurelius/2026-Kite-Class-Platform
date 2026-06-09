/**
 * Enrollment bulk-import types — mirror BE contract from
 * {@code com.kiteclass.core.module.enrollment.bulkimport.dto.EnrollmentBulkResult}
 * (GAP-1104). Reuses the {@code RowError} shape shared with student bulk-import.
 *
 * @author KiteClass Team
 * @since 3.x (Wave KC enrollment)
 */

/**
 * Single validation/processing error for one bulk-enroll row.
 *
 * - {@code rowNumber} 1-indexed (header = 1, first data row = 2)
 * - {@code field} column name (e.g. {@code class_code}) OR {@code "row"} for
 *   row-level problems (in-file duplicate, business-rule failure)
 * - {@code message} Vietnamese error message from BE
 */
export interface EnrollmentRowError {
  rowNumber: number;
  field: string;
  message: string;
}

/**
 * Preview/commit response summary. Unlike student bulk-import there is no
 * {@code jobId} — each row delegates to the single-enroll transaction.
 *
 * - {@code totalRows} total data rows detected in the file
 * - {@code successCount} rows that would be (preview) or were (commit) enrolled
 * - {@code errorCount} rows that failed resolution/validation/business rules
 * - {@code errors} first 10 errors inline
 */
export interface EnrollmentBulkResult {
  totalRows: number;
  successCount: number;
  errorCount: number;
  errors: EnrollmentRowError[];
}

/**
 * UI wizard phase for the bulk-enroll screen.
 *
 * ```
 *   idle ──pick file──> selected ──preview──> previewing
 *                                                 ├──success──> previewed ──commit──> committing ──success──> committed
 *                                                 └──error─────> error
 * ```
 */
export type EnrollmentBulkPhase =
  | 'idle'
  | 'selected'
  | 'previewing'
  | 'previewed'
  | 'committing'
  | 'committed'
  | 'error';
