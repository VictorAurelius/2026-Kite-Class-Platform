package com.kiteclass.core.module.lms.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Response DTO for basic lesson information.
 *
 * @since 2.9.0
 */
@Builder
public record LessonResponse(
    Long id,
    Long moduleId,
    String title,
    String content,
    String videoUrl,
    Boolean isTrial,
    Integer orderNumber,
    Integer estimatedDuration,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
