package com.kiteclass.core.module.marketing.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Response DTO for landing page content.
 *
 * @since 2.10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingPageResponse {

    private Long id;

    // Center identity (GAP-1083) — nav/footer/JsonLd prefer this over heroTitle slogan.
    private String centerName;

    private String heroTitle;
    private String heroSubtitle;
    private String heroImageUrl;

    private String teacherBio;

    private String logoUrl;
    private String tagline;
    private String primaryColor;
    private String secondaryColor;

    private String contactEmail;
    private String contactPhone;
    private String address;

    private String facebookUrl;
    private String youtubeUrl;
    private String instagramUrl;

    // Zalo OA deep-link for FloatingCTA (GAP-1083). Hidden by FE when null.
    private String zaloUrl;

    // Data-driven landing sections (wave-thesis-4) — all nullable; FE falls back
    // to defaults when null. Shapes documented on the LandingPage entity fields.
    private String aboutText;
    private String templateType;
    private JsonNode teachers;
    private JsonNode programs;
    private JsonNode pricingTiers;
    private JsonNode testimonials;
    private JsonNode faqs;
    private JsonNode stats;

    // landing-100 F-sections (GAP-1083) — all nullable; FE sections fall back to VN
    // defaults when null. Shapes documented on the LandingPage entity fields.
    private JsonNode problemSolution;
    private JsonNode howItWorks;
    private JsonNode trustStrip;
}
