package com.kiteclass.core.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin sets/resets a tenant-scoped user's login password (Wave auth-1 — Hướng B).
 * Email + role come from the target domain entity (teacher/student), so the
 * request only carries the password. Reusable across roles.
 */
public record SetPasswordRequest(
        @NotBlank(message = "Mật khẩu là bắt buộc")
        @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8-100 ký tự")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Mật khẩu phải có chữ, số và ký tự đặc biệt")
        String password
) {
}
