package com.kiteclass.core.module.lms.dto.response;

import lombok.Builder;

/**
 * Response DTO for course progress information.
 * Used to display overall progress for a user in a course.
 *
 * @since 2.9.0
 */
@Builder
public record CourseProgressResponse(
    Long courseId,
    Long userId,
    Integer totalLessons,
    Integer completedLessons,
    Double progressPercent  // Calculated: (completedLessons / totalLessons) * 100
) {
}
