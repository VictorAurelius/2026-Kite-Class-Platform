package com.kiteclass.core.module.lms.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for detailed lesson information with nested resources.
 * Used for lesson detail endpoints.
 *
 * @since 2.9.0
 */
@Builder
public record LessonDetailResponse(
    Long id,
    Long moduleId,
    String title,
    String content,
    String videoUrl,
    Boolean isTrial,
    Integer orderNumber,
    Integer estimatedDuration,
    List<LearningResourceResponse> resources,
    Integer resourceCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
