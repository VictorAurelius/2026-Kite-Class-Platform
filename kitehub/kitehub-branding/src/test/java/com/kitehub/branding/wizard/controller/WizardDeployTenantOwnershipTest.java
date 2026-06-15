package com.kitehub.branding.wizard.controller;

import com.kitehub.branding.config.SecurityConfig;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.lifecycle.repository.BrandingLifecycleEventRepository;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.tenant.SubscriptionTierResolver;
import com.kitehub.branding.wizard.sse.SseTokenService;
import com.kitehub.branding.wizard.service.RegenerateQuotaService;
import com.kitehub.branding.wizard.service.SlugAvailabilityService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-tenant IDOR regression — GAP-1420 (GAP-1019 sweep miss for the wizard +
 * deploy-stream controllers). The {@code @PreAuthorize} OWNER gate passes; these
 * tests prove the {@link com.kitehub.branding.security.TenantOwnershipGuard} layer
 * denies (403) when the client {@code X-Instance-Id} (or the job's instance) differs
 * from the gateway-trusted {@code X-Tenant-Id}. Uses the enforcing {@code rbac-test}
 * security profile.
 */
@WebMvcTest(controllers = {BrandingWizardController.class, DeployStreamController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("rbac-test")
@DisplayName("Wizard + DeployStream — cross-tenant ownership (GAP-1420)")
class WizardDeployTenantOwnershipTest {

    private static final UUID OWN = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID JOB_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SlugAvailabilityService slugService;
    @MockitoBean private RegenerateQuotaService quotaService;
    @MockitoBean private SubscriptionTierResolver tierResolver;
    @MockitoBean private BrandingJobRepository brandingJobRepository;
    @MockitoBean private SseTokenService sseTokenService;
    @MockitoBean private BrandingLifecycleEventRepository lifecycleEventRepository;

    private BrandingJob ownJob() {
        BrandingJob job = new BrandingJob();
        job.setId(JOB_ID);
        job.setInstanceId(OWN);
        return job;
    }

    // -------------------- regenerate --------------------

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("regenerate with another tenant's X-Instance-Id → 403 (quota never touched)")
    void regenerateCrossTenant_403() throws Exception {
        mockMvc.perform(post("/api/v1/branding/jobs/{jobId}/regenerate", JOB_ID)
                        .with(csrf())
                        .header("X-Instance-Id", OTHER.toString())
                        .header("X-Tenant-Id", OWN.toString())
                        .header("Idempotency-Key", "key-1"))
                .andExpect(status().isForbidden());
        verify(quotaService, never()).regenerate(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("regenerate own instance → passes guard (reaches quota service, 200)")
    void regenerateOwn_passesGuard() throws Exception {
        when(tierResolver.resolveEffectiveTier(any(), anyString())).thenReturn("FREE");
        when(quotaService.regenerate(any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(ownJob());

        mockMvc.perform(post("/api/v1/branding/jobs/{jobId}/regenerate", JOB_ID)
                        .with(csrf())
                        .header("X-Instance-Id", OWN.toString())
                        .header("X-Tenant-Id", OWN.toString())
                        .header("Idempotency-Key", "key-1"))
                .andExpect(status().isOk());
        verify(quotaService).regenerate(any(), any(), anyString(), anyString(), anyString());
    }

    // -------------------- sse-token --------------------

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("mint SSE token for another tenant's job → 403 (token never minted)")
    void mintSseTokenCrossTenant_403() throws Exception {
        when(brandingJobRepository.findById(JOB_ID)).thenReturn(Optional.of(ownJob()));

        mockMvc.perform(post("/api/v1/branding/jobs/{jobId}/sse-token", JOB_ID)
                        .with(csrf())
                        .header("X-Tenant-Id", OTHER.toString())
                        .header("X-User-Id", "usr-1")
                        .header("X-User-Roles", "OWNER"))
                .andExpect(status().isForbidden());
        verify(sseTokenService, never()).mint(anyString(), anyString(), any());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("mint SSE token for own job → 200")
    void mintSseTokenOwn_200() throws Exception {
        when(brandingJobRepository.findById(JOB_ID)).thenReturn(Optional.of(ownJob()));
        when(sseTokenService.mint(anyString(), anyString(), any())).thenReturn("tok");
        when(sseTokenService.getTtlSeconds()).thenReturn(120L);

        mockMvc.perform(post("/api/v1/branding/jobs/{jobId}/sse-token", JOB_ID)
                        .with(csrf())
                        .header("X-Tenant-Id", OWN.toString())
                        .header("X-User-Id", "usr-1")
                        .header("X-User-Roles", "OWNER"))
                .andExpect(status().isOk());
        verify(sseTokenService).mint(anyString(), anyString(), any());
    }
}
