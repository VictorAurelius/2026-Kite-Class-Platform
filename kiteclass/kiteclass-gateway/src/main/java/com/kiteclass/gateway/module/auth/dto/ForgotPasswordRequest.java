package com.kiteclass.gateway.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for forgot password.
 *
 * @param email user email to send reset link
 * @author KiteClass Team
 * @since 1.0.0
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.format}")
        String email
) {
}
