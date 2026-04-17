package com.kiteclass.core.module.parent.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin / teacher request to invite a parent to link to a specific student.
 *
 * @param studentId   id of the child in this tenant (required)
 * @param parentEmail email where the invitation link will be sent (required, unique-per-tenant)
 * @since 2.14.0
 */
public record InviteParentRequest(
        @NotNull(message = "studentId là bắt buộc")
        Long studentId,

        @NotBlank(message = "Email là bắt buộc")
        @Email(message = "Email không hợp lệ")
        @Size(max = 255)
        String parentEmail
) {
}
