package com.kiteclass.core.module.lms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a course module.
 * All fields are optional - only provided fields will be updated.
 *
 * @since 2.9.0
 */
public record UpdateCourseModuleRequest(
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title,

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,

    @Min(value = 1, message = "Order number must be at least 1")
    Integer orderNumber
) {
}
