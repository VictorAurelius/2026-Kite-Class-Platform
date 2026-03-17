package com.kitehub.subscription.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Self-service registration request DTO.
 *
 * @since 1.0.0
 */
@Data
public class RegisterRequest {
    @NotBlank
    @Size(max = 200)
    private String organizationName;

    @NotBlank
    @Size(min = 3, max = 50)
    private String subdomain;

    @NotBlank
    @Email
    private String ownerEmail;

    @NotBlank
    @Size(min = 8)
    private String ownerPassword;
}
