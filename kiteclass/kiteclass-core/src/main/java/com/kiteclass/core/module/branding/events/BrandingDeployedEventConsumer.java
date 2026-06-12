package com.kiteclass.core.module.branding.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.marketing.service.LandingPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Consumes the cross-service {@code branding.deployed} event from kitehub-branding (GAP-1213)
 * and applies the deployed AI-branding theme to the tenant landing page — closing the broken
 * last mile where a wizard "Deploy thành công" never changed the real public per-tenant landing.
 *
 * <p>Bound to {@link BrandingEventsConfig#DEPLOYED_QUEUE} on the {@code branding.events} topic
 * exchange (routing key {@code branding.deployed}). Receives the raw {@link Message} + decodes
 * UTF-8 (NOT {@code @RabbitListener(String)} — the shared Jackson converter would try to map the
 * {@code application/json} body onto a String / producer class and throw, killing the listener;
 * GAP-1045/GAP-925 precedent) then parses it here.</p>
 *
 * <p>The consumer thread has no request-scoped {@link TenantContext}; LandingPage/Branding are
 * RLS-scoped by {@code instance_id}, so the tenant is established for the apply call then cleared
 * (ThreadLocal hygiene). Malformed payloads + apply failures are swallowed + ACKed (no
 * poison-message requeue loop — same handling as {@code TenantCreatedEventConsumer}); a missed
 * apply only delays a re-deployable theme.</p>
 *
 * @since GAP-1213 (Wave branding-100 Bucket C)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BrandingDeployedEventConsumer {

    private final ObjectMapper objectMapper;
    private final LandingPageService landingPageService;

    @RabbitListener(queues = BrandingEventsConfig.DEPLOYED_QUEUE)
    public void handle(Message message) {
        handlePayload(new String(message.getBody(), StandardCharsets.UTF_8));
    }

    /** Package-visible so unit tests can exercise the apply logic without an AMQP {@link Message}. */
    void handlePayload(String payloadJson) {
        BrandingDeployedEvent event;
        try {
            event = objectMapper.readValue(payloadJson, BrandingDeployedEvent.class);
        } catch (JsonProcessingException ex) {
            log.error("[branding] failed to deserialize branding.deployed payload — dropping: {}",
                    ex.getMessage());
            return;
        }

        if (event.tenantId() == null || event.tenantId().isBlank()) {
            log.error("[branding] branding.deployed missing tenantId — dropping");
            return;
        }
        UUID tenantUuid;
        try {
            tenantUuid = UUID.fromString(event.tenantId().trim());
        } catch (RuntimeException ex) {
            log.error("[branding] branding.deployed non-UUID tenantId='{}' — dropping", event.tenantId());
            return;
        }

        log.info("[branding] received branding.deployed tenant={} slug={} version={}",
                event.tenantId(), event.slug(), event.brandingVersion());

        TenantContext.setCurrentTenant(tenantUuid);
        try {
            boolean changed = landingPageService.applyDeployedBranding(
                    tenantUuid,
                    event.primaryColor(),
                    event.secondaryColor(),
                    event.logoUrl(),
                    event.brandingVersion());
            log.info("[branding] applied branding.deployed tenant={} changed={}", tenantUuid, changed);
        } catch (RuntimeException ex) {
            // Swallow + ACK: a failed apply must not poison-loop the queue. The deploy can be
            // re-published (idempotent on brandingVersion) without re-provisioning.
            log.error("[branding] failed to apply branding.deployed tenant={} — ACK: {}",
                    tenantUuid, ex.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}
