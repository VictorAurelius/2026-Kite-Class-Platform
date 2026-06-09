package com.kiteclass.core.module.enrollment.bulkimport.dto;

/**
 * A single raw row parsed from the uploaded bulk-enroll xlsx.
 *
 * <p>All fields are {@link String} to capture the user's original input verbatim
 * (preserves leading zeros in phone numbers, exact class-code casing) before
 * applying validation and resolution to entity IDs.
 *
 * <p>Mirrors {@code BulkImportRow} (student bulk-import) but with enrollment-specific
 * columns. A student is resolved by {@code studentEmail} (preferred) then
 * {@code studentPhone}; the class is resolved by {@code classCode}.
 *
 * @param rowNumber       1-indexed row number in the original file (header = 1,
 *                        first data row = 2)
 * @param studentEmail    student email — used to resolve the student (optional if
 *                        {@code studentPhone} present)
 * @param studentPhone    student phone — fallback resolver when email absent
 * @param classCode       class code (required) — resolves the target class
 * @param tuitionAmount   tuition amount as raw string (required, numeric)
 * @param discountPercent discount percent 0-100 as raw string (optional, default 0)
 * @param note            free-text enrollment note (optional)
 * @author KiteClass Team
 * @since 2.7.0
 */
public record EnrollmentBulkRow(
        int rowNumber,
        String studentEmail,
        String studentPhone,
        String classCode,
        String tuitionAmount,
        String discountPercent,
        String note
) {
}
