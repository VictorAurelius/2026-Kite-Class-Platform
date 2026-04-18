package com.kitehub.email.listener;

import com.kitehub.email.client.BrandingClient;
import com.kitehub.email.config.BrandingEventsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Evicts the local branding cache when {@code kiteclass-core} publishes a
 * {@code branding.updated} event. The event payload is expected to be a JSON
 * object containing at minimum an {@code instanceId}.
 *
 * @since Wave 4 (GAP-021)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "kitehub.email.branding.rabbit-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class BrandingUpdatedListener {

    private final BrandingClient brandingClient;

    @RabbitListener(queues = BrandingEventsConfig.EMAIL_BRANDING_UPDATED_QUEUE)
    public void onBrandingUpdated(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }

        Object raw = payload.get("instanceId");
        Long instanceId = toLong(raw);
        if (instanceId == null) {
            log.warn("branding.updated received with missing/invalid instanceId: {}", payload);
            return;
        }

        brandingClient.evict(instanceId);
        log.info("Evicted email branding cache for instance={} (event payload={})",
                instanceId, payload);
    }

    private static Long toLong(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
