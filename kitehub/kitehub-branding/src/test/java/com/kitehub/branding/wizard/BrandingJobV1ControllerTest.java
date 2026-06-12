package com.kitehub.branding.wizard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.service.BrandingJobService;
import com.kitehub.branding.service.FullAiQuotaService;
import com.kitehub.branding.service.S3StorageService;
import com.kitehub.branding.service.banner.BannerComposition;
import com.kitehub.branding.service.banner.BannerHtmlComposer;
import com.kitehub.branding.service.banner.BannerRenderer;
import com.kitehub.branding.wizard.dto.BrandColours;
import com.kitehub.branding.wizard.dto.BrandingJobResponse;
import com.kitehub.branding.wizard.dto.PreviewBannerRequest;
import com.kitehub.branding.wizard.dto.ApproveDeployRequest;
import com.kitehub.branding.wizard.quality.BrandColoursDeriver;
import com.kitehub.branding.wizard.quality.QualityScoreAggregator;
import com.kitehub.branding.wizard.quality.dto.QualityScoreResponse;
import com.kitehub.branding.wizard.service.MockProvisioningService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BrandingJobV1Controller")
class BrandingJobV1ControllerTest {

    @Mock
    private BrandingJobRepository jobRepository;

    @Mock
    private BrandingJobService brandingJobService;

    @Mock
    private MockProvisioningService mockProvisioningService;

    @Mock
    private QualityScoreAggregator qualityScoreAggregator;

    @Mock
    private BannerHtmlComposer bannerHtmlComposer;

    @Mock
    private BannerRenderer bannerRenderer;

    @Mock
    private FullAiQuotaService fullAiQuotaService;

    @Mock
    private S3StorageService s3StorageService;

    private final BrandColoursDeriver coloursDeriver = new BrandColoursDeriver();

    private BrandingJobV1Controller controller;

