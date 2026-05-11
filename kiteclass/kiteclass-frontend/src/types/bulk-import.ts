/**
 * Bulk Import types — mirror BE contract from
 * {@code com.kiteclass.core.module.student.bulkimport.dto.BulkImportResult}.
 *
 * GAP-137 Wave 60 Bucket B — FE consumes existing BE endpoints
 * shipped Wave 1 (GAP-051).
 *
 * @author KiteClass Team
 * @since 3.60.0
 */

/**
 * Single validation/processing error for one row of the uploaded xlsx.
 *
 * - {@code rowNumber} 1-indexed (matches spreadsheet row; header = 1, first data = 2)
 * - {@code field} field name OR {@code "row"} for row-level problems
 * - {@code message} Vietnamese error message from BE
 */
export interface RowError {
  rowNumber: number;
  field: string;
  message: string;
}

/**
 * Preview/commit response summary.
 *
 * - {@code jobId} persisted BulkImportJob id (null for preview)
 * - {@code totalRows} total data rows detected in the file
 * - {@code successCount} rows that would be (preview) or were (commit) created
 * - {@code errorCount} rows that failed validation
 * - {@code errors} first 10 errors inline (rest via error-report endpoint)
 */
export interface BulkImportResult {
  jobId: number | null;
  totalRows: number;
  successCount: number;
  errorCount: number;
  errors: RowError[];
}

/**
 * UI wizard phase. State machine (詳細設計 — design-layer-coverage.md §2.1 layer 3):
 *
 * ```
 *   idle ──pick file──> selected ──preview──> previewing
 *                                                 │
 *                                                 ├──success──> previewed ──commit──> committing
 *                                                 │                                       │
 *                                                 └──error─────> error                    ├──success──> committed
 *                                                                                         │
 *                                                                                         └──error─────> error
 * ```
 *
 * Transitions:
 * - reset(): any state → idle
 * - retry(): error → selected
 */
export type BulkImportPhase =
  | 'idle'
  | 'selected'
  | 'previewing'
  | 'previewed'
  | 'committing'
  | 'committed'
  | 'error';
