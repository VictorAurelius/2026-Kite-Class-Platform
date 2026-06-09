package com.kiteclass.core.module.enrollment.bulkimport.dto;

import com.kiteclass.core.module.student.bulkimport.dto.RowError;

import java.util.List;

/**
 * Summary returned to the client after a bulk-enroll preview or commit call.
 *
 * <p>Reuses {@link RowError} (student bulk-import) so the per-row error shape is
 * identical across both bulk-import features and the FE can share its error
 * rendering. For commit responses, only the first {@value #MAX_RETURNED_ERRORS}
 * errors are embedded inline.
 *
 * <p>Unlike the student bulk-import, bulk-enroll does NOT persist a job row — it
 * delegates each row to the existing {@code EnrollmentService.enrollStudent}
 * transaction (skip-and-report per row), so there is no {@code jobId}.
 *
 * @param totalRows    total data rows detected in the file
 * @param successCount rows that would be (preview) or were (commit) enrolled
 * @param errorCount   rows that failed validation, resolution, or business rules
 * @param errors       first {@value #MAX_RETURNED_ERRORS} errors (inline)
 * @author KiteClass Team
 * @since 2.7.0
 */
public record EnrollmentBulkResult(
        int totalRows,
        int successCount,
        int errorCount,
        List<RowError> errors
) {

    /** Maximum errors returned inline in the response body. */
    public static final int MAX_RETURNED_ERRORS = 10;
}
