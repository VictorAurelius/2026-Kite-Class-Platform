package com.kiteclass.core.module.marketing.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Landing page content for each tenant.
 * Business Rule: BR-MKT-001 - Each tenant has ONE landing page.
 *
 * @since 2.10
 */
@Entity
@Table(name = "landing_pages")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "instance_id = :tenantId AND deleted = false")
public class LandingPage extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    // Center identity (GAP-1083) — tenant/center display name. Nav/footer/JsonLd prefer this
    // over heroTitle (the marketing slogan). Nullable; inherited from settings.Branding
    // displayName on first create, FE falls back to heroTitle → generic when null.
    @Column(name = "center_name", length = 200)
    @Size(max = 200, message = "{landing.centerName.size}")
    private String centerName;

    // Hero Section
    @Column(name = "hero_title", nullable = false, length = 200)
    @Size(max = 200, message = "{landing.hero.title.size}")
    private String heroTitle = "Welcome to Our Learning Center";

    @Column(name = "hero_subtitle", length = 500)
    @Size(max = 500, message = "{landing.hero.subtitle.size}")
    private String heroSubtitle;

    @Column(name = "hero_image_url", length = 500)
    @Size(max = 500, message = "{landing.hero.image.size}")
    private String heroImageUrl;

    // Teacher/About Section
    @Column(name = "teacher_bio", columnDefinition = "TEXT")
    @Size(max = 2000, message = "{landing.teacherBio.size}")
    private String teacherBio;

    // Branding
    @Column(name = "logo_url", length = 500)
    @Size(max = 500, message = "{landing.logo.size}")
    private String logoUrl;

    @Column(name = "tagline", length = 200)
    @Size(max = 200, message = "{landing.tagline.size}")
    private String tagline;

    @Column(name = "primary_color", length = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{landing.color.invalid}")
    private String primaryColor = "#3B82F6";

    @Column(name = "secondary_color", length = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{landing.color.invalid}")
    private String secondaryColor = "#8B5CF6";

    // Contact Info
    @Column(name = "contact_email")
    @Email(message = "{landing.email.invalid}")
    @Size(max = 255, message = "{landing.email.size}")
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    @Size(max = 20, message = "{landing.phone.size}")
    private String contactPhone;

    @Column(name = "address", columnDefinition = "TEXT")
    @Size(max = 500, message = "{landing.address.size}")
    private String address;

    // Social Media
    @Column(name = "facebook_url")
    @Size(max = 255, message = "{landing.social.size}")
    private String facebookUrl;

    @Column(name = "youtube_url")
    @Size(max = 255, message = "{landing.social.size}")
    private String youtubeUrl;

    @Column(name = "instagram_url")
    @Size(max = 255, message = "{landing.social.size}")
    private String instagramUrl;

    // Zalo OA deep-link for the FloatingCTA (GAP-1083). Bare Zalo ID or full zalo.me URL;
    // FE normalises to https://zalo.me/<id>. Nullable; inherited from settings.Branding
    // zaloUrl on first create. FloatingCTA hides the Zalo button when null.
    @Column(name = "landing_zalo_url", length = 255)
    @Size(max = 255, message = "{landing.social.size}")
    private String zaloUrl;

    // Data-driven landing sections (wave-thesis-4) — all nullable; FE reads per-tenant
    // content from DB instead of hardcoded copy. JSONB-backed via Hibernate JdbcTypeCode
    // (GAP-220 pattern: bind structured Java type → jsonb, not VARCHAR).

    /** Free-text "About" / introduction paragraph for the center. */
    @Column(name = "about_text", columnDefinition = "TEXT")
    @Size(max = 5000, message = "{landing.aboutText.size}")
    private String aboutText;

    /** Landing template type: "personal" (GV độc lập) | "organization" (trung tâm). */
    @Column(name = "template_type", length = 20)
    private String templateType;

    /** Teacher cards: [{"name","subject","credentials":["..."]}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teachers", columnDefinition = "jsonb")
    private JsonNode teachers;

    /** Programs / subjects offered: [{"name","description","detail":["..."]}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "programs", columnDefinition = "jsonb")
    private JsonNode programs;

    /** Pricing tiers: [{"name","price","period","features":["..."],"highlighted":bool}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pricing_tiers", columnDefinition = "jsonb")
    private JsonNode pricingTiers;

    /** Testimonials: [{"author","role","content","rating":int}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "testimonials", columnDefinition = "jsonb")
    private JsonNode testimonials;

    /** FAQs: [{"question","answer"}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "faqs", columnDefinition = "jsonb")
    private JsonNode faqs;

    /** Stats highlights: [{"value","label"}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stats", columnDefinition = "jsonb")
    private JsonNode stats;

    // landing-100 F-sections (GAP-1083) — all nullable; FE ProblemSolution/HowItWorks/
    // TrustStrip sections fall back to generic VN platform copy when null (per-section
    // DEFAULT, NOT fabricated partner data — see component docs / GAP-958 spirit).

    /** Problem→Solution cards: [{"title" (pain),"description" (problem),"items":["fix"]}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "problem_solution", columnDefinition = "jsonb")
    private JsonNode problemSolution;

    /** How-it-works steps: [{"title" (step name),"description"}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "how_it_works", columnDefinition = "jsonb")
    private JsonNode howItWorks;

    /** Trust signals: [{"icon" ("shield"|"lock"|"support"|"vn"|"spark"),"title","description"}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trust_strip", columnDefinition = "jsonb")
    private JsonNode trustStrip;
}
