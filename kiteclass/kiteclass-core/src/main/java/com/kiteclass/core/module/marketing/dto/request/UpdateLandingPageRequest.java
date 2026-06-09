package com.kiteclass.core.module.marketing.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating landing page content.
 * All fields optional for partial updates (PATCH semantics).
 *
 * @since 2.10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLandingPageRequest {

    @Size(max = 200, message = "{landing.centerName.size}")
    private String centerName;

    @Size(max = 200, message = "{landing.hero.title.size}")
    private String heroTitle;

    @Size(max = 500, message = "{landing.hero.subtitle.size}")
    private String heroSubtitle;

    @Size(max = 500, message = "{landing.hero.image.size}")
    private String heroImageUrl;

    @Size(max = 2000, message = "{landing.teacherBio.size}")
    private String teacherBio;

    @Size(max = 500, message = "{landing.logo.size}")
    private String logoUrl;

    @Size(max = 200, message = "{landing.tagline.size}")
    private String tagline;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{landing.color.invalid}")
    private String primaryColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{landing.color.invalid}")
    private String secondaryColor;

    @Email(message = "{landing.email.invalid}")
    @Size(max = 255, message = "{landing.email.size}")
    private String contactEmail;

    @Size(max = 20, message = "{landing.phone.size}")
    private String contactPhone;

    @Size(max = 500, message = "{landing.address.size}")
    private String address;

    @Size(max = 255, message = "{landing.social.size}")
    private String facebookUrl;

    @Size(max = 255, message = "{landing.social.size}")
    private String youtubeUrl;

    @Size(max = 255, message = "{landing.social.size}")
    private String instagramUrl;

    @Size(max = 255, message = "{landing.social.size}")
    private String zaloUrl;

    // Data-driven landing sections (wave-thesis-4) — completes Entity-Migration-Mapper
    // triad per design-patterns.md §3.12. FE reads per-tenant content from DB; admin
    // PATCH endpoint updates these via this DTO. JsonNode fields = JSONB columns; client
    // sends JSON tree, Jackson deserializes. Shape contracts documented on entity fields.
    @Size(max = 5000, message = "{landing.aboutText.size}")
    private String aboutText;

    @Pattern(regexp = "^(personal|organization)$", message = "{landing.template.type.invalid}")
    private String templateType;

    private JsonNode teachers;
    private JsonNode programs;
    private JsonNode pricingTiers;
    private JsonNode testimonials;
    private JsonNode faqs;
    private JsonNode stats;

    // landing-100 F-sections (GAP-1083). JsonNode → JSONB columns. Shape contracts
    // documented on the LandingPage entity fields.
    private JsonNode problemSolution;
    private JsonNode howItWorks;
    private JsonNode trustStrip;
}
