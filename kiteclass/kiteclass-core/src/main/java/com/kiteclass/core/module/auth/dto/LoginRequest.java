package com.kiteclass.core.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * KC-native login request (Wave auth-1) for PARENT/TEACHER/STUDENT.
 */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format invalid")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
