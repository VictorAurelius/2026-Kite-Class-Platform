package com.kiteclass.core.module.student.bulkimport.dto;

import java.util.List;

/**
 * Summary returned to the client after a preview or commit call.
 *
 * <p>For commit responses, only the first {@value #MAX_RETURNED_ERRORS} errors
 * are embedded — the rest are available via the error-report endpoint.
 *
 * @param jobId        persisted {@code BulkImportJob} id (null for preview)
 * @param totalRows    total data rows detected in the file
 * @param successCount rows that would be (or were) created successfully
 * @param errorCount   rows that failed validation or duplicate checks
 * @param credentialsProvisioned KC-native login credentials auto-provisioned for
 *        created students when the batch supplied an {@code initialPassword}
 *        (Wave flow-kc3, GAP-1277). 0 for preview and for commits without a batch
 *        password. ≤ {@code successCount}.
 * @param errors       first {@value #MAX_RETURNED_ERRORS} errors (inline preview)
 * @author KiteClass Team
 * @since 2.4.0
 */
public record BulkImportResult(
        Long jobId,
        int totalRows,
        int successCount,
        int errorCount,
        int credentialsProvisioned,
        List<RowError> errors
) {

    /** Maximum errors returned inline in the response body. */
    public static final int MAX_RETURNED_ERRORS = 10;
}
