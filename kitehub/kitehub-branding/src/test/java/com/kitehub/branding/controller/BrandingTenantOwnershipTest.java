package com.kitehub.branding.controller;

import com.kitehub.branding.config.SecurityConfig;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.service.BrandingJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-tenant IDOR regression tests — GAP-1019 branding {@code X-Instance-Id} binding.
 *
 * <p>The {@code @PreAuthorize} OWNER role gate passes; these tests prove the new
 * {@link com.kitehub.branding.security.TenantOwnershipGuard} layer denies (403) when the
 * client-supplied {@code X-Instance-Id} differs from the gateway-trusted {@code X-Tenant-Id},
 * while platform admins bypass. Uses the enforcing {@code rbac-test} security profile.</p>
 */
@WebMvcTest(controllers = BrandingJobController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("rbac-test")
@DisplayName("BrandingJobController — cross-tenant ownership (GAP-1019)")
class BrandingTenantOwnershipTest {

    private static final UUID OWN_INSTANCE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_INSTANCE = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID JOB_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandingJobService jobService;

    // GAP-1021 (Bucket C): SecurityConfig giờ inject SseTokenService (SseQueryTokenAuthFilter)
    // — slice context cần mock để load.
    @MockitoBean
    private com.kitehub.branding.wizard.sse.SseTokenService sseTokenService;

    @BeforeEach
    void setUp() {
        Mockito.reset(jobService);
        BrandingJob stub = new BrandingJob();
        when(jobService.createJob(any(), any(), any(), any())).thenReturn(stub);
        when(jobService.getJob(any(), any())).thenReturn(stub);
        when(jobService.cancelJob(any(), any())).thenReturn(true);
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER create job for another tenant's instance → 403 (job never created)")
    void ownerCreateCrossTenant_403() throws Exception {
        mockMvc.perform(post("/api/platform/branding/jobs")
                        .with(csrf())
                        .header("X-Instance-Id", OTHER_INSTANCE.toString())
                        .header("X-Tenant-Id", OWN_INSTANCE.toString())
                        .param("organizationName", "Trung tâm Sky Education")
                        .param("language", "vi")
                        .param("logoUrl", "https://example.com/logo.png"))
                .andExpect(status().isForbidden());
        verify(jobService, never()).createJob(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER read job with another tenant's X-Instance-Id → 403")
    void ownerGetCrossTenant_403() throws Exception {
        mockMvc.perform(get("/api/platform/branding/jobs/" + JOB_ID)
                        .header("X-Instance-Id", OTHER_INSTANCE.toString())
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(status().isForbidden());
        verify(jobService, never()).getJob(any(), any());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER create job for own instance → 201")
    void ownerCreateOwn_201() throws Exception {
        mockMvc.perform(post("/api/platform/branding/jobs")
                        .with(csrf())
                        .header("X-Instance-Id", OWN_INSTANCE.toString())
                        .header("X-Tenant-Id", OWN_INSTANCE.toString())
                        .param("organizationName", "Trung tâm Sky Education")
                        .param("language", "vi")
                        .param("logoUrl", "https://example.com/logo.png"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("PLATFORM_ADMIN create job for any instance (no X-Tenant-Id) → 201 bypass")
    void adminCreateAny_201() throws Exception {
        mockMvc.perform(post("/api/platform/branding/jobs")
                        .with(csrf())
                        .header("X-Instance-Id", OTHER_INSTANCE.toString())
                        .param("organizationName", "Sky Education")
                        .param("language", "vi")
                        .param("logoUrl", "https://example.com/logo.png"))
                .andExpect(status().isCreated());
    }
}
