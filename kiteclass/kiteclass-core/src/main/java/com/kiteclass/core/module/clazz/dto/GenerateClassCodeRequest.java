package com.kiteclass.core.module.clazz.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Request DTO for generating a class enrollment code.
 *
 * @param customCode  Optional custom code (6-20 uppercase alphanumeric chars).
 *                    When null, a random code is auto-generated.
 * @param expiresAt   Optional expiry timestamp. When null, code never expires.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public record GenerateClassCodeRequest(

        @Size(min = 6, max = 20, message = "Mã lớp phải từ 6 đến 20 ký tự")
        @Pattern(regexp = "^[A-Z0-9]*$", message = "Mã lớp chỉ được chứa chữ in hoa và số")
        String customCode,

        Instant expiresAt
) {
}
