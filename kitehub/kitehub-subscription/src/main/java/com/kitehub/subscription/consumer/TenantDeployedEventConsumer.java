package com.kitehub.subscription.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.EmailQueueConfig;
import com.kitehub.subscription.dto.TenantDeployedEvent;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.InstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumes {@code tenant.deployed} events from kiteclass-core and dispatches the
 * tenant-ready email (Wave provisioning-1 Bucket C — GAP-948).
 *
 * <p><strong>Why here, not in kiteclass-core:</strong> the owner email lives in
 * kitehub-subscription's {@code Instance} table, NOT in kiteclass-core (the
 * {@code TenantCreatedEvent} payload carries only tenantId/slug/audience/tone — no
 * recipient). So the kiteclass-core consumer can only <em>trigger</em> the email by
 * publishing {@code tenant.deployed}; the actual resolve-owner-and-send happens here,
 * co-located with {@link EmailServiceClient} + {@link InstanceRepository}.
 *
 * <p><strong>Wire-format (GAP-925 + GAP-1045 fix):</strong> kiteclass-core publishes raw-UTF8
 * JSON bytes (content-type {@code application/json}). The shared listener container factory uses a
 * {@code Jackson2JsonMessageConverter}, which would throw a fatal {@code MessageConversionException}
 * if this listener took a {@code String} param (Jackson cannot map a JSON object onto {@code String}).
 * So we take the raw {@link Message} (bypassing the converter) and decode + parse it ourselves in
 * {@link #handlePayload(String)}.
 *
 * <p><strong>Failure handling:</strong> malformed payload OR unresolvable tenant are
 * swallowed + ACKed (broken/orphan messages must not requeue-loop; DLQ wiring on
 * {@code tenant.deployed.dlq} covers retry-exhausted poison). The actual email delivery
 * retry/DLQ is the existing {@code email.send} → 3-retry → {@code email.dlq} path, since
 * {@link EmailServiceClient#sendTenantReadyEmail} routes through {@code dispatchEmail}.
 *
 * @since Wave provisioning-1 Bucket C (GAP-948)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kitehub.email.use-queue", havingValue = "true", matchIfMissing = true)
public class TenantDeployedEventConsumer {

    private final ObjectMapper objectMapper;
    private final InstanceRepository instanceRepository;
    private final EmailServiceClient emailServiceClient;
    private final InstanceService instanceService;

    @RabbitListener(queues = EmailQueueConfig.TENANT_DEPLOYED_QUEUE)
    public void handle(Message message) {
        handlePayload(new String(message.getBody(), StandardCharsets.UTF_8));
    }

    /**
     * Parse + dispatch the decoded JSON payload. Package-visible so unit tests can exercise the
     * resolve-owner-and-send logic directly without constructing an AMQP {@link Message}.
     */
    void handlePayload(String payloadJson) {
        TenantDeployedEvent event;
        try {
            event = objectMapper.readValue(payloadJson, TenantDeployedEvent.class);
        } catch (JsonProcessingException ex) {
            log.error("[provisioning] failed to deserialize TenantDeployedEvent — dropping: {}",
                    ex.getMessage());
            return; // broken payload — swallow (DLQ covers retry-exhausted poison)
        }

        UUID instanceId = parseInstanceId(event.tenantId());
        if (instanceId == null) {
            log.warn("[provisioning] tenant.deployed has unparseable tenantId='{}' slug={} — dropping",
                    event.tenantId(), event.slug());
            return;
        }

        Optional<Instance> instanceOpt = instanceRepository.findById(instanceId);
        if (instanceOpt.isEmpty()) {
            log.warn("[provisioning] tenant.deployed for unknown instance={} slug={} — dropping",
                    instanceId, event.slug());
            return;
        }

        Instance instance = instanceOpt.get();

        // GAP-945 follow-up: flip lifecycle status PENDING → TRIAL now that kiteclass-core has
        // deployed the tenant. Best-effort — a status-flip failure must not abort the email send
        // (and runs BEFORE the contactEmail guard so a missing email never leaves status stuck).
        try {
            instanceService.markProvisioned(instanceId);
        } catch (Exception ex) {
            log.warn("[provisioning] markProvisioned failed for instance={} slug={} — {}",
                    instanceId, event.slug(), ex.getMessage());
        }

        if (!StringUtils.hasText(instance.getContactEmail())) {
            log.warn("[provisioning] instance={} slug={} has no contactEmail — cannot send tenant-ready email",
                    instanceId, event.slug());
            return;
        }

        log.info("[provisioning] tenant.deployed instance={} slug={} → sending tenant-ready email to {}",
                instanceId, event.slug(), instance.getContactEmail());

        // Best-effort send (EmailServiceClient swallows internally + the email.send queue
        // owns the 3-retry + email.dlq reliability path).
        emailServiceClient.sendTenantReadyEmail(
                instance.getId(),
                instance.getContactEmail(),
                instance.getOrganizationName(),
                instance.getSubdomain());
    }

    private UUID parseInstanceId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return UUID.fromString(tenantId.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
