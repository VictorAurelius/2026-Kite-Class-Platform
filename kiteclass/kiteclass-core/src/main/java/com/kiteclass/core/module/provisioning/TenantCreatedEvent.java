package com.kiteclass.core.module.provisioning;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Signal that a new tenant has been created and needs frontend-instance provisioning.
 *
 * <p>Published by kitehub-subscription {@code AuthService.registerFromBetaInvite} via
 * {@code SubscriptionEventEmitter.emit(instanceId, "TENANT_CREATED", "tenant.created", json)}
 * to the {@code email.exchange} DirectExchange with routing key {@code tenant.created};
 * consumed by {@link TenantCreatedEventConsumer} (Wave provisioning-1 Bucket A — GAP-945)
 * which deserializes this payload and invokes
 * {@link TenantProvisioningSaga#provision(TenantCreatedEvent)}.
 *
 * <p>{@code @Jacksonized} pairs with {@code @Builder} so Jackson can deserialize this
 * immutable {@code @Value} type from the cross-service JSON payload (raw-UTF8 bytes per
 * GAP-925) — without it, deserialization fails (no usable creator).
 *
 * @since 3.22.0 (Wave 3 Sub-PR 3.6); RabbitMQ wiring Wave provisioning-1 Bucket A (GAP-945)
 */
@Value
@Builder
@Jacksonized
public class TenantCreatedEvent {

    String tenantId;
    String slug;
    String audience;
    String tone;
}
