package com.kiteclass.gateway.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for resetting password with token.
 *
 * @param token password reset token
 * @param newPassword new password
 * @author KiteClass Team
 * @since 1.0.0
 */
public record ResetPasswordRequest(

        @NotBlank(message = "{validation.resetToken.required}")
        String token,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, max = 128, message = "{validation.password.size}")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "{validation.password.pattern}"
        )
        String newPassword
) {
}
