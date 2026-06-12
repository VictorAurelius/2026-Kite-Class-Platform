package com.kiteclass.core.module.branding.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Inbound {@code branding.deployed} event from kitehub-branding (GAP-1213).
 *
 * <p>Mirror of the producer-side payload (kitehub-branding {@code outbox.BrandingDeployedEvent}).
 * {@code tenantId} IS the subscription Instance UUID = the RLS tenant here (same value as
 * {@code LandingPage.instanceId}). {@code @JsonIgnoreProperties(ignoreUnknown=true)} để consumer survive khi
 * producer thêm field (per {@code TenantDeployedEvent} precedent). Jackson 2.15 deserialize
 * record qua canonical constructor + component names natively — explicit
 * {@code @JsonCreator} 9-param bị checkstyle ParameterNumber (max 8) chặn nên bỏ.</p>
 *
 * @since GAP-1213 (Wave branding-100 Bucket C)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrandingDeployedEvent(
        String tenantId,
        String slug,
        String frontendUrl,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String logoUrl,
        Integer brandingVersion,
        String deployedAt
) {
}

