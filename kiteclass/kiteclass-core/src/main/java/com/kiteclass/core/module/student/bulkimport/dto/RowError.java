package com.kiteclass.core.module.student.bulkimport.dto;

/**
 * Describes a single validation/processing error for one row of the uploaded
 * xlsx.
 *
 * @param rowNumber 1-indexed row number (matches spreadsheet row)
 * @param field     name of the offending field, or {@code "row"} for
 *                  row-level problems (e.g. completely empty row)
 * @param message   human-readable error message (Vietnamese)
 * @author KiteClass Team
 * @since 2.4.0
 */
public record RowError(
        int rowNumber,
        String field,
        String message
) {
}
