package com.kiteclass.core.module.student.bulkimport.dto;

/**
 * A single raw row parsed from the uploaded xlsx.
 *
 * <p>All fields are {@link String} so we can capture the user's original input
 * verbatim (preserves leading zeros in phone numbers, locale-formatted dates)
 * before applying validation and conversion.
 *
 * @param rowNumber   1-indexed row number in the original file (header = 1,
 *                    first data row = 2)
 * @param name        student full name (required)
 * @param email       student email (required)
 * @param phone       Vietnamese phone, 10 digits starting with 0 (optional)
 * @param dateOfBirth date of birth in {@code dd/MM/yyyy} format (optional)
 * @param gender      MALE / FEMALE / OTHER, case-insensitive (optional)
 * @param address     free-text address (optional)
 * @param note        free-text note (optional)
 * @author KiteClass Team
 * @since 2.4.0
 */
public record BulkImportRow(
        int rowNumber,
        String name,
        String email,
        String phone,
        String dateOfBirth,
        String gender,
        String address,
        String note
) {
}
