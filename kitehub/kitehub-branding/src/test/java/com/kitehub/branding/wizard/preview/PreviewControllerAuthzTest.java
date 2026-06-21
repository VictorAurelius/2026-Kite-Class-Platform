package com.kitehub.branding.wizard.preview;

import com.kitehub.branding.config.SecurityConfig;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.quality.BrandColoursDeriver;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization + cross-tenant IDOR regression tests for {@link PreviewController}
 * (GAP-1526, OWASP A01). The preview endpoint had NO {@code @PreAuthorize} and NO tenant binding —
 * anyone could read any tenant's job preview HTML by jobId. The fix adds READ-tier authz +
 * a {@link com.kitehub.branding.security.TenantOwnershipGuard} check after findById.
 */
@WebMvcTest(controllers = PreviewController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("rbac-test")
@DisplayName("PreviewController @PreAuthorize + IDOR ownership (GAP-1526, OWASP A01)")
class PreviewControllerAuthzTest {

    private static final UUID OWN_INSTANCE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_TENANT = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID JOB_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandingJobRepository jobRepository;
    @MockitoBean
    private BrandColoursDeriver coloursDeriver;
    @MockitoBean
    private com.kitehub.branding.wizard.sse.SseTokenService sseTokenService;

    @BeforeEach
    void setUp() {
        BrandingJob job = new BrandingJob();
        job.setId(JOB_ID);
        job.setInstanceId(OWN_INSTANCE);
        job.setOrganizationName("Trường ABC");
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(coloursDeriver.derive(any())).thenReturn(
                new com.kitehub.branding.wizard.dto.BrandColours(
                        "#1E40AF", "#F59E0B", "#F59E0B", "#0F172A", "#FFFFFF",
                        com.kitehub.branding.wizard.dto.BrandColours.Source.TEMPLATE));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER preview own-tenant job → not 403 (read allowed)")
    void owner_previewOwnJob_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/branding/jobs/{jobId}/preview", JOB_ID)
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(result -> assertNotForbidden(result.getResponse().getStatus(),
                        "OWNER preview own job"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("OWASP A01: STUDENT preview → 403 (read is staff-tier)")
    void student_preview_denied() throws Exception {
        mockMvc.perform(get("/api/v1/branding/jobs/{jobId}/preview", JOB_ID)
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWASP A01 IDOR: OWNER preview another tenant's job → 403")
    void owner_previewCrossTenant_denied() throws Exception {
        mockMvc.perform(get("/api/v1/branding/jobs/{jobId}/preview", JOB_ID)
                        .header("X-Tenant-Id", OTHER_TENANT.toString()))
                .andExpect(status().isForbidden());
    }

    private static void assertNotForbidden(int statusCode, String label) {
        if (statusCode == 403) {
            throw new AssertionError(label + " must NOT be 403 (allowed role + own tenant)");
        }
    }
}
