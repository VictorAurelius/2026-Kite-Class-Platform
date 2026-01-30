package com.kiteclass.core.module.teacher.dto;

/**
 * Response DTO for Teacher entity.
 *
 * <p>Contains complete teacher information for API responses.
 *
 * @param id Teacher ID
 * @param name Teacher's full name
 * @param email Teacher's email address
 * @param phoneNumber Teacher's phone number
 * @param specialization Teacher's specialization/subject area
 * @param bio Teacher's biography
 * @param qualification Teacher's qualification/education
 * @param experienceYears Years of teaching experience
 * @param avatarUrl URL to teacher's avatar
 * @param status Current teacher status (ACTIVE, INACTIVE, ON_LEAVE)
 * @author KiteClass Team
 * @since 2.3.1
 */
public record TeacherResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String specialization,
        String bio,
        String qualification,
        Integer experienceYears,
        String avatarUrl,
        String status
) {
}
