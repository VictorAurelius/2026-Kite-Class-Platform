package com.kiteclass.core.module.settings.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Branding.
 *
 * @since 2.9
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandingResponse {
    private Long id;

    // Visual branding
    private String logoUrl;
    private String faviconUrl;
    private String displayName;
    private String tagline;

    // Colors
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;

    // Contact information
    private String contactEmail;
    private String contactPhone;
    private String address;

    // Social media links
    private String facebookUrl;
    private String zaloUrl;
    private String websiteUrl;
}
