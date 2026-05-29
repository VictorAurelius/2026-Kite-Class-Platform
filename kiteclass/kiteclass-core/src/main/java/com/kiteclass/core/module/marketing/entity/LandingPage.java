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

    // Data-driven landing sections (wave-thesis-4) — all nullable; FE reads per-tenant
    // content from DB instead of hardcoded copy. JSONB-backed via Hibernate JdbcTypeCode
    // (GAP-220 pattern: bind structured Java type → jsonb, not VARCHAR).

    /** Free-text "About" / introduction paragraph for the center. */
    @Column(name = "about_text", columnDefinition = "TEXT")
    private String aboutText;

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
}
