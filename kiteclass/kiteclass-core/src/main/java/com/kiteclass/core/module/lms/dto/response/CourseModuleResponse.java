package com.kiteclass.core.module.lms.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Response DTO for basic course module information.
 *
 * @since 2.9.0
 */
@Builder
public record CourseModuleResponse(
    Long id,
    Long courseId,
    String title,
    String description,
    Integer orderNumber,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
