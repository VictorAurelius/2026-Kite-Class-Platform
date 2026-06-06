package com.kiteclass.core.module.provisioning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Publishes the {@code tenant.deployed} cross-service event after provisioning completes
 * (Wave provisioning-1 Bucket C — GAP-948).
 *
 * <p>This is <strong>dedicated dispatcher infrastructure</strong> (per
 * {@code design-patterns.md} §3.5.1 Exception D) — its single purpose is the broker
 * handoff. The caller ({@link TenantCreatedEventConsumer}) only invokes it AFTER
 * {@code TenantProvisioningSaga.provision(...)} has already persisted the FrontendInstance
 * as DEPLOYED, so the domain change is durable before this publish. It contains no business
 * logic — just serialization + send.
 *
 * <p><strong>Wire-format (GAP-925):</strong> sends a raw-UTF8 JSON body with content-type
 * {@code application/json} via {@code rabbitTemplate.send(Message)} (NOT {@code convertAndSend},
 * which would double-encode the String / leak a cross-package {@code __TypeId__} header). This
 * mirrors {@code SubscriptionEventEmitter}'s {@code tenant.created} producer so the
 * kitehub-subscription consumer can read it as a plain JSON String.
 *
 * @since Wave provisioning-1 Bucket C (GAP-948)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantReadyNotifier {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Best-effort publish of {@code tenant.deployed}. Never throws — provisioning already
     * succeeded; a publish miss only delays the tenant-ready email (recoverable), it must
     * NOT fail the consumer (which would poison-loop a DEPLOYED tenant).
     *
     * @param tenantId           Instance UUID (as String) from the originating event
     * @param slug               provisioned subdomain slug
     * @param frontendInstanceId FrontendInstance id returned by the saga
     */
    public void notifyDeployed(String tenantId, String slug, Long frontendInstanceId) {
        TenantDeployedEvent event = new TenantDeployedEvent(tenantId, slug, frontendInstanceId);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            log.error("[provisioning] failed to serialize TenantDeployedEvent tenant={}: {}",
                    tenantId, ex.getMessage());
            return;
        }

        try {
            Message message = MessageBuilder
                    .withBody(payload.getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .build();
            rabbitTemplate.send(RabbitConfig.EMAIL_EXCHANGE,
                    RabbitConfig.TENANT_DEPLOYED_ROUTING_KEY, message);
            log.info("[provisioning] published tenant.deployed tenant={} slug={} frontendInstanceId={}",
                    tenantId, slug, frontendInstanceId);
        } catch (Exception ex) {
            log.warn("[provisioning] tenant.deployed publish failed tenant={} slug={} — "
                            + "tenant-ready email skipped (provisioning unaffected): {}",
                    tenantId, slug, ex.getMessage());
        }
    }
}
