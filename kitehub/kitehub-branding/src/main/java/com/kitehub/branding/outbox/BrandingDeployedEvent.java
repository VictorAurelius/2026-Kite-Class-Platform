package com.kitehub.branding.outbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cross-service {@code branding.deployed} event payload (GAP-1213).
 *
 * <p>Emitted by kitehub-branding when a wizard deploy reaches DEPLOYED, consumed by
 * kiteclass-core to apply the theme + assets onto the tenant landing page so the public
 * landing actually changes after "Triển khai thành công". Mirrors the {@code TenantDeployedEvent}
 * record + {@code @JsonCreator}/{@code @JsonProperty} precedent (field-name stable across the
 * record-accessor bytecode) + {@code @JsonIgnoreProperties(ignoreUnknown=true)} so the
 * consumer is forward-compatible when new fields are added.</p>
 *
 * <p>{@code tenantId} IS the subscription Instance UUID = the RLS tenant in kiteclass-core
 * (same value as {@code BrandingJob.instanceId}). {@code brandingVersion} drives idempotency
 * on the consumer (apply only when incoming version &gt; stored). Asset URLs are stable object
 * keys / mock-CDN placeholders — NOT short-lived presigned URLs.</p>
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
