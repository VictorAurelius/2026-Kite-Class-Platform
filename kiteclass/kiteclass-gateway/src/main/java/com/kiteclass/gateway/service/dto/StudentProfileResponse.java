package com.kiteclass.gateway.service.dto;

import java.time.LocalDate;

/**
 * Student profile response from Core service.
 *
 * <p>Simplified DTO for Gateway service to display student profile information.
 * Fetched from Core service via Feign Client when user logs in with userType = STUDENT.
 *
 * @param id          Student's unique identifier (matches User.referenceId)
 * @param name        Student's full name
 * @param email       Student's email address
 * @param phone       Student's phone number
 * @param dateOfBirth Student's date of birth
 * @param gender      Student's gender (MALE, FEMALE, OTHER)
 * @param avatarUrl   URL to student's avatar image
 * @param status      Student's current status (ACTIVE, INACTIVE, etc.)
 * @author KiteClass Team
 * @since 1.8.0
 */
public record StudentProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String avatarUrl,
        String status
) {
}
