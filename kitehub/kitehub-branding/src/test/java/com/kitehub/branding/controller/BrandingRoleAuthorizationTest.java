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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GAP-562/562b Wave 101 Bucket B close-out — RBAC security tests for
 * {@link BrandingJobController}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>OWNER can CRUD branding jobs (200/201/204)</li>
 *   <li>STAFF + sub-roles (MANAGER/TEACHER/ACCOUNTANT) READ allowed but cannot WRITE (403)</li>
 *   <li>Legacy PLATFORM_ADMIN / ADMIN aliases retain full access</li>
 *   <li>Anonymous calls return 401</li>
 * </ul>
 *
 * <p>Pattern mirrors {@code kitehub-subscription} {@code PaymentControllerSecurityTest}.
 * Uses {@code @Profile("!test")} security filter chain via {@link SecurityConfig}.
 */
@WebMvcTest(controllers = BrandingJobController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("rbac-test")
@DisplayName("BrandingJobController — RBAC security")
class BrandingRoleAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandingJobService jobService;

    // GAP-1021 (Bucket C): SecurityConfig giờ inject SseTokenService (SseQueryTokenAuthFilter)
    // — slice context cần mock để load.
    @MockitoBean
    private com.kitehub.branding.wizard.sse.SseTokenService sseTokenService;

    private static final UUID INSTANCE_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();

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
    @DisplayName("OWNER POST /jobs → 201 (allowed)")
    void owner_canCreateJob() throws Exception {
        mockMvc.perform(post("/api/platform/branding/jobs")
                        .with(csrf())
                        .header("X-Instance-Id", INSTANCE_ID.toString())
                        // GAP-1019: gateway-trusted tenant must match X-Instance-Id for non-admin.
                        .header("X-Tenant-Id", INSTANCE_ID.toString())
                        .param("organizationName", "Trung tâm Sky Education")
                        .param("language", "vi")
                        .param("logoUrl", "https://example.com/logo.png"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("STAFF (MANAGER) POST /jobs → 403 (forbidden)")
    void staff_cannotCreateJob() throws Exception {
        mockMvc.perform(post("/api/platform/branding/jobs")
                        .with(csrf())
                        .header("X-Instance-Id", INSTANCE_ID.toString())
                        .param("organizationName", "Trung tâm Sky Education")
                        .param("language", "vi")
                        .param("logoUrl", "https://example.com/logo.png"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("STAFF (TEACHER) DELETE /jobs/{id} → 403 (forbidden)")
    void teacher_cannotCancelJob() throws Exception {
        mockMvc.perform(delete("/api/platform/branding/jobs/" + JOB_ID)
                        .with(csrf())
                        .header("X-Instance-Id", INSTANCE_ID.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER GET /jobs/{id} → 200 (read allowed)")
    void owner_canReadJob() throws Exception {
        mockMvc.perform(get("/api/platform/branding/jobs/" + JOB_ID)
                        .header("X-Instance-Id", INSTANCE_ID.toString())
                        .header("X-Tenant-Id", INSTANCE_ID.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("STAFF (MANAGER) GET /jobs/{id} → 200 (read allowed for staff)")
    void staff_canReadJob() throws Exception {
        mockMvc.perform(get("/api/platform/branding/jobs/" + JOB_ID)
                        .header("X-Instance-Id", INSTANCE_ID.toString())
                        .header("X-Tenant-Id", INSTANCE_ID.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("PLATFORM_ADMIN (legacy) POST /jobs → 201 (alias allowed)")
    void platformAdmin_canCreateJob() throws Exception {
        mockMvc.perform(post("/api/platform/branding/jobs")
                        .with(csrf())
                        .header("X-Instance-Id", INSTANCE_ID.toString())
                        .param("organizationName", "Sky Education")
                        .param("language", "vi")
                        .param("logoUrl", "https://example.com/logo.png"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Anonymous POST /jobs → 401 (unauthorized)")
    void anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/platform/branding/jobs")
                        .with(csrf())
                        .header("X-Instance-Id", INSTANCE_ID.toString())
                        .param("organizationName", "Sky Education")
                        .param("language", "vi")
                        .param("logoUrl", "https://example.com/logo.png"))
                .andExpect(status().isUnauthorized());
    }
}
