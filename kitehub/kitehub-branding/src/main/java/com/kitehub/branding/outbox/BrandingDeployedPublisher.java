package com.kitehub.branding.outbox;

import com.kitehub.branding.config.RabbitMQConfig;
import com.kitehub.branding.lifecycle.entity.BrandingInstanceState;
import com.kitehub.branding.lifecycle.repository.BrandingInstanceStateRepository;
import com.kitehub.branding.wizard.dto.BrandColours;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes the cross-service {@code branding.deployed} event (GAP-1213) when a wizard deploy
 * reaches DEPLOYED, so kiteclass-core applies the generated theme/assets onto the tenant
 * landing page — closing the broken last mile where "Deploy thành công" never changed the
 * real per-tenant landing.
 *
 * <p>Runs in its OWN physical transaction ({@code REQUIRES_NEW}) so the outbox-row write
 * commits atomically per {@link BrandingEventEmitter} contract WITHOUT being able to poison
 * the caller (the async mock-provision flow has no surrounding transaction). The emitter does
 * outbox-first + best-effort fast-path publish per {@code design-patterns.md} §3.5.1
 * Exception A — outbox is the reliability net.</p>
 *
 * @since GAP-1213 (Wave branding-100 Bucket C)
 */
@Slf4j
@Service
public class BrandingDeployedPublisher {

    private final BrandingEventEmitter eventEmitter;
    private final BrandingInstanceStateRepository stateRepository;

    public BrandingDeployedPublisher(BrandingEventEmitter eventEmitter,
                                     BrandingInstanceStateRepository stateRepository) {
        this.eventEmitter = eventEmitter;
        this.stateRepository = stateRepository;
    }

    /**
     * Emit {@code branding.deployed} for a freshly-deployed instance.
     *
     * @param instanceId  tenant instance UUID (the RLS tenant in kiteclass-core)
     * @param slug        tenant slug (observability + landing link)
     * @param frontendUrl resolved landing URL ({@code https://{slug}.kitehub.me} mock default)
     * @param colours     derived brand colours (primary/secondary/accent map onto landing theme)
     * @param logoUrl     uploaded/generated logo URL (nullable)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishDeployed(UUID instanceId, String slug, String frontendUrl,
                                BrandColours colours, String logoUrl) {
        if (instanceId == null) {
            log.warn("branding.deployed skipped — null instanceId");
            return;
        }
        int brandingVersion = stateRepository.findById(instanceId)
                .map(BrandingInstanceState::getBrandingVersion)
                .filter(v -> v != null)
                .orElse(1);

        BrandingDeployedEvent event = new BrandingDeployedEvent(
                instanceId.toString(),
                slug,
                frontendUrl,
                colours == null ? null : colours.primary(),
                colours == null ? null : colours.secondary(),
                colours == null ? null : colours.accent(),
                logoUrl,
                brandingVersion,
                Instant.now().toString());

        eventEmitter.emit(
                instanceId,
                instanceId,
                "branding.deployed",
                RabbitMQConfig.BRANDING_EVENTS_EXCHANGE,
                RabbitMQConfig.BRANDING_DEPLOYED_ROUTING_KEY,
                event);

        log.info("Published branding.deployed instance={} slug={} version={} frontendUrl={}",
                instanceId, slug, brandingVersion, frontendUrl);
    }
}
