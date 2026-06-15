package com.kitehub.branding.wizard.controller;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.wizard.dto.RegenerateQuotaResponse;
import com.kitehub.branding.wizard.dto.SlugAvailabilityResponse;
import com.kitehub.branding.wizard.service.RegenerateQuotaService;
import com.kitehub.branding.wizard.service.SlugAvailabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrandingWizardController.class)
@AutoConfigureMockMvc(addFilters = false)
class BrandingWizardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SlugAvailabilityService slugService;

    @MockitoBean
    private RegenerateQuotaService quotaService;

    @MockitoBean
    private com.kitehub.branding.tenant.SubscriptionTierResolver tierResolver;

    @org.junit.jupiter.api.BeforeEach
    void stubTierResolver() {
        // GAP-1020: resolver passes the gateway-trusted header through when no DB tier is found
        // (the @WebMvcTest context has no DB) — preserves the routing/error-envelope assertions.
        when(tierResolver.resolveEffectiveTier(any(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    // -------------------- slug-availability --------------------

    @Test
    void slugAvailability_invalidFormat_returns400() throws Exception {
        when(slugService.validateFormat("BAD!"))
                .thenReturn("slug must be 3–63 chars, lowercase alphanumeric + hyphens, "
                        + "no leading/trailing hyphen");

        mockMvc.perform(get("/api/v1/branding/slug-availability").param("slug", "BAD!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_SLUG_FORMAT"))
                .andExpect(jsonPath("$.slug").value("BAD!"));
    }

    @Test
    void slugAvailability_available_returnsTrueEmptySuggestions() throws Exception {
        when(slugService.validateFormat("free-slug")).thenReturn(null);
        when(slugService.check(org.mockito.ArgumentMatchers.eq("free-slug"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SlugAvailabilityResponse(true, List.of()));

        mockMvc.perform(get("/api/v1/branding/slug-availability").param("slug", "free-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.suggestions", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void slugAvailability_taken_returnsSuggestions() throws Exception {
        when(slugService.validateFormat("abc-school")).thenReturn(null);
        when(slugService.check(org.mockito.ArgumentMatchers.eq("abc-school"), org.mockito.ArgumentMatchers.any())).thenReturn(new SlugAvailabilityResponse(
                false, List.of("abc-school-2", "abc-school-vn", "abc-school-edu")));

        mockMvc.perform(get("/api/v1/branding/slug-availability").param("slug", "abc-school"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.suggestions[0]").value("abc-school-2"));
    }

    // -------------------- regenerate-quota GET --------------------

    @Test
    void getQuota_returnsTierLimitsFromService() throws Exception {
        when(quotaService.getQuota("usr-1", "PRO")).thenReturn(
                new RegenerateQuotaResponse("PRO", 4, 10, Instant.parse("2026-05-08T00:00:00Z")));

        mockMvc.perform(get("/api/v1/branding/regenerate-quota")
                        .header("X-User-Id", "usr-1")
                        .header("X-Subscription-Tier", "PRO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("PRO"))
                .andExpect(jsonPath("$.used").value(4))
                .andExpect(jsonPath("$.limit").value(10));
    }

    // -------------------- regenerate POST --------------------

    @Test
    void regenerate_missingIdempotencyKey_returns400() throws Exception {
        UUID jobId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/branding/jobs/{jobId}/regenerate", jobId)
                        .header("X-Instance-Id", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_IDEMPOTENCY_KEY"));
    }

    @Test
    void regenerate_quotaExceeded_returns403WithEnvelope() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();

        when(quotaService.regenerate(any(), any(), anyString(), anyString(), anyString()))
                .thenThrow(new RegenerateQuotaService.QuotaExceededException(
                        "FREE", 3, 3, Instant.parse("2026-05-08T00:00:00Z")));

        mockMvc.perform(post("/api/v1/branding/jobs/{jobId}/regenerate", jobId)
                        .header("X-Instance-Id", instanceId.toString())
                        .header("X-User-Id", "usr-1")
                        .header("X-Subscription-Tier", "FREE")
                        .header("Idempotency-Key", "key-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("AI_REGENERATE_QUOTA_EXCEEDED"))
                .andExpect(jsonPath("$.tier").value("FREE"))
                .andExpect(jsonPath("$.limit").value(3));
    }

    @Test
    void regenerate_invalidState_returns409() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();

        when(quotaService.regenerate(any(), any(), anyString(), anyString(), anyString()))
                .thenThrow(new RegenerateQuotaService.InvalidJobStateException("PROCESSING"));

        mockMvc.perform(post("/api/v1/branding/jobs/{jobId}/regenerate", jobId)
                        .header("X-Instance-Id", instanceId.toString())
                        .header("Idempotency-Key", "key-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_JOB_STATE"))
                .andExpect(jsonPath("$.currentStatus").value("PROCESSING"));
    }

    @Test
    void regenerate_happyPath_returnsJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        BrandingJob job = new BrandingJob();
        job.setId(jobId);
        job.setInstanceId(instanceId);
        job.setStatus(JobStatus.PROCESSING);
        job.setProgress(0);
        job.setOrganizationName("Test");
        job.setLanguage("vi");

        when(quotaService.regenerate(any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(job);

        mockMvc.perform(post("/api/v1/branding/jobs/{jobId}/regenerate", jobId)
                        .header("X-Instance-Id", instanceId.toString())
                        .header("Idempotency-Key", "key-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }
}
