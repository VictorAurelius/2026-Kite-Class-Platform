package com.kitehub.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO for setting up a custom domain.
 * Only available for PREMIUM and ENTERPRISE tiers.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
public class DomainSetupRequest {

    /**
     * Custom domain to set up (e.g., "school.example.com").
     * Must be a valid fully-qualified domain name.
     */
    @NotBlank(message = "Custom domain is required")
    @Pattern(
        regexp = "^[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]\\.[a-zA-Z]{2,}$",
        message = "Invalid domain format. Use format: subdomain.domain.tld (e.g., school.example.com)"
    )
    private String customDomain;
}
