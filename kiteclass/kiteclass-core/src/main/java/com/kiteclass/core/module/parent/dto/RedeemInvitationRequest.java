package com.kiteclass.core.module.parent.dto;

import com.kiteclass.core.module.auth.AuthPasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public request submitted by the parent from the invitation-redemption page.
 *
 * <p>Wave auth-2 (GAP-1013c): password policy unified via {@link AuthPasswordPolicy}
 * — the single shared validator for every {@code auth_credentials} provisioning
 * path (parent redeem + teacher/student admin set-password). Broadens the old
 * restricted symbol set to any non-alphanumeric while keeping the upper/lower/
 * digit/symbol requirement.
 *
 * @param password     desired login password (8–100 chars, upper + lower + digit + symbol)
 * @param fullName     parent's full name (2–100 chars)
 * @param phoneNumber  optional Vietnamese phone number
 * @param relationship FATHER / MOTHER / GUARDIAN
 * @since 2.14.0
 */
public record RedeemInvitationRequest(
        @NotBlank(message = "Mật khẩu là bắt buộc")
        @Size(min = AuthPasswordPolicy.MIN_LENGTH, max = AuthPasswordPolicy.MAX_LENGTH,
                message = "Mật khẩu phải từ 8-100 ký tự")
        @Pattern(regexp = AuthPasswordPolicy.PATTERN, message = AuthPasswordPolicy.MESSAGE)
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
