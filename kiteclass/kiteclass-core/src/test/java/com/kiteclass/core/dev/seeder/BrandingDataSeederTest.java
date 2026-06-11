package com.kiteclass.core.dev.seeder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.outbox.OutboxEvent;
import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.marketing.entity.LandingPage;
import com.kiteclass.core.module.marketing.repository.LandingPageRepository;
import com.kiteclass.core.module.quality.entity.QualityReport;
import com.kiteclass.core.module.quality.entity.QualityReportRepository;
import com.kiteclass.core.module.settings.entity.Branding;
import org.springframework.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandingDataSeederTest {

    @Mock private FrontendInstanceRepository instanceRepo;
    @Mock private BrandingResourceRepository resourceRepo;
    @Mock private QualityReportRepository qualityRepo;
    @Mock private LandingPageRepository landingPageRepository;
    @Mock private BrandingRepository brandingRepository;
    @Mock private OutboxEventWriter outbox;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private org.springframework.cache.CacheManager cacheManager;

    private ObjectMapper objectMapper;
    private BrandingDataSeeder seeder;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        seeder = new BrandingDataSeeder(
                instanceRepo,
                resourceRepo,
                qualityRepo,
                landingPageRepository,
                brandingRepository,
                outbox,
                objectMapper,
                transactionTemplate,
                cacheManager);
    }

    @Test
    @DisplayName("seed() is idempotent — skips when dev tenant already present")
    void seedSkipsWhenInstanceExists() {
        when(instanceRepo.existsBySlugAndDeletedFalse(BrandingDataSeeder.DEV_TENANT_SLUG))
                .thenReturn(true);

        seeder.seed();

        verify(instanceRepo, never()).save(any());
        verifyNoInteractions(resourceRepo, qualityRepo, outbox);
    }

    @Test
    @DisplayName("seed() saves 1 DEPLOYED instance + 3 resources (one per category) + 1 quality report + 1 outbox event")
    void seedPersistsExpectedEntitiesWhenAbsent() {
        when(instanceRepo.existsBySlugAndDeletedFalse(BrandingDataSeeder.DEV_TENANT_SLUG))
                .thenReturn(false);
        when(instanceRepo.save(any(FrontendInstance.class)))
                .thenAnswer(invocation -> {
                    FrontendInstance saved = invocation.getArgument(0);
                    saved.setId(42L);
                    return saved;
                });

        seeder.seed();

        ArgumentCaptor<FrontendInstance> instanceCaptor = ArgumentCaptor.forClass(FrontendInstance.class);
        verify(instanceRepo).save(instanceCaptor.capture());
        FrontendInstance instance = instanceCaptor.getValue();
        assertThat(instance.getStatus()).isEqualTo(FrontendInstanceStatus.DEPLOYED);
        assertThat(instance.getBrandingVersion()).isEqualTo(1);
        assertThat(instance.getDeployedAt()).isNotNull();
        assertThat(instance.getSlug()).isEqualTo(BrandingDataSeeder.DEV_TENANT_SLUG);

        ArgumentCaptor<List<BrandingResource>> resourceCaptor = ArgumentCaptor.captor();
        verify(resourceRepo).saveAll(resourceCaptor.capture());
        List<BrandingResource> resources = resourceCaptor.getValue();
        assertThat(resources).hasSize(3);
        assertThat(resources)
                .extracting(BrandingResource::getCategory)
                .containsExactlyInAnyOrder(ResourceCategory.STATIC, ResourceCategory.TEMPLATE, ResourceCategory.FULL_AI);
        assertThat(resources)
                .extracting(BrandingResource::getType)
                .containsExactlyInAnyOrder(ResourceType.LOGO, ResourceType.BANNER, ResourceType.HERO);

        ArgumentCaptor<QualityReport> reportCaptor = ArgumentCaptor.forClass(QualityReport.class);
        verify(qualityRepo).save(reportCaptor.capture());
        QualityReport report = reportCaptor.getValue();
        assertThat(report.getScore()).isEqualTo(85);
        assertThat(report.getPassed()).isTrue();
        assertThat(report.getTargetInstanceId()).isEqualTo(42L);
        assertThat(report.getBrandingVersion()).isEqualTo(1);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outbox, times(1)).enqueue(
                eq("branding.updated"),
                eq("FrontendInstance"),
                eq("42"),
                payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .contains("\"instanceId\":42")
                .contains("\"slug\":\"thanglong\"")
                .contains("\"brandingVersion\":1")
                .contains("\"deployedAt\":");
    }

    @Test
    @DisplayName("BrandingResource invariants pass for all 3 seeded categories")
    void seededResourcesPassCategoryInvariants() {
        when(instanceRepo.existsBySlugAndDeletedFalse(anyString())).thenReturn(false);
        when(instanceRepo.save(any(FrontendInstance.class)))
                .thenAnswer(invocation -> {
                    FrontendInstance i = invocation.getArgument(0);
                    i.setId(1L);
                    return i;
                });

        // If validateInvariants() throws, seed() propagates and the test fails.
        seeder.seed();

        ArgumentCaptor<List<BrandingResource>> resourceCaptor = ArgumentCaptor.captor();
        verify(resourceRepo).saveAll(resourceCaptor.capture());
        for (BrandingResource r : resourceCaptor.getValue()) {
            r.validateInvariants(); // re-assert, no throw
        }
    }

    @Test
    @DisplayName("outbox.enqueue is invoked exactly once per seed run")
    void outboxEnqueuedOnce() {
        when(instanceRepo.existsBySlugAndDeletedFalse(anyString())).thenReturn(false);
        when(instanceRepo.save(any(FrontendInstance.class)))
                .thenAnswer(invocation -> {
                    FrontendInstance i = invocation.getArgument(0);
                    i.setId(7L);
                    return i;
                });
        when(outbox.enqueue(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new OutboxEvent());

        seeder.seed();

        verify(outbox, times(1)).enqueue(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("GAP-1203/1083: seedDemoTrio upserts landing (centerName + F-sections + stable logo) and evicts landingPages cache")
    void seedDemoTrioUpsertsLandingAndEvictsCache() {
        // transactionTemplate runs the callback synchronously.
        doAnswer(inv -> {
            inv.getArgument(0, Consumer.class).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        // Instances already exist → instance creation + seed(slug) heavy path skip.
        when(instanceRepo.existsBySlugAndDeletedFalse(anyString())).thenReturn(true);
        // No existing branding/landing rows → orElseGet creates new, then upsert sets fields
        // (GAP-1203: upsert path runs even when row absent — same code path reconciles existing).
        when(brandingRepository.findByInstanceIdAndDeletedFalse(any())).thenReturn(Optional.empty());
        when(landingPageRepository.findByInstanceIdAndDeletedFalse(any())).thenReturn(Optional.empty());
        Cache landingCache = mock(Cache.class);
        when(cacheManager.getCache("landingPages")).thenReturn(landingCache);

        seeder.onApplicationReady();

        // Landing upsert: Hà row carries the short centerName + stable logo + F-section JSONB.
        ArgumentCaptor<LandingPage> lpCaptor = ArgumentCaptor.forClass(LandingPage.class);
        verify(landingPageRepository, atLeastOnce()).save(lpCaptor.capture());
        LandingPage ha = lpCaptor.getAllValues().stream()
                .filter(lp -> BrandingDataSeeder.HA_TENANT_ID.equals(lp.getInstanceId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Hà landing page not seeded"));
        assertThat(ha.getCenterName()).isEqualTo("Cô Hà Toán");
        assertThat(ha.getLogoUrl()).isEqualTo("/demo-banners/co-ha-toan-logo.webp");
        assertThat(ha.getZaloUrl()).isEqualTo("https://zalo.me/co-ha-toan");
        assertThat(ha.getTemplateType()).isEqualTo("personal");
        assertThat(ha.getProblemSolution()).isNotNull();
        assertThat(ha.getProblemSolution().toString()).contains("mất gốc");
        assertThat(ha.getHowItWorks()).isNotNull();
        assertThat(ha.getTrustStrip()).isNotNull();
        // GAP-1224: FAQ + testimonials seeded so FaqSection/TestimonialsSection render.
        assertThat(ha.getFaqs()).isNotNull();
        assertThat(ha.getFaqs().toString()).contains("Học phí");
        assertThat(ha.getTestimonials()).isNotNull();
        assertThat(ha.getTestimonials().toString()).contains("Phụ huynh");
        // GAP-826: Hà ships a single committed banner → 1-slide carousel.
        assertThat(ha.getHeroImages()).containsExactly("/demo-banners/co-ha-toan.webp");

        // GAP-826: the Sky/Khánh §4.1 walkthrough tenant ships a 2-slide carousel so the
        // demo shows the rotator (1st committed banner + 2nd gitignored GAP-810 slide).
        LandingPage sky = lpCaptor.getAllValues().stream()
                .filter(lp -> BrandingDataSeeder.SKY_TENANT_ID.equals(lp.getInstanceId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Sky/Khánh landing page not seeded"));
        assertThat(sky.getHeroImages())
                .containsExactly("/demo-banners/co-khanh-phapluat.webp", "/demo/sky/teacher-do-lan-khanh.webp");
        // GAP-1224: sky/Khánh sparse tenant gets teacher + pricing + FAQ + testimonials
        // (no stats — no academic core, anti-fabrication) to lift the lowest-tenant bar.
        assertThat(sky.getTeachers()).isNotNull();
        assertThat(sky.getTeachers().toString()).contains("Đỗ Lan Khánh");
        assertThat(sky.getPricingTiers()).isNotNull();
        assertThat(sky.getFaqs()).isNotNull();
        assertThat(sky.getTestimonials()).isNotNull();
        // Anti-fabrication: sky has no real academic core, so stats stays unseeded.
        assertThat(sky.getStats()).isNull();

        // Branding upserted (reconciled content) for the demo-trio rows.
        ArgumentCaptor<Branding> bCaptor = ArgumentCaptor.forClass(Branding.class);
        verify(brandingRepository, atLeastOnce()).save(bCaptor.capture());
        assertThat(bCaptor.getAllValues())
                .anySatisfy(b -> assertThat(b.getLogoUrl()).isEqualTo("/demo-banners/co-ha-toan-logo.webp"));

        // GAP-1203: public landing cache evicted so the next read repopulates the reconciled row.
        verify(landingCache, atLeastOnce()).evict(any());
    }
}
