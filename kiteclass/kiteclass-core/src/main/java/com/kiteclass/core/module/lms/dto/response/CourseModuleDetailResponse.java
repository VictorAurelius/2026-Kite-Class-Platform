package com.kiteclass.core.module.lms.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for detailed course module information with nested lessons.
 * Used for course structure endpoints.
 *
 * @since 2.9.0
 */
@Builder
public record CourseModuleDetailResponse(
    Long id,
    Long courseId,
    String title,
    String description,
    Integer orderNumber,
    List<LessonResponse> lessons,
    Integer lessonCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
