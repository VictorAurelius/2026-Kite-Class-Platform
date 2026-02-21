package com.kiteclass.core.module.clazz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for cancelling a class.
 *
 * @param reason  Reason for cancellation (required, max 500 chars)
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public record CancelClassRequest(

        @NotBlank(message = "Lý do hủy lớp không được để trống")
        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
        String reason
) {
}
