package com.kiteclass.core.module.settings.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating branding.
 *
 * @since 2.9
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBrandingRequest {

    @NotBlank(message = "Display name is required")
    @Size(max = 200, message = "Display name must not exceed 200 characters")
    private String displayName;

    @Size(max = 500, message = "Tagline must not exceed 500 characters")
    private String tagline;

    @NotBlank(message = "Primary color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Primary color must be valid hex format (#RRGGBB)")
    private String primaryColor;

    @NotBlank(message = "Secondary color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Secondary color must be valid hex format (#RRGGBB)")
    private String secondaryColor;

    @NotBlank(message = "Accent color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Accent color must be valid hex format (#RRGGBB)")
    private String accentColor;

    @Email(message = "Contact email must be valid email format")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    private String contactEmail;

    @Size(max = 20, message = "Contact phone must not exceed 20 characters")
    private String contactPhone;

    private String address;

    @Size(max = 500, message = "Facebook URL must not exceed 500 characters")
    private String facebookUrl;

    @Size(max = 500, message = "Zalo URL must not exceed 500 characters")
    private String zaloUrl;

    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    private String websiteUrl;
}
