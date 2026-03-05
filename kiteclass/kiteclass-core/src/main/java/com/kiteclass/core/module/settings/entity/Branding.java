package com.kiteclass.core.module.settings.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Branding entity for tenant customization.
 *
 * <p>Stores visual branding (logo, colors), contact info, and social media links.
 * One branding record per tenant (instance_id).
 *
 * @since 2.9
 */
@Entity
@Table(name = "branding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Branding extends BaseEntity {

    // Visual branding

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "favicon_url", length = 500)
    private String faviconUrl;

    @NotBlank(message = "Display name is required")
    @Size(max = 200, message = "Display name must not exceed 200 characters")
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Size(max = 500, message = "Tagline must not exceed 500 characters")
    @Column(name = "tagline", length = 500)
    private String tagline;

    // Colors (hex format: #RRGGBB)

    @NotBlank(message = "Primary color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Primary color must be valid hex format (#RRGGBB)")
    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor = "#3B82F6";

    @NotBlank(message = "Secondary color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Secondary color must be valid hex format (#RRGGBB)")
    @Column(name = "secondary_color", nullable = false, length = 7)
    private String secondaryColor = "#8B5CF6";

    @NotBlank(message = "Accent color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Accent color must be valid hex format (#RRGGBB)")
    @Column(name = "accent_color", nullable = false, length = 7)
    private String accentColor = "#10B981";

    // Contact information

    @Email(message = "Contact email must be valid email format")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Size(max = 20, message = "Contact phone must not exceed 20 characters")
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    // Social media links

    @Size(max = 500, message = "Facebook URL must not exceed 500 characters")
    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Size(max = 500, message = "Zalo URL must not exceed 500 characters")
    @Column(name = "zalo_url", length = 500)
    private String zaloUrl;

    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    @Column(name = "website_url", length = 500)
    private String websiteUrl;
}
