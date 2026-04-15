package com.kiteclass.core.module.instance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarkFailedRequest(
        @NotBlank @Size(max = 1000) String reason) {
}
