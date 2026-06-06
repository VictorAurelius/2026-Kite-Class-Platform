package com.kiteclass.core.module.provisioning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer wiring the previously-orphan {@link TenantProvisioningSaga} into the
 * cross-service tenant-creation flow (Wave provisioning-1 Bucket A — GAP-945).
 *
 * <p><strong>The keystone fix:</strong> before this consumer existed, {@code TenantProvisioningSaga}
 * had no {@code @RabbitListener} — kitehub-subscription created the {@code Instance} but the
 * KiteClass {@code FrontendInstance} was never provisioned, leaving the tenant stuck
 * {@code INITIALIZING}. This consumer binds the saga to the {@code tenant.created.queue}
 * (see {@link RabbitConfig#tenantCreatedQueue()} bound to the {@code email.exchange}
 * DirectExchange with routing key {@code tenant.created}).
 *
 * <p><strong>Payload contract (frozen here, GAP-945):</strong> kitehub-subscription publishes a
 * pre-serialized {@link TenantCreatedEvent} JSON string via
 * {@code SubscriptionEventEmitter.emit(instanceId, "TENANT_CREATED", "tenant.created", json)}.
 * The emitter builds the AMQP {@code Message} with raw UTF-8 bytes + {@code Content-Type:
 * application/json} (GAP-925) so this {@code @RabbitListener(String)} receives the JSON object
 * directly — NOT a double-encoded quoted string.
 *
 * <p><strong>Failure handling:</strong> {@code saga.provision(...)} compensates internally
 * ({@code markFailed}) then rethrows. We catch and ACK rather than letting the exception
 * propagate, because the broker would otherwise requeue and re-provision a tenant the saga
 * already persisted as {@code FAILED} — an idempotency-violating poison-message loop. Retry of
 * a FAILED tenant is admin-driven (GAP-953 force-retry endpoint), not broker auto-requeue, per
 * the saga's "each lifecycle step is an independent persisted transaction to drive retry logic"
 * design. Malformed payloads are likewise swallowed (broken JSON must not clog the queue —
 * dead-letter handling is GAP-948/GAP-952 scope).
 *
 * @since Wave provisioning-1 Bucket A (GAP-945)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCreatedEventConsumer {

    private final ObjectMapper objectMapper;
    private final TenantProvisioningSaga saga;

    @RabbitListener(queues = RabbitConfig.TENANT_CREATED_QUEUE)
    public void handle(String payloadJson) {
        TenantCreatedEvent event;
        try {
            event = objectMapper.readValue(payloadJson, TenantCreatedEvent.class);
        } catch (JsonProcessingException ex) {
            log.error("[provisioning] failed to deserialize TenantCreatedEvent payload — dropping: {}",
                    ex.getMessage());
            return; // broken payload — swallow (DLQ wiring = GAP-948/GAP-952)
        }

        log.info("[provisioning] received tenant.created tenant={} slug={}",
                event.getTenantId(), event.getSlug());

        try {
            Long instanceId = saga.provision(event);
            log.info("[provisioning] saga completed tenant={} frontendInstanceId={}",
                    event.getTenantId(), instanceId);
        } catch (RuntimeException sagaFailure) {
            // Saga already compensated (markFailed) + rethrew. ACK to avoid poison-message
            // requeue loop; FAILED tenant is recovered via admin force-retry (GAP-953).
            log.error("[provisioning] saga failed tenant={} slug={} — marked FAILED, ACK (admin retry via GAP-953): {}",
                    event.getTenantId(), event.getSlug(), sagaFailure.getMessage());
        }
    }
}
