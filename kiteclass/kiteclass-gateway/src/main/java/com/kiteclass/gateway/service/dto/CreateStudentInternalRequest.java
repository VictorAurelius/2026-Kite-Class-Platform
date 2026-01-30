package com.kiteclass.gateway.service.dto;

import java.time.LocalDate;

/**
 * Request DTO for creating a student in Core service (internal API).
 *
 * <p>This DTO is used by Gateway service when calling Core's POST /internal/students
 * endpoint during student registration flow.
 *
 * <p>Maps to Core's CreateStudentRequest.
 *
 * @param name        Student's full name (required)
 * @param email       Student's email address
 * @param phone       Student's phone number (Vietnamese format: 10 digits starting with 0)
 * @param dateOfBirth Student's date of birth
 * @param gender      Student's gender (MALE, FEMALE, OTHER)
 * @param address     Student's address
 * @param note        Additional notes
 * @author KiteClass Team
 * @since 1.8.0
 */
public record CreateStudentInternalRequest(
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String address,
        String note
) {
}
