package com.kitehub.email.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Branding subset consumed by email templates.
 *
 * <p>Mirrors essential fields from kiteclass-core's branding package response.
 * Kept intentionally narrow — we only need what templates reference.
 *
 * <p>{@link Serializable} lets Spring Cache serialize instances into any cache
 * backend (Caffeine, Redis) without extra configuration.
 *
 * @since Wave 4 (GAP-021 — branding propagation to email templates)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantBranding implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Tenant's display name, e.g. "ABC Education Center". */
    private String displayName;

    /** Absolute URL to tenant logo (null if none uploaded). */
    private String logoUrl;

    /** Primary color in hex form, e.g. "#2563EB". */
    private String primaryColor;

    /** Secondary color in hex form. */
    private String secondaryColor;

    /** Accent color in hex form. */
    private String accentColor;

    /** Contact email for footer. */
    private String contactEmail;

    /** Tenant-facing organization name (may equal displayName). */
    private String organizationName;

    /**
     * Default / fallback branding used when tenant context missing or lookup fails.
     * Matches legacy hardcoded palette in pre-Wave-4 templates so emails continue
     * to look correct even with graceful degradation.
     *
     * <p>GAP-543 (Wave 107 tone pass): {@code contactEmail} corrected from the
     * unreachable {@code support@kitehub.me} to {@code support@kitehub.me} —
     * Phase 1 BETA operates on the {@code kitehub.me} domain (dual-brand KiteClass
     * customer-facing rename deferred to Phase 1.5+ per ad-interim decision). A
     * non-existent support address in every email footer = trust loss + bounced
     * help requests, so the fallback contact must resolve to a real mailbox.</p>
     */
    public static TenantBranding defaultBranding() {
        return TenantBranding.builder()
                .displayName("KiteClass")
                .organizationName("KiteClass")
                .primaryColor("#667eea")
                .secondaryColor("#764ba2")
                .accentColor("#10B981")
                .contactEmail("support@kitehub.me")
                .build();
        }
}
