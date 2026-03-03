package com.kiteclass.core.module.lms.dto.request;

import com.kiteclass.core.common.constant.ResourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a learning resource.
 *
 * @since 2.9.0
 */
public record CreateLearningResourceRequest(
    @NotNull(message = "Resource type is required")
    ResourceType type,

    @NotBlank(message = "URL is required")
    @Size(max = 500, message = "URL must not exceed 500 characters")
    String url,

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title,

    @Min(value = 1, message = "File size must be at least 1 byte")
    Long fileSize  // Optional, in bytes
) {
}
