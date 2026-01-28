package com.kiteclass.gateway.service.dto;

/**
 * Teacher profile response from Core service.
 *
 * <p>Simplified DTO for Gateway service to display teacher profile information.
 * Fetched from Core service via Feign Client when user logs in with userType = TEACHER.
 *
 * <p><b>Note:</b> Teacher module not yet implemented in Core service (planned for future PR).
 * This DTO is a placeholder to support the cross-service architecture.
 *
 * @param id        Teacher's unique identifier (matches User.referenceId)
 * @param name      Teacher's full name
 * @param email     Teacher's email address
 * @param phone     Teacher's phone number
 * @param subject   Primary subject taught by teacher
 * @param avatarUrl URL to teacher's avatar image
 * @param status    Teacher's current status (ACTIVE, INACTIVE, etc.)
 * @author KiteClass Team
 * @since 1.8.0
 */
public record TeacherProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        String subject,
        String avatarUrl,
        String status
) {
}
