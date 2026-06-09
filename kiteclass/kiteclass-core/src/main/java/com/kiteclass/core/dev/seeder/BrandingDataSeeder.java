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
import com.kiteclass.core.module.marketing.entity.LandingPage;
import com.kiteclass.core.module.marketing.repository.LandingPageRepository;
import com.kiteclass.core.module.quality.entity.QualityReport;
import com.kiteclass.core.module.quality.entity.QualityReportRepository;
import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
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

    // GAP-805 Bucket A — demo tenant "Sky Education" branding seed.
    // Instance UUID matches the kitehub gateway `instances` row for subdomain
    // `sky-education` (id e8ff87e1…) so browser→gateway→core resolves the demo data
    // (shared-DB + RLS canonical model per ADR-023). Previously a made-up a5e00000…
    // that the gateway never resolved — see 2026-05-29 demo-trio walk align-seed-to-gateway fix.
    // Slug + display name mirror scripts/seed-thesis-demo-tenants.sh tenant_a (Sky Education),
    // proving live UI theme customisation on the multi-tenant demo.
    static final UUID SKY_TENANT_ID = UUID.fromString("e8ff87e1-69fc-4842-a263-7385c68b4ffb");
    static final String SKY_TENANT_SLUG = "sky-education";
    static final String SKY_TENANT_REF = "dev-tenant-sky-education";
    static final String SKY_FRONTEND_URL = "https://sky-education.kite.local";
    static final String SKY_DISPLAY_NAME = "Trung tâm Anh ngữ Sky Education";
    static final String SKY_TAGLINE = "Chắp cánh tương lai Anh ngữ";
    // Warm education palette (orange/amber) — deliberately distinct from the default
    // shadcn blue so the live theme swap is visually obvious during the demo.
    static final String SKY_PRIMARY_COLOR = "#E8590C";   // cam đậm — primary actions
    static final String SKY_SECONDARY_COLOR = "#1B4965";  // xanh navy — headers/footer
    static final String SKY_ACCENT_COLOR = "#FFB703";    // vàng hổ phách — highlights

    // ── Wave landing-100 Bucket G — demo-trio Branding settings seed (GAP-805) ──
    // Three independent instructors proving plan-tier + branding variety per thesis §4.1-4.2
    // (Hình 4.3 / 4.4). Each seeds a `Branding` settings entity (color + tagline + theme JSON)
    // + FrontendInstance + LandingPage hero — directly, NOT via the AI wizard (FULL_AI landing
    // persist breaks per GAP-1021). Banner assets are HTML-composed AI scenes (GAP-810) living in
    // kiteclass-frontend/public/demo-banners/; the seed stores only the URL string.
    // Khánh (§4.1 walkthrough tenant) reuses the existing Sky instance; Hà + Nhì are fresh tenants.

    // Cô Nguyễn Thị Hà — gói Miễn phí, Toán tiểu học, tông xanh dương (template, no AI).
    static final UUID HA_TENANT_ID = UUID.fromString("a1100000-0000-4000-a000-000000000001");
    static final String HA_TENANT_SLUG = "co-ha-toan";
    static final String HA_TENANT_REF = "dev-tenant-co-ha-toan";
    static final String HA_FRONTEND_URL = "https://co-ha-toan.kite.local";
    static final String HA_DISPLAY_NAME = "Lớp Toán cô Nguyễn Thị Hà";
    static final String HA_TAGLINE = "Toán tiểu học vững nền tảng";
    static final String HA_PRIMARY_COLOR = "#2563EB";    // xanh dương
    static final String HA_SECONDARY_COLOR = "#1E40AF";
    static final String HA_ACCENT_COLOR = "#60A5FA";
    static final String HA_BANNER_URL = "/demo-banners/co-ha-toan.png";

    // Thầy Nguyễn Đình Nhì — gói Trả phí, Hóa THCS, tông xanh lá (AI Branding).
    static final UUID NHI_TENANT_ID = UUID.fromString("b1100000-0000-4000-a000-000000000002");
    static final String NHI_TENANT_SLUG = "thay-nhi-hoa";
    static final String NHI_TENANT_REF = "dev-tenant-thay-nhi-hoa";
    static final String NHI_FRONTEND_URL = "https://thay-nhi-hoa.kite.local";
    static final String NHI_DISPLAY_NAME = "Hóa học THCS thầy Nguyễn Đình Nhì";
    static final String NHI_TAGLINE = "Hóa học THCS — học là hiểu";
    static final String NHI_PRIMARY_COLOR = "#16A34A";   // xanh lá
    static final String NHI_SECONDARY_COLOR = "#14532D";
    static final String NHI_ACCENT_COLOR = "#4ADE80";
    static final String NHI_BANNER_URL = "/demo-banners/thay-nhi-hoa.png";

    // Cô Đỗ Lan Khánh — §4.1 walkthrough tenant; reuses the Sky instance, adds a Branding row.
    static final String KHANH_DISPLAY_NAME = "Trung tâm cô Đỗ Lan Khánh";
    static final String KHANH_BANNER_URL = "/demo-banners/co-khanh-phapluat.png";

    private final FrontendInstanceRepository instanceRepo;
    private final BrandingResourceRepository resourceRepo;
    private final QualityReportRepository qualityRepo;
    private final LandingPageRepository landingPageRepository;
    private final BrandingRepository brandingRepository;
    private final OutboxEventWriter outbox;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    /** Triggered after the Spring context is fully initialized. */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Seed each demo tenant under its own TenantContext so the
        // EntityPersistenceListener stamps the correct instance_id per resource.
        seedTenant(DEV_TENANT_ID, DEV_TENANT_SLUG);
        seedTenant(SKY_TENANT_ID, SKY_TENANT_SLUG);
        // Wave landing-100 Bucket G — demo-trio Branding settings (GAP-805).
        seedDemoTrio();
    }

    /**
     * Seeds the demo-trio (Khánh / Hà / Nhì) so each tenant's public homepage renders
     * a distinct branded theme + banner per thesis §4.1-4.2. Idempotent.
     */
    private void seedDemoTrio() {
        seedTrioTenant(new TrioSpec(HA_TENANT_ID, HA_TENANT_SLUG, HA_TENANT_REF, HA_FRONTEND_URL,
                HA_DISPLAY_NAME, HA_TAGLINE, HA_PRIMARY_COLOR, HA_SECONDARY_COLOR, HA_ACCENT_COLOR,
                HA_BANNER_URL,
                "Lấy lại căn bản môn Toán cùng cô Hà",
                "Lộ trình Toán tiểu học bài bản, lớp nhỏ, kèm sát từng học viên.",
                "https://zalo.me/co-ha-toan", "https://facebook.com/cohatoan"));
        seedTrioTenant(new TrioSpec(NHI_TENANT_ID, NHI_TENANT_SLUG, NHI_TENANT_REF, NHI_FRONTEND_URL,
                NHI_DISPLAY_NAME, NHI_TAGLINE, NHI_PRIMARY_COLOR, NHI_SECONDARY_COLOR, NHI_ACCENT_COLOR,
                NHI_BANNER_URL,
                "Hóa học THCS — học là hiểu cùng thầy Nhì",
                "Khóa Hóa học THCS đầy đủ, bộ nhận diện sinh tự động bằng AI Branding.",
                "https://zalo.me/thay-nhi-hoa", "https://facebook.com/thaynhihoa"));
        seedKhanhBranding();
    }

    /** Immutable spec for one demo-trio tenant seed (keeps {@link #seedTrioTenant} param count sane). */
    private record TrioSpec(UUID tenantId, String slug, String tenantRef, String frontendUrl,
                            String displayName, String tagline,
                            String primary, String secondary, String accent, String bannerUrl,
                            String heroTitle, String heroSubtitle, String zaloUrl, String facebookUrl) {
    }

    /**
     * Seeds one demo-trio tenant: DEPLOYED FrontendInstance + Branding settings row
     * + LandingPage hero. Each step is independently idempotent (skip when present /
     * upsert the landing) so re-running on every boot does not duplicate rows.
     *
     * <p>The Branding {@code instanceId} is stamped by {@code EntityPersistenceListener}
     * from the {@link TenantContext} set by the caller — consistent with how the
     * {@link BrandingResource} rows are seeded.
     */
    private void seedTrioTenant(TrioSpec spec) {
        try {
            TenantContext.setCurrentTenant(spec.tenantId());
            transactionTemplate.executeWithoutResult(status -> {
                // 1. FrontendInstance (DEPLOYED) — idempotent by slug.
                if (!instanceRepo.existsBySlugAndDeletedFalse(spec.slug())) {
                    FrontendInstance instance = FrontendInstance.builder()
                            .tenantSlug(spec.tenantRef())
                            .slug(spec.slug())
                            .frontendUrl(spec.frontendUrl())
                            .build();
                    instance.transitionTo(FrontendInstanceStatus.INITIALIZING);
                    instance.transitionTo(FrontendInstanceStatus.GENERATING);
                    instance.transitionTo(FrontendInstanceStatus.DEPLOYED);
                    instanceRepo.save(instance);
                }
                // 2. Branding settings row — idempotent by instance.
                if (!brandingRepository.existsByInstanceIdAndDeletedFalse(spec.tenantId())) {
                    Branding branding = new Branding();
                    branding.setDisplayName(spec.displayName());
                    branding.setTagline(spec.tagline());
                    branding.setPrimaryColor(spec.primary());
                    branding.setSecondaryColor(spec.secondary());
                    branding.setAccentColor(spec.accent());
                    branding.setThemeConfigJson(
                            buildTrioThemeConfigJson(spec.displayName(), spec.tagline(),
                                    spec.primary(), spec.secondary(), spec.accent(),
                                    spec.zaloUrl(), spec.facebookUrl(), spec.frontendUrl()));
                    branding.setLogoUrl(spec.bannerUrl());
                    branding.setZaloUrl(spec.zaloUrl());
                    branding.setFacebookUrl(spec.facebookUrl());
                    branding.setWebsiteUrl(spec.frontendUrl());
                    brandingRepository.save(branding);
                }
                // 3. LandingPage hero — upsert (lazily created on first GET otherwise, BR-MKT-001).
                LandingPage lp = landingPageRepository.findByInstanceIdAndDeletedFalse(spec.tenantId())
                        .orElseGet(() -> {
                            LandingPage created = new LandingPage();
                            created.setInstanceId(spec.tenantId());
                            return created;
                        });
                lp.setHeroTitle(spec.heroTitle());
                lp.setHeroSubtitle(spec.heroSubtitle());
                lp.setHeroImageUrl(spec.bannerUrl());
                lp.setTagline(spec.tagline());
                lp.setPrimaryColor(spec.primary());
                lp.setSecondaryColor(spec.secondary());
                // template_type NOT NULL (DB constraint) — trio đều là GV cá nhân → "personal"
                // ("personal" GV độc lập | "organization" trung tâm, per LandingPage entity §106).
                if (lp.getTemplateType() == null) {
                    lp.setTemplateType("personal");
                }
                landingPageRepository.save(lp);
            });
            log.info("Seeded demo-trio tenant (slug={}, primary={})", spec.slug(), spec.primary());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Seeds cô Đỗ Lan Khánh's Branding settings row on the existing Sky instance
     * (§4.1 walkthrough tenant). The Sky FrontendInstance + LandingPage are seeded
     * elsewhere; here we only add the missing {@link Branding} settings row.
     */
    private void seedKhanhBranding() {
        try {
            TenantContext.setCurrentTenant(SKY_TENANT_ID);
            transactionTemplate.executeWithoutResult(status -> {
                if (brandingRepository.existsByInstanceIdAndDeletedFalse(SKY_TENANT_ID)) {
                    return;
                }
                Branding branding = new Branding();
                branding.setDisplayName(KHANH_DISPLAY_NAME);
                branding.setTagline(SKY_TAGLINE);
                branding.setPrimaryColor(SKY_PRIMARY_COLOR);
                branding.setSecondaryColor(SKY_SECONDARY_COLOR);
                branding.setAccentColor(SKY_ACCENT_COLOR);
                branding.setThemeConfigJson(buildSkyThemeConfigJson());
                branding.setLogoUrl(KHANH_BANNER_URL);
                brandingRepository.save(branding);
            });
            log.info("Seeded Khánh (Sky) Branding settings row (instance={})", SKY_TENANT_ID);
        } finally {
            TenantContext.clear();
        }
    }

    /** Builds the theme-config JSON stored in {@link Branding#getThemeConfigJson()} for a trio tenant. */
    private String buildTrioThemeConfigJson(String displayName, String tagline,
                                            String primary, String secondary, String accent,
                                            String zaloUrl, String facebookUrl, String website) {
        Map<String, Object> theme = new LinkedHashMap<>();
        theme.put("displayName", displayName);
        theme.put("tagline", tagline);

        Map<String, String> cssVars = new LinkedHashMap<>();
        cssVars.put("--brand-primary", primary);
        cssVars.put("--brand-secondary", secondary);
        cssVars.put("--brand-accent", accent);
        theme.put("cssVars", cssVars);

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("zalo", zaloUrl);
        contact.put("facebook", facebookUrl);
        contact.put("website", website);
        theme.put("contact", contact);

        try {
            return objectMapper.writeValueAsString(theme);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize demo-trio theme config", e);
        }
    }

    private void seedTenant(UUID tenantId, String slug) {
        try {
            TenantContext.setCurrentTenant(tenantId);
            transactionTemplate.executeWithoutResult(status -> {
                seed(slug);
                if (SKY_TENANT_SLUG.equals(slug)) {
                    seedSkyLanding(tenantId);
                }
            });
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Seeds the Sky Education landing page hero so the public homepage renders the
     * promo banner (slogan + teacher portrait + CTA). Idempotent: refreshes the demo
     * hero fields on each boot. {@code heroImageUrl} points at a static asset served
     * by the frontend ({@code public/demo/sky/}, local-only/gitignored per GAP-810);
     * the seed stores only the URL string. The landing row is otherwise lazily
     * created on first GET (BR-MKT-001), so we upsert here.
     */
    private void seedSkyLanding(UUID tenantId) {
        LandingPage lp = landingPageRepository.findByInstanceIdAndDeletedFalse(tenantId)
                .orElseGet(() -> {
                    LandingPage created = new LandingPage();
                    created.setInstanceId(tenantId);
                    return created;
                });
        lp.setHeroTitle("Mất gốc tiếng Anh? Đã có cô Khánh");
        lp.setHeroSubtitle("Lộ trình lấy lại căn bản tiếng Anh, học cùng giáo viên tận tâm.");
        lp.setHeroImageUrl("/demo/sky/teacher-do-lan-khanh.webp");
        lp.setTagline(SKY_TAGLINE);
        lp.setPrimaryColor(SKY_PRIMARY_COLOR);
        lp.setSecondaryColor(SKY_SECONDARY_COLOR);
        landingPageRepository.save(lp);
        log.info("Seeded Sky landing hero (instance={})", tenantId);
    }

    /**
     * Seeds the thanglong demo tenant. Kept for backward compatibility with
     * existing tests that call {@code seed()} with no argument.
     *
     * <p>Public for direct invocation in tests. Caller must have a transaction
     * open (or rely on {@link #onApplicationReady()} which wraps via
     * {@link TransactionTemplate}) AND must set {@link TenantContext} to
     * {@link #DEV_TENANT_ID} first.
     */
    @Transactional
    public void seed() {
        seed(DEV_TENANT_SLUG);
    }

    /**
     * Seeds branding for the tenant identified by {@code slug}. Idempotent —
     * skips silently when an instance with the slug already exists.
     *
     * <p>Caller MUST have already set {@link TenantContext} to the matching tenant
     * id so the {@code EntityPersistenceListener} stamps the right instance_id.
     */
    @Transactional
    public void seed(String slug) {
        if (instanceRepo.existsBySlugAndDeletedFalse(slug)) {
            log.info("Dev branding seed already present (slug={}). Skipping.", slug);
            return;
        }

        FrontendInstance instance = instanceRepo.save(buildInstance(slug));
        resourceRepo.saveAll(buildResources(slug));
        qualityRepo.save(buildQualityReport(instance));
        emitOutboxEvent(instance);

        log.info("Seeded dev branding: instance id={}, slug={}, brandingVersion={}",
                instance.getId(), instance.getSlug(), instance.getBrandingVersion());
    }

    private FrontendInstance buildInstance(String slug) {
        boolean isSky = SKY_TENANT_SLUG.equals(slug);
        FrontendInstance instance = FrontendInstance.builder()
                .tenantSlug(isSky ? SKY_TENANT_REF : DEV_TENANT_REF)
                .slug(slug)
                .frontendUrl(isSky ? SKY_FRONTEND_URL : DEV_FRONTEND_URL)
                .build();
        instance.transitionTo(FrontendInstanceStatus.INITIALIZING);
        instance.transitionTo(FrontendInstanceStatus.GENERATING);
        instance.transitionTo(FrontendInstanceStatus.DEPLOYED);
        return instance;
    }

    private List<BrandingResource> buildResources(String slug) {
        if (SKY_TENANT_SLUG.equals(slug)) {
            return buildSkyResources();
        }
        return buildThangLongResources();
    }

    private List<BrandingResource> buildThangLongResources() {
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

    /**
     * Sky Education resources. The LOGO resource carries the theme config
     * (display name, tagline, custom CSS palette) in its {@code metadata} jsonb
     * column — the same slot the AI branding pipeline writes theme vars into —
     * so the FE renders Sky's warm orange/amber palette instead of the default
     * shadcn blue. Logo asset path is seeded directly (no upload controller round-trip
     * per GAP-804 / GAP-798b).
     */
    private List<BrandingResource> buildSkyResources() {
        BrandingResource logo = BrandingResource.builder()
                .type(ResourceType.LOGO)
                .category(ResourceCategory.STATIC)
                .storageUrl("/mocks/assets/logo-sky-education.png")
                .metadata(buildSkyThemeConfigJson())
                .build();
        logo.validateInvariants();

        BrandingResource banner = BrandingResource.builder()
                .type(ResourceType.BANNER)
                .category(ResourceCategory.TEMPLATE)
                .storageUrl("/mocks/assets/banner-sky-education.svg")
                .templateId(1L)
                .build();
        banner.validateInvariants();

        BrandingResource hero = BrandingResource.builder()
                .type(ResourceType.HERO)
                .category(ResourceCategory.FULL_AI)
                .storageUrl("/mocks/assets/hero-sky-education.png")
                .aiJobId(UUID.randomUUID())
                .build();
        hero.validateInvariants();

        return List.of(logo, banner, hero);
    }

    /**
     * Builds the Sky Education theme config JSON stored in the LOGO resource
     * metadata: display name, tagline, CSS palette + VN contact links.
     */
    private String buildSkyThemeConfigJson() {
        Map<String, Object> theme = new LinkedHashMap<>();
        theme.put("displayName", SKY_DISPLAY_NAME);
        theme.put("tagline", SKY_TAGLINE);

        Map<String, String> cssVars = new LinkedHashMap<>();
        cssVars.put("--brand-primary", SKY_PRIMARY_COLOR);
        cssVars.put("--brand-secondary", SKY_SECONDARY_COLOR);
        cssVars.put("--brand-accent", SKY_ACCENT_COLOR);
        theme.put("cssVars", cssVars);

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("zalo", "https://zalo.me/sky-education");
        contact.put("facebook", "https://facebook.com/skyedu.vn");
        contact.put("website", "https://sky-education.kite.local");
        theme.put("contact", contact);

        try {
            return objectMapper.writeValueAsString(theme);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Sky theme config", e);
        }
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
