package com.kiteclass.core.module.branding.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Inbound {@code branding.deployed} event from kitehub-branding (GAP-1213).
 *
 * <p>Mirror of the producer-side payload (kitehub-branding {@code outbox.BrandingDeployedEvent}).
 * {@code tenantId} IS the subscription Instance UUID = the RLS tenant here (same value as
 * {@code LandingPage.instanceId}). {@code @JsonIgnoreProperties(ignoreUnknown=true)} +
 * explicit {@code @JsonCreator}/{@code @JsonProperty} so the field names stay stable across the
 * record-accessor bytecode and the consumer survives the producer adding fields (per the
 * {@code TenantDeployedEvent} precedent).</p>
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

    @JsonCreator
    public BrandingDeployedEvent(
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("slug") String slug,
            @JsonProperty("frontendUrl") String frontendUrl,
            @JsonProperty("primaryColor") String primaryColor,
            @JsonProperty("secondaryColor") String secondaryColor,
            @JsonProperty("accentColor") String accentColor,
            @JsonProperty("logoUrl") String logoUrl,
            @JsonProperty("brandingVersion") Integer brandingVersion,
            @JsonProperty("deployedAt") String deployedAt) {
        this.tenantId = tenantId;
        this.slug = slug;
        this.frontendUrl = frontendUrl;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.accentColor = accentColor;
        this.logoUrl = logoUrl;
        this.brandingVersion = brandingVersion;
        this.deployedAt = deployedAt;
    }
}
