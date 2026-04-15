package com.kiteclass.core.module.instance.dto;

import jakarta.validation.constraints.Size;

public record MarkBrandingCompletedRequest(
        @Size(max = 300) String frontendUrl) {
}
