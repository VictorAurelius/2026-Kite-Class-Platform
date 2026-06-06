package com.kiteclass.core.module.provisioning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.config.RabbitConfig;
import com.kiteclass.core.common.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
    private final TenantReadyNotifier tenantReadyNotifier;

    /**
     * RabbitMQ entry point. Receives the raw {@link Message} and decodes the body as UTF-8 —
     * NOT a {@code @RabbitListener(String)} (GAP-1045 fix): the shared
     * {@code rabbitListenerContainerFactory} uses a {@link org.springframework.amqp.support.converter.Jackson2JsonMessageConverter},
     * which tries to map the {@code application/json} body onto the {@code String} parameter type and
     * throws a fatal {@code MessageConversionException} ("message rejected; dropped") — silently killing
     * the saga. The producer ({@code SubscriptionEventEmitter.emit}) sends raw UTF-8 JSON bytes via
     * {@code rabbitTemplate.send(new Message(...))}, so we take the raw {@code Message} (which bypasses
     * the converter) and parse it ourselves in {@link #handlePayload(String)}.
     */
    @RabbitListener(queues = RabbitConfig.TENANT_CREATED_QUEUE)
    public void handle(Message message) {
        handlePayload(new String(message.getBody(), StandardCharsets.UTF_8));
    }

    /**
     * Parse + dispatch the decoded JSON payload. Package-visible so unit tests can exercise the
     * saga-orchestration logic directly without constructing an AMQP {@link Message}.
     */
    void handlePayload(String payloadJson) {
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

        // GAP-1047: the saga runs in a RabbitMQ consumer thread with no request-scoped
        // TenantContext. FrontendInstance + every branding entity it persists are RLS-scoped —
        // BaseEntity.instanceId is auto-populated by EntityPersistenceListener from the current
        // TenantContext. Without it, instance_id is NULL → NOT NULL violation kills initiate() and
        // the saga never provisions. The event's tenantId IS the subscription Instance UUID (the RLS
        // tenant), so establish it for the whole saga, then clear (ThreadLocal hygiene).
        UUID tenantUuid;
        try {
            tenantUuid = UUID.fromString(event.getTenantId().trim());
        } catch (RuntimeException ex) {
            log.error("[provisioning] tenant.created has non-UUID tenantId='{}' slug={} — dropping",
                    event.getTenantId(), event.getSlug());
            return; // unusable tenant id — swallow (cannot scope RLS context)
        }

        TenantContext.setCurrentTenant(tenantUuid);
        try {
            Long instanceId = saga.provision(event);
            log.info("[provisioning] saga completed tenant={} frontendInstanceId={}",
                    event.getTenantId(), instanceId);

            // Tenant DEPLOYED → trigger the tenant-ready email (Wave provisioning-1 Bucket C,
            // GAP-948). The consumer is the clean "provision succeeded → notify" orchestration
            // boundary. The owner email lives in kitehub-subscription (not in this event), so we
            // only PUBLISH tenant.deployed here; kitehub-subscription resolves the owner + sends.
            // Best-effort: notifier never throws (a publish miss only delays a recoverable email).
            tenantReadyNotifier.notifyDeployed(event.getTenantId(), event.getSlug(), instanceId);
        } catch (RuntimeException sagaFailure) {
            // Saga already compensated (markFailed) + rethrew. ACK to avoid poison-message
            // requeue loop; FAILED tenant is recovered via admin force-retry (GAP-953).
            log.error("[provisioning] saga failed tenant={} slug={} — marked FAILED, ACK (admin retry via GAP-953): {}",
                    event.getTenantId(), event.getSlug(), sagaFailure.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}
