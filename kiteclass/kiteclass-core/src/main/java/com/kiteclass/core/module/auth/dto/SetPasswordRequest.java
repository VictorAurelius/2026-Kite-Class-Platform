package com.kiteclass.core.module.auth.dto;

import com.kiteclass.core.module.auth.AuthPasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin sets/resets a tenant-scoped user's login password (Wave auth-1 — Hướng B).
 * Email + role come from the target domain entity (teacher/student), so the
 * request only carries the password. Reusable across roles.
 *
 * <p>Wave auth-2 (GAP-1013c): password policy unified via {@link AuthPasswordPolicy}
 * — same constraint as the parent self-redeem path ({@code RedeemInvitationRequest}).
 */
public record SetPasswordRequest(
        @NotBlank(message = "Mật khẩu là bắt buộc")
        @Size(min = AuthPasswordPolicy.MIN_LENGTH, max = AuthPasswordPolicy.MAX_LENGTH,
                message = "Mật khẩu phải từ 8-100 ký tự")
        @Pattern(regexp = AuthPasswordPolicy.PATTERN, message = AuthPasswordPolicy.MESSAGE)
        String password
) {
}
