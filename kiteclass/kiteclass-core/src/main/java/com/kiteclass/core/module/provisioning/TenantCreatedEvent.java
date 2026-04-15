package com.kiteclass.core.module.provisioning;

import lombok.Builder;
import lombok.Value;

/**
 * Signal that a new tenant has been created and needs frontend-instance provisioning.
 *
 * <p>Published by KiteHub platform onboarding; consumed by {@link TenantProvisioningSaga}.
 * At present the saga is invoked directly via
 * {@link TenantProvisioningSaga#provision(TenantCreatedEvent)}; Spring's
 * {@code @EventListener} / RabbitMQ consumer wiring lands alongside the outbox
 * RabbitMQ dispatcher in a later Sub-PR.
 *
 * @since 3.22.0 (Wave 3 Sub-PR 3.6)
 */
@Value
@Builder
public class TenantCreatedEvent {

    String tenantId;
    String slug;
    String audience;
    String tone;
}
