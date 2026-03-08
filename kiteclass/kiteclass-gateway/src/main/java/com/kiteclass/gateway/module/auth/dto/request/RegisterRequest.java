package com.kiteclass.gateway.module.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Simplified request DTO for user registration.
 *
 * <p>Used for basic registration flow with minimal required fields.
 * For student-specific registration with additional profile fields,
 * use {@link com.kiteclass.gateway.module.auth.dto.RegisterStudentRequest}.
 *
 * @param email    User's email (used for authentication)
 * @param password User's password (will be hashed with BCrypt)
 * @param name     User's full name
 * @author KiteClass Team
 * @since 1.1.0
 */
public record RegisterRequest(
        @NotBlank(message = "validation.email.required")
        @Email(message = "validation.email.invalid")
        @Size(max = 255, message = "validation.max_length")
        String email,

        @NotBlank(message = "validation.password.required")
        @Size(min = 8, max = 100, message = "validation.password.size")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
                message = "validation.password.pattern"
        )
        String password,

        @NotBlank(message = "validation.required")
        @Size(min = 2, max = 100, message = "validation.max_length")
        String name
) {
}
