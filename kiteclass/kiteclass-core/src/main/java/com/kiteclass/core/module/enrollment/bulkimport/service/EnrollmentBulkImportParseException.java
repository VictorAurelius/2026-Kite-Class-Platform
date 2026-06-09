package com.kiteclass.core.module.enrollment.bulkimport.service;

import com.kiteclass.core.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the uploaded bulk-enroll xlsx cannot be parsed — missing required
 * headers, empty workbook, corrupt bytes, or I/O error.
 *
 * <p>Maps to HTTP 400 BAD_REQUEST. Mirrors {@code BulkImportParseException}
 * (student bulk-import) so the failure semantics are consistent across both
 * features.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
public class EnrollmentBulkImportParseException extends BusinessException {

    /** Stable error code surfaced to API consumers. */
    public static final String ERROR_CODE = "ENROLLMENT_BULK_IMPORT_PARSE_ERROR";

    /**
     * @param detail human-readable cause (Vietnamese)
     */
    public EnrollmentBulkImportParseException(String detail) {
        super(ERROR_CODE, HttpStatus.BAD_REQUEST, detail);
    }

    /**
     * @param detail human-readable cause (Vietnamese)
     * @param cause  underlying exception
     */
    public EnrollmentBulkImportParseException(String detail, Throwable cause) {
        super(ERROR_CODE, HttpStatus.BAD_REQUEST, cause, detail);
    }
}
