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
    // KH-7 FM-2: the old regex matched ONLY 2-label domains (label.tld), rejecting
    // multi-label FQDNs like school.example.com and — critically for the VN market —
    // every `*.edu.vn` school domain. Allow one or more sub-labels before the TLD.
    @NotBlank(message = "Custom domain is required")
    @Pattern(
        regexp = "^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$",
        message = "Invalid domain format. Use a fully-qualified domain (e.g., school.example.com or truong.edu.vn)"
    )
    private String customDomain;
}
