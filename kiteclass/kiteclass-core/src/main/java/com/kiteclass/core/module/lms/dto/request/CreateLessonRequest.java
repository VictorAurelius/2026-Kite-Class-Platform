package com.kiteclass.core.module.lms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a lesson.
 *
 * @since 2.9.0
 */
public record CreateLessonRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title,

    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    String content,

    @Size(max = 500, message = "Video URL must not exceed 500 characters")
    String videoUrl,

    Boolean isTrial,  // Default: false (set in service layer)

    @NotNull(message = "Order number is required")
    @Min(value = 1, message = "Order number must be at least 1")
    Integer orderNumber,

    @Min(value = 1, message = "Estimated duration must be at least 1 minute")
    Integer estimatedDuration  // Optional, in minutes
) {
}
