package com.kiteclass.core.module.lms.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Response DTO for lesson progress information.
 *
 * @since 2.9.0
 */
@Builder
public record LessonProgressResponse(
    Long id,
    Long userId,
    Long lessonId,
    Boolean completed,
    LocalDateTime completedAt,
    Integer progressPercent,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
