package com.kiteclass.gateway.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 *
 * @param email user email
 * @param password user password
 * @author KiteClass Team
 * @since 1.0.0
 */
public record LoginRequest(

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.format}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        String password
) {
}
