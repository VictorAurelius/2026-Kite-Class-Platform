package com.kiteclass.core.module.lms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a course module.
 *
 * @since 2.9.0
 */
public record CreateCourseModuleRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title,

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,

    @NotNull(message = "Order number is required")
    @Min(value = 1, message = "Order number must be at least 1")
    Integer orderNumber
) {
}
