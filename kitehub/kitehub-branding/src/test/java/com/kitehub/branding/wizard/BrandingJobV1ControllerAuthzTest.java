package com.kitehub.branding.wizard;

import com.kitehub.branding.client.ResilientAIClient;
import com.kitehub.branding.config.SecurityConfig;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.service.BrandingJobService;
import com.kitehub.branding.service.FullAiQuotaService;
import com.kitehub.branding.service.S3StorageService;
import com.kitehub.branding.service.banner.BannerHtmlComposer;
import com.kitehub.branding.service.banner.BannerRenderer;
import com.kitehub.branding.wizard.quality.QualityScoreAggregator;
import com.kitehub.branding.wizard.quality.dto.QualityScoreResponse;
import com.kitehub.branding.wizard.service.MockProvisioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization + cross-tenant IDOR regression tests for
 * {@link BrandingJobV1Controller} (GAP-1526, OWASP A01).
 *
 * <p>The controller already had {@code @PreAuthorize} (READ/WRITE tiers) but {@code approve} +
 * {@code getJob} performed {@code findById} without binding the resolved job's instance to the
 * caller's tenant — a classic IDOR (any OWNER could approve/read another tenant's job by jobId).
 * The fix adds a {@link com.kitehub.branding.security.TenantOwnershipGuard} check after findById.</p>
 */
@WebMvcTest(controllers = BrandingJobV1Controller.class)
@Import(SecurityConfig.class)
@ActiveProfiles("rbac-test")
@DisplayName("BrandingJobV1Controller @PreAuthorize + IDOR ownership (GAP-1526, OWASP A01)")
class BrandingJobV1ControllerAuthzTest {

    private static final UUID OWN_INSTANCE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OWNER_TENANT = OWN_INSTANCE;
    private static final UUID OTHER_TENANT = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID JOB_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandingJobRepository jobRepository;
    @MockitoBean
    private com.kitehub.branding.wizard.quality.BrandColoursDeriver coloursDeriver;
    @MockitoBean
    private BrandingJobService brandingJobService;
    @MockitoBean
    private MockProvisioningService mockProvisioningService;
    @MockitoBean
    private QualityScoreAggregator qualityScoreAggregator;
    @MockitoBean
    private BannerHtmlComposer bannerHtmlComposer;
    @MockitoBean
    private BannerRenderer bannerRenderer;
    @MockitoBean
    private FullAiQuotaService fullAiQuotaService;
    @MockitoBean
    private ResilientAIClient resilientAiClient;
    @MockitoBean
    private S3StorageService s3StorageService;
    @MockitoBean
    private com.kitehub.branding.wizard.sse.SseTokenService sseTokenService;

    @BeforeEach
    void setUp() {
        BrandingJob job = new BrandingJob();
        job.setId(JOB_ID);
        job.setInstanceId(OWN_INSTANCE);   // job belongs to OWNER's tenant
        job.setStatus(JobStatus.COMPLETED);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(coloursDeriver.derive(any())).thenReturn(
                new com.kitehub.branding.wizard.dto.BrandColours(
                        "#1E40AF", "#F59E0B", "#F59E0B", "#0F172A", "#FFFFFF",
                        com.kitehub.branding.wizard.dto.BrandColours.Source.TEMPLATE));
        when(qualityScoreAggregator.aggregate(any())).thenReturn(new QualityScoreResponse(
                JOB_ID.toString(), 88, true, 70, Map.of(), List.of(), Instant.now()));
    }

    // ---- READ (getJob) -----------------------------------------------------

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER GET own-tenant job → not 403 (read allowed)")
    void owner_getOwnJob_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/branding/jobs/{jobId}", JOB_ID)
                        .header("X-Tenant-Id", OWNER_TENANT.toString()))
                .andExpect(result -> assertNotForbidden(result.getResponse().getStatus(),
                        "OWNER read own job"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("OWASP A01: STUDENT GET job → 403 (read is staff-tier)")
    void student_getJob_denied() throws Exception {
        mockMvc.perform(get("/api/v1/branding/jobs/{jobId}", JOB_ID)
                        .header("X-Tenant-Id", OWNER_TENANT.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWASP A01 IDOR: OWNER GET another tenant's job → 403")
    void owner_getCrossTenantJob_denied() throws Exception {
        mockMvc.perform(get("/api/v1/branding/jobs/{jobId}", JOB_ID)
                        .header("X-Tenant-Id", OTHER_TENANT.toString()))
                .andExpect(status().isForbidden());
    }

    // ---- WRITE (approve) ---------------------------------------------------

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER approve own-tenant job → not 403 (write allowed)")
    void owner_approveOwnJob_allowed() throws Exception {
        mockMvc.perform(post("/api/v1/branding/jobs/{jobId}/approve", JOB_ID)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}")
                        .header("X-Tenant-Id", OWNER_TENANT.toString()))
                .andExpect(result -> assertNotForbidden(result.getResponse().getStatus(),
                        "OWNER approve own job"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("OWASP A01: TEACHER approve → 403 (approve is OWNER-tier write)")
    void teacher_approve_denied() throws Exception {
        mockMvc.perform(post("/api/v1/branding/jobs/{jobId}/approve", JOB_ID)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}")
                        .header("X-Tenant-Id", OWNER_TENANT.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWASP A01 IDOR: OWNER approve another tenant's job → 403")
    void owner_approveCrossTenantJob_denied() throws Exception {
        mockMvc.perform(post("/api/v1/branding/jobs/{jobId}/approve", JOB_ID)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}")
                        .header("X-Tenant-Id", OTHER_TENANT.toString()))
                .andExpect(status().isForbidden());
    }

    private static void assertNotForbidden(int statusCode, String label) {
        if (statusCode == 403) {
            throw new AssertionError(label + " must NOT be 403 (allowed role + own tenant)");
        }
    }
}
