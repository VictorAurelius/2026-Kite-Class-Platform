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
import com.kiteclass.core.module.marketing.repository.LandingPageRepository;
import com.kiteclass.core.module.quality.entity.QualityReport;
import com.kiteclass.core.module.quality.entity.QualityReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
                transactionTemplate);
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
}
