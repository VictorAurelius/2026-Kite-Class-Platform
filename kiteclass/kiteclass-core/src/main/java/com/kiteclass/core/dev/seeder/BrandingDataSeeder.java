package com.kiteclass.core.dev.seeder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.quality.entity.QualityReport;
import com.kiteclass.core.module.quality.entity.QualityReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Idempotently seeds the AI Branding demo dataset when the {@code dev} profile
 * is active: 1 DEPLOYED {@link FrontendInstance}, 3 {@link BrandingResource}
 * rows (one per {@link ResourceCategory}), 1 {@link QualityReport}, and
 * 1 outbox event. The dataset matches the wave plan §7.4 and lets the
 * frontend wizard demo run end-to-end without hitting Ollama.
 *
 * <p>Skips silently if the dev tenant slug already has an instance — safe to
 * re-run on every boot.
 *
 * <p>Tracking: GAP-235 Sub-PR F.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class BrandingDataSeeder {

    static final UUID DEV_TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final String DEV_TENANT_SLUG = "thanglong";
    static final String DEV_TENANT_REF = "dev-tenant-thanglong";
    static final String DEV_FRONTEND_URL = "https://thanglong.kite.local";

    private final FrontendInstanceRepository instanceRepo;
    private final BrandingResourceRepository resourceRepo;
    private final QualityReportRepository qualityRepo;
    private final OutboxEventWriter outbox;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    /** Triggered after the Spring context is fully initialized. */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            TenantContext.setCurrentTenant(DEV_TENANT_ID);
            transactionTemplate.executeWithoutResult(status -> seed());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Public for direct invocation in tests. Caller must have a transaction
     * open (or rely on {@link #onApplicationReady()} which wraps via
     * {@link TransactionTemplate}).
     */
    @Transactional
    public void seed() {
        if (instanceRepo.existsBySlugAndDeletedFalse(DEV_TENANT_SLUG)) {
            log.info("Dev branding seed already present (slug={}). Skipping.", DEV_TENANT_SLUG);
            return;
        }

        FrontendInstance instance = instanceRepo.save(buildInstance());
        resourceRepo.saveAll(buildResources());
        qualityRepo.save(buildQualityReport(instance));
        emitOutboxEvent(instance);

        log.info("Seeded dev branding: instance id={}, slug={}, brandingVersion={}",
                instance.getId(), instance.getSlug(), instance.getBrandingVersion());
    }

    private FrontendInstance buildInstance() {
        FrontendInstance instance = FrontendInstance.builder()
                .tenantId(DEV_TENANT_REF)
                .slug(DEV_TENANT_SLUG)
                .frontendUrl(DEV_FRONTEND_URL)
                .build();
        instance.transitionTo(FrontendInstanceStatus.INITIALIZING);
        instance.transitionTo(FrontendInstanceStatus.GENERATING);
        instance.transitionTo(FrontendInstanceStatus.DEPLOYED);
        return instance;
    }

    private List<BrandingResource> buildResources() {
        BrandingResource logo = BrandingResource.builder()
                .type(ResourceType.LOGO)
                .category(ResourceCategory.STATIC)
                .storageUrl("/mocks/assets/logo-thanglong.png")
                .build();
        logo.validateInvariants();

        BrandingResource banner = BrandingResource.builder()
                .type(ResourceType.BANNER)
                .category(ResourceCategory.TEMPLATE)
                .storageUrl("/mocks/assets/banner-thanglong.svg")
                .templateId(1L)
                .build();
        banner.validateInvariants();

        BrandingResource hero = BrandingResource.builder()
                .type(ResourceType.HERO)
                .category(ResourceCategory.FULL_AI)
                .storageUrl("/mocks/assets/hero-thanglong.png")
                .aiJobId(UUID.randomUUID())
                .build();
        hero.validateInvariants();

        return List.of(logo, banner, hero);
    }

    private QualityReport buildQualityReport(FrontendInstance instance) {
        return QualityReport.builder()
                .targetInstanceId(instance.getId())
                .brandingVersion(instance.getBrandingVersion())
                .score(85)
                .passed(true)
                .contrastScore(85)
                .cssVarsScore(90)
                .assetUrlsScore(80)
                .visualRegressionScore(82)
                .logoPlacementScore(88)
                .build();
    }

    private void emitOutboxEvent(FrontendInstance instance) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instanceId", instance.getId());
        payload.put("slug", instance.getSlug());
        payload.put("brandingVersion", instance.getBrandingVersion());
        payload.put("deployedAt", instance.getDeployedAt() == null ? null : instance.getDeployedAt().toString());
        try {
            outbox.enqueue(
                    "branding.updated",
                    "FrontendInstance",
                    String.valueOf(instance.getId()),
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize dev seed outbox payload", e);
        }
    }
}
