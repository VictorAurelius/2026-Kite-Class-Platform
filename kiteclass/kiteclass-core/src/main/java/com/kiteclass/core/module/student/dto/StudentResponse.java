package com.kiteclass.core.module.student.dto;

import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.common.constant.StudentStatus;

import java.time.LocalDate;

/**
 * Response DTO for Student entity.
 *
 * <p>Contains all student information returned to clients.
 * Uses Java record for immutability and concise syntax.
 *
 * @param id           Student's unique identifier
 * @param name         Student's full name
 * @param email        Student's email address
 * @param phone        Student's phone number
 * @param dateOfBirth  Student's date of birth
 * @param gender       Student's gender
 * @param address      Student's address
 * @param avatarUrl    URL to student's avatar
 * @param status       Current status of the student
 * @param note         Additional notes about the student
 * @author KiteClass Team
 * @since 2.3.0
 */
public record StudentResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth,
        Gender gender,
        String address,
        String avatarUrl,
        StudentStatus status,
        String note
) {
}
