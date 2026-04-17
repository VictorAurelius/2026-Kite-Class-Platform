package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.enums.PricingTier;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new instance.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInstanceRequest {

    @NotBlank(message = "Subdomain is required")
    @Size(min = 3, max = 50, message = "Subdomain must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomain must contain only lowercase letters, numbers, and hyphens")
    private String subdomain;

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 200, message = "Organization name must be between 2 and 200 characters")
    private String organizationName;

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    private String contactEmail;

    @NotNull(message = "Pricing tier is required")
    private PricingTier tier;

    @Size(max = 255, message = "Custom domain must not exceed 255 characters")
    private String customDomain;
}