    private BrandingJob job;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        controller = new BrandingJobV1Controller(
                jobRepository, coloursDeriver, brandingJobService, mockProvisioningService,
                qualityScoreAggregator,
                bannerHtmlComposer, bannerRenderer, fullAiQuotaService, s3StorageService);
        jobId = UUID.randomUUID();
        job = new BrandingJob();
        job.setId(jobId);
        job.setInstanceId(UUID.randomUUID());
        job.setOrganizationName("Trường ABC");
        job.setLanguage("vi");
        job.setStatus(JobStatus.COMPLETED);
        job.setLogoUrl("https://cdn.example.com/logo.png");
        job.setProgress(100);
        job.setRetryCount(2);
        job.setQueuedAt(LocalDateTime.now());
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("GAP-1141: preview-banner composes via renderer + returns TEMPLATE mode (no quota)")
    void previewBanner_returnsBannerUrlAndTemplateMode() {
        BannerComposition composition = new BannerComposition("<html></html>", 1200, 630);
        when(bannerHtmlComposer.compose(any(), any(), any(), any(), any(), any()))
                .thenReturn(composition);
        when(bannerRenderer.render(eq(composition), any()))
                .thenReturn("https://cdn.example.com/banner.webp");

        PreviewBannerRequest req = new PreviewBannerRequest(
                "Trung tâm Sky", "Học giỏi", "https://cdn.example.com/logo.png",
                List.of(), "📚",
                new BrandColours("#1E40AF", "#F59E0B", "#F59E0B", "#0F172A", "#FFFFFF",
                        BrandColours.Source.TEMPLATE),
                null);

        ResponseEntity<?> response = controller.previewBanner(req, "FREE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsEntry("mode", "TEMPLATE");
        assertThat(body).containsEntry("bannerUrl", "https://cdn.example.com/banner.webp");
    }

    @Test
    @DisplayName("GAP-1141: preview-banner falls back to default palette when colours absent")
    void previewBanner_nullColours_usesDefaultPalette() {
        BannerComposition composition = new BannerComposition("<html></html>", 1200, 630);
        when(bannerHtmlComposer.compose(any(), any(), any(), any(), any(), any()))
                .thenReturn(composition);
        when(bannerRenderer.render(eq(composition), any())).thenReturn(null);

        PreviewBannerRequest req = new PreviewBannerRequest(
                "Trung tâm Sky", null, null, null, null, null, null);

        ResponseEntity<?> response = controller.previewBanner(req, "FREE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        // null bannerUrl is allowed (StubBannerRenderer) — FE falls back to logo/placeholder.
        assertThat(body).containsEntry("mode", "TEMPLATE");
        assertThat(body).containsKey("bannerUrl");
    }

    @Test
    @DisplayName("GAP-1218: FULL_AI khi image-gen chưa wire → NOT_AVAILABLE + KHÔNG trừ quota")
    void previewBanner_fullAiImageGenDisabled_fallsBackWithoutCharging() {
        // Default flag = false (image-gen chưa wire per GAP-1135).
        PreviewBannerRequest req = new PreviewBannerRequest(
                "Trung tâm Sky", "Học giỏi", null, null, null, null, "FULL_AI");

        ResponseEntity<?> response = controller.previewBanner(req, "PREMIUM");

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("mode", "TEMPLATE");
        assertThat(body).containsEntry("fallbackReason", "NOT_AVAILABLE");
        org.mockito.Mockito.verify(fullAiQuotaService, org.mockito.Mockito.never())
                .recordFullAiUsage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GAP-1147: FULL_AI from PREMIUM with quota → mode FULL_AI + quota recorded")
    void previewBanner_fullAiPremiumWithQuota_grantsFullAi() {
        // GAP-1218: FULL_AI chỉ granted khi image-gen thật khả dụng.
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "fullAiImageGenEnabled", true);
        BannerComposition composition = new BannerComposition("<html></html>", 1200, 630);
        when(bannerHtmlComposer.compose(any(), any(), any(), any(), any(), any()))
                .thenReturn(composition);
        when(bannerRenderer.render(eq(composition), any()))
                .thenReturn("https://cdn.example.com/banner.webp");
        when(fullAiQuotaService.canUseFullAi(any(), eq("PREMIUM"))).thenReturn(true);

        PreviewBannerRequest req = new PreviewBannerRequest(
                "Trung tâm Sky", "Học giỏi", null, null, null, null, "FULL_AI");

        ResponseEntity<?> response = controller.previewBanner(req, "PREMIUM");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("mode", "FULL_AI");
        assertThat(body).doesNotContainKey("fallbackReason");
        org.mockito.Mockito.verify(fullAiQuotaService).recordFullAiUsage(any(), eq("PREMIUM"));
    }

    @Test
    @DisplayName("GAP-1147: FULL_AI from FREE → falls back to TEMPLATE (tier not eligible, no bypass)")
    void previewBanner_fullAiFreeTier_fallsBackTemplate() {
        BannerComposition composition = new BannerComposition("<html></html>", 1200, 630);
        when(bannerHtmlComposer.compose(any(), any(), any(), any(), any(), any()))
                .thenReturn(composition);
        when(bannerRenderer.render(eq(composition), any())).thenReturn("https://cdn.example.com/b.webp");

        PreviewBannerRequest req = new PreviewBannerRequest(
                "Trung tâm Sky", null, null, null, null, null, "FULL_AI");

        ResponseEntity<?> response = controller.previewBanner(req, "FREE");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("mode", "TEMPLATE");
        assertThat(body).containsEntry("fallbackReason", "TIER_NOT_ELIGIBLE");
        // Gate enforced server-side: the FREE caller never touches the quota meter.
        org.mockito.Mockito.verify(fullAiQuotaService, org.mockito.Mockito.never())
                .recordFullAiUsage(any(), any());
    }

    @Test
    @DisplayName("GAP-1147: FULL_AI from PREMIUM with exhausted quota → fallback TEMPLATE")
    void previewBanner_fullAiPremiumExhausted_fallsBackTemplate() {
        // GAP-1218: bật image-gen để chạm tới nhánh quota (NOT_AVAILABLE precede).
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "fullAiImageGenEnabled", true);
        BannerComposition composition = new BannerComposition("<html></html>", 1200, 630);
        when(bannerHtmlComposer.compose(any(), any(), any(), any(), any(), any()))
                .thenReturn(composition);
        when(bannerRenderer.render(eq(composition), any())).thenReturn("https://cdn.example.com/b.webp");
        when(fullAiQuotaService.canUseFullAi(any(), eq("PREMIUM"))).thenReturn(false);

        PreviewBannerRequest req = new PreviewBannerRequest(
                "Trung tâm Sky", null, null, null, null, null, "FULL_AI");

        ResponseEntity<?> response = controller.previewBanner(req, "PREMIUM");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("mode", "TEMPLATE");
        assertThat(body).containsEntry("fallbackReason", "QUOTA_EXHAUSTED");
        org.mockito.Mockito.verify(fullAiQuotaService, org.mockito.Mockito.never())
                .recordFullAiUsage(any(), any());
    }

    @Test
    @DisplayName("GAP-390-A: tenantId populated from MDC when request carries tenant context")
    void tenantIdSourcedFromMdc() {
        String tenantId = "550e8400-e29b-41d4-a716-446655440000";
        MDC.put("tenantId", tenantId);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = controller.getJob(jobId);

        BrandingJobResponse body = (BrandingJobResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.tenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("GAP-390-A: tenantId is null when MDC has no tenant context (system jobs)")
    void tenantIdNullWhenMdcEmpty() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = controller.getJob(jobId);

        BrandingJobResponse body = (BrandingJobResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.tenantId()).isNull();
    }

    @Test
    @DisplayName("returns BrandingJobResponse with brandColors populated")
    void returnsBrandColors() throws Exception {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = controller.getJob(jobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BrandingJobResponse body = (BrandingJobResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.jobId()).isEqualTo(jobId);
        assertThat(body.brandColors()).isNotNull();
        assertThat(body.brandColors().primary()).matches("^#[0-9A-Fa-f]{6}$");
        assertThat(body.previewUrl()).isEqualTo("/api/v1/branding/jobs/" + jobId + "/preview");
        // Maps queue COMPLETED → contract DEPLOYED
        assertThat(body.status()).isEqualTo("DEPLOYED");

        // Round-trip JSON contains brandColors
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String json = mapper.writeValueAsString(body);
        assertThat(json).contains("\"brandColors\"");
        assertThat(json).contains("\"primary\"");
    }

    @Test
    @DisplayName("returns 404 with JOB_NOT_FOUND when job missing")
    void returns404WhenMissing() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getJob(jobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "JOB_NOT_FOUND");
    }

    @Test
    @DisplayName("GAP-1217: approve blocks deploy when quality score < threshold (FAILED, no provision)")
    void approve_qualityBelowThreshold_blocksDeploy() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(qualityScoreAggregator.aggregate(job)).thenReturn(new QualityScoreResponse(
                jobId.toString(), 55, false, 70,
                java.util.Map.of(), java.util.List.of(), java.time.Instant.now()));

        ResponseEntity<?> response = controller.approve(jobId,
                new ApproveDeployRequest("acme", "modern", List.of("logo", "colors")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "QUALITY_GATE_FAILED");
        assertThat(body).containsEntry("score", 55);
        org.mockito.Mockito.verify(brandingJobService).markJobFailed(eq(jobId), any());
        org.mockito.Mockito.verify(mockProvisioningService, org.mockito.Mockito.never())
                .provisionAsync(any(), any(), any(), any());
    }

    @Test
    @DisplayName("GAP-1217: approve proceeds to deploy when quality score >= threshold (202 + score)")
    void approve_qualityPasses_deploys() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(qualityScoreAggregator.aggregate(job)).thenReturn(new QualityScoreResponse(
                jobId.toString(), 88, true, 70,
                java.util.Map.of(), java.util.List.of(), java.time.Instant.now()));

        ResponseEntity<?> response = controller.approve(jobId,
                new ApproveDeployRequest("acme", "modern", List.of("logo", "colors")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("status", "INITIALIZING");
        assertThat(body).containsEntry("qualityScore", 88);
        org.mockito.Mockito.verify(mockProvisioningService)
                .provisionAsync(eq(jobId), eq("acme"), eq("modern"), any());
        org.mockito.Mockito.verify(brandingJobService, org.mockito.Mockito.never())
                .markJobFailed(any(), any());
    }
}
