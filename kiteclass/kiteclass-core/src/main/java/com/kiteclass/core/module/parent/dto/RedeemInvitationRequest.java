package com.kiteclass.core.module.parent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public request submitted by the parent from the invitation-redemption page.
 *
 * <p>Password policy mirrors the Gateway's {@code AuthServiceImpl.PASSWORD_PATTERN}
 * — keeping it duplicated here avoids a cross-module dependency and makes the
 * validation error surface on the Core side without an extra round-trip.
 *
 * @param password     desired login password (8–100 chars, letter + digit + symbol)
 * @param fullName     parent's full name (2–100 chars)
 * @param phoneNumber  optional Vietnamese phone number
 * @param relationship FATHER / MOTHER / GUARDIAN
 * @since 2.14.0
 */
public record RedeemInvitationRequest(
        @NotBlank(message = "Mật khẩu là bắt buộc")
        @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8-100 ký tự")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
                message = "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt"
        )
        String password,

        @NotBlank(message = "Tên là bắt buộc")
        @Size(min = 2, max = 100, message = "Tên phải từ 2-100 ký tự")
        String fullName,

        @Pattern(
                regexp = "^$|^0\\d{9}$",
                message = "Số điện thoại không hợp lệ (phải là 10 số bắt đầu bằng 0)"
        )
        String phoneNumber,

        @Pattern(
                regexp = "^(FATHER|MOTHER|GUARDIAN)$",
                message = "Quan hệ phải là FATHER, MOTHER hoặc GUARDIAN"
        )
        String relationship
) {
}
