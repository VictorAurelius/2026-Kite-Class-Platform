package com.kiteclass.core.module.instance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/instances}.
 *
 * @since 3.20.0 (Wave 3 Sub-PR 3.4)
 */
public record InitiateInstanceRequest(

        @NotBlank
        @Size(max = 100)
        String tenantId,

        @NotBlank
        @Size(min = 3, max = 80)
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$",
                message = "slug must be lowercase letters/digits/hyphens, start+end alphanumeric")
        String slug
) {
}
