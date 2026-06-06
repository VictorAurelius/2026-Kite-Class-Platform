package com.kiteclass.core.module.provisioning;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Signal that a tenant finished provisioning (FrontendInstance DEPLOYED) — published by
 * {@link TenantCreatedEventConsumer} back to kitehub-subscription after
 * {@link TenantProvisioningSaga#provision(TenantCreatedEvent)} returns (Wave provisioning-1
 * Bucket C — GAP-948).
 *
 * <p>kitehub-subscription's {@code TenantDeployedEventConsumer} resolves the owner email
 * from its {@code Instance} table (keyed on {@code tenantId} = Instance UUID — the recipient
 * is NOT available in kiteclass-core) and dispatches the tenant-ready email.
 *
 * <p>Serialized to a raw-UTF8 JSON String (content-type {@code application/json}, GAP-925
 * wire-format) by {@link TenantReadyNotifier} — mirroring the {@code tenant.created} contract.
 * The explicit {@code @JsonCreator} keeps field names stable across record-accessor bytecode.
 *
 * @param tenantId           Instance UUID (as String) — copied from {@link TenantCreatedEvent}
 * @param slug               provisioned subdomain slug
 * @param frontendInstanceId kiteclass-core FrontendInstance id returned by the saga (correlation)
 * @since Wave provisioning-1 Bucket C (GAP-948)
 */
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
