package com.kiteclass.core.module.lms.dto.response;

import com.kiteclass.core.common.constant.ResourceType;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Response DTO for learning resource information.
 *
 * @since 2.9.0
 */
@Builder
public record LearningResourceResponse(
    Long id,
    Long lessonId,
    ResourceType type,
    String url,
    String title,
    Long fileSize,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
