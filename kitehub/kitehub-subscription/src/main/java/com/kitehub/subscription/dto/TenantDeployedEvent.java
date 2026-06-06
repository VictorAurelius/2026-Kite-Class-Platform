package com.kitehub.subscription.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cross-service signal that a KiteClass tenant finished provisioning (FrontendInstance
 * DEPLOYED) — published by kiteclass-core's tenant-creation consumer after the saga
 * returns successfully (Wave provisioning-1 Bucket C — GAP-948).
 *
 * <p>Mirror of kiteclass-core's {@code TenantDeployedEvent}. Consumed by
 * {@link com.kitehub.subscription.consumer.TenantDeployedEventConsumer}, which resolves
 * the owner email from {@code Instance} (keyed on {@code tenantId} = Instance UUID) and
 * dispatches the tenant-ready email via {@code EmailServiceClient.sendTenantReadyEmail}.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} keeps the contract forward-compatible
 * if kiteclass-core adds fields later. The explicit {@code @JsonCreator} keeps field names
 * stable regardless of record-accessor bytecode rendering.
 *
 * @param tenantId          the KiteHub Instance UUID (as String) — owner-resolution key
 * @param slug              provisioned subdomain slug (observability)
 * @param frontendInstanceId kiteclass-core FrontendInstance id (correlation; nullable)
 * @since Wave provisioning-1 Bucket C (GAP-948)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TenantDeployedEvent(
        String tenantId,
        String slug,
        Long frontendInstanceId
) {

    @JsonCreator
    public TenantDeployedEvent(
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("slug") String slug,
            @JsonProperty("frontendInstanceId") Long frontendInstanceId) {
        this.tenantId = tenantId;
        this.slug = slug;
        this.frontendInstanceId = frontendInstanceId;
    }
}
