package com.kiteclass.core.module.student.bulkimport.service;

import com.kiteclass.core.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an uploaded xlsx cannot be parsed at all (missing required
 * headers, corrupt file, empty workbook).
 *
 * <p>Returns HTTP 400 BAD_REQUEST.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
public class BulkImportParseException extends BusinessException {

    private static final String ERROR_CODE = "BULK_IMPORT_PARSE_ERROR";

    public BulkImportParseException(String detail) {
        super(ERROR_CODE, HttpStatus.BAD_REQUEST, detail);
    }

    public BulkImportParseException(String detail, Throwable cause) {
        super(ERROR_CODE, HttpStatus.BAD_REQUEST, cause, detail);
    }
}
