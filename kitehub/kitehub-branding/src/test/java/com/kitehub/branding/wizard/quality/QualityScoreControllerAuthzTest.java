package com.kitehub.branding.wizard.quality;

import com.kitehub.branding.config.SecurityConfig;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.quality.dto.QualityScoreResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization + cross-tenant IDOR regression tests for {@link QualityScoreController}
 * (GAP-1526, OWASP A01). The endpoint had NO {@code @PreAuthorize} and NO tenant binding — anyone
 * could read any tenant's job quality score by jobId. The fix adds READ-tier authz +
 * a {@link com.kitehub.branding.security.TenantOwnershipGuard} check after findById.
 */
@WebMvcTest(controllers = QualityScoreController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("rbac-test")
@DisplayName("QualityScoreController @PreAuthorize + IDOR ownership (GAP-1526, OWASP A01)")
class QualityScoreControllerAuthzTest {

    private static final UUID OWN_INSTANCE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_TENANT = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID JOB_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandingJobRepository jobRepository;
    @MockitoBean
    private QualityScoreAggregator aggregator;
    @MockitoBean
    private com.kitehub.branding.wizard.sse.SseTokenService sseTokenService;

    @BeforeEach
    void setUp() {
        BrandingJob job = new BrandingJob();
        job.setId(JOB_ID);
        job.setInstanceId(OWN_INSTANCE);
        job.setStatus(JobStatus.COMPLETED);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(aggregator.aggregate(any())).thenReturn(new QualityScoreResponse(
                JOB_ID.toString(), 88, true, 70, Map.of(), List.of(), Instant.now()));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER quality-score own-tenant job → not 403 (read allowed)")
    void owner_qualityScoreOwnJob_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/branding/jobs/{jobId}/quality-score", JOB_ID)
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(result -> assertNotForbidden(result.getResponse().getStatus(),
                        "OWNER quality-score own job"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("OWASP A01: STUDENT quality-score → 403 (read is staff-tier)")
    void student_qualityScore_denied() throws Exception {
        mockMvc.perform(get("/api/v1/branding/jobs/{jobId}/quality-score", JOB_ID)
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWASP A01 IDOR: OWNER quality-score another tenant's job → 403")
    void owner_qualityScoreCrossTenant_denied() throws Exception {
        mockMvc.perform(get("/api/v1/branding/jobs/{jobId}/quality-score", JOB_ID)
                        .header("X-Tenant-Id", OTHER_TENANT.toString()))
                .andExpect(status().isForbidden());
    }

    private static void assertNotForbidden(int statusCode, String label) {
        if (statusCode == 403) {
            throw new AssertionError(label + " must NOT be 403 (allowed role + own tenant)");
        }
    }
}
