package com.kiteclass.core.module.branding.events;

import java.io.Serializable;
import java.time.Instant;

/**
 * Payload published on exchange {@code branding.events} with routing key
 * {@code branding.updated} after a successful branding mutation.
 *
 * <p>Consumers (e.g. kitehub-email, kitehub-gateway) use this to evict caches.
 *
 * @param instanceId numeric branding row ID in kiteclass-core (NOT tenant UUID)
 * @param tenantId   tenant (instance) UUID as string
 * @param version    monotonic version number after the update
 * @param updatedAt  instant the update was committed
 * @since Wave 4 (GAP-021)
 */
public record BrandingUpdatedEvent(
        Long instanceId,
        String tenantId,
        Integer version,
        Instant updatedAt) implements Serializable {

    private static final long serialVersionUID = 1L;
}
