package com.kitehub.subscription.onboarding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.onboarding.domain.OnboardingStepId;
import com.kitehub.subscription.onboarding.dto.OnboardingProgressResponse;
import com.kitehub.subscription.onboarding.dto.OnboardingProgressUpdateCommand;
import com.kitehub.subscription.onboarding.dto.OnboardingStepDto;
import com.kitehub.subscription.onboarding.service.OnboardingProgressService;
import com.kitehub.subscription.service.JwtKeyService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for {@link OnboardingProgressController} (Wave 78 GAP-538).
 *
 * <p>Verifies contract per
 * {@code documents/01-business/kitehub/onboarding/api-contract.md}: GET returns
 * 5-step shape, PUT enforces stepId enum, missing X-Tenant-Id → 403.</p>
 */
@WebMvcTest(controllers = OnboardingProgressController.class)
@Import(SecurityConfig.class)
@DisplayName("OnboardingProgressController")
class OnboardingProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private OnboardingProgressService service;

    @MockitoBean
    private JwtKeyService jwtKeyService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final UUID TENANT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");

    @BeforeEach
    void setUp() {
        Mockito.reset(service, jwtKeyService);
    }

    /**
     * Build a mocked {@link Jws} carrying a {@code tenantId} claim. Used to
     * simulate JwtKeyService.parse() output in GAP-554 cross-check tests.
     */
    @SuppressWarnings("unchecked")
    private void stubJwtTenantClaim(String tenantClaim) {
        Jws<Claims> jws = (Jws<Claims>) mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(jws.getPayload()).thenReturn(claims);
        when(claims.get("tenantId")).thenReturn(tenantClaim);
        when(jwtKeyService.parse(anyString())).thenReturn(jws);
    }

    private OnboardingProgressResponse sampleResponse(int completionPercent) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-14T08:30:00Z");
        List<OnboardingStepDto> steps = List.of(
                new OnboardingStepDto(OnboardingStepId.PROFILE_SETUP, completionPercent >= 20, completionPercent >= 20 ? now : null),
                new OnboardingStepDto(OnboardingStepId.INVITE_TEAM, false, null),
                new OnboardingStepDto(OnboardingStepId.IMPORT_DATA, false, null),
                new OnboardingStepDto(OnboardingStepId.CREATE_FIRST_CLASS, false, null),
                new OnboardingStepDto(OnboardingStepId.EXPLORE_FEATURES, false, null)
        );
        int completed = (int) steps.stream().filter(OnboardingStepDto::completed).count();
        return new OnboardingProgressResponse(TENANT_ID, completionPercent, steps.size(), completed, now, steps);
    }

    @Test
    @WithMockUser
    @DisplayName("GET — 200 with 5-step response shape")
    void getReturns200WithSteps() throws Exception {
        when(service.getProgress(eq(TENANT_ID))).thenReturn(sampleResponse(0));

        mockMvc.perform(get("/api/v1/onboarding-progress")
                        .header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.totalSteps").value(5))
                .andExpect(jsonPath("$.completedSteps").value(0))
                .andExpect(jsonPath("$.completionPercent").value(0))
                .andExpect(jsonPath("$.steps[0].stepId").value("PROFILE_SETUP"))
                .andExpect(jsonPath("$.steps[4].stepId").value("EXPLORE_FEATURES"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET — 403 TENANT_CONTEXT_MISSING when X-Tenant-Id absent")
    void getMissingTenantReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding-progress"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("TENANT_CONTEXT_MISSING"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET — 403 TENANT_CONTEXT_MISSING when X-Tenant-Id malformed")
    void getMalformedTenantReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding-progress")
                        .header("X-Tenant-Id", "not-a-uuid"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("TENANT_CONTEXT_MISSING"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT — 200 with updated state on valid payload")
    void putValidReturns200() throws Exception {
        when(service.updateStep(eq(TENANT_ID), any())).thenReturn(sampleResponse(20));
        OnboardingProgressUpdateCommand cmd =
                new OnboardingProgressUpdateCommand(OnboardingStepId.PROFILE_SETUP, true);

        mockMvc.perform(put("/api/v1/onboarding-progress")
                        .with(csrf())
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionPercent").value(20))
                .andExpect(jsonPath("$.completedSteps").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT — 400 ONBOARDING_INVALID_STEP_ID on unknown step value")
    void putInvalidStepReturns400() throws Exception {
        String body = "{\"stepId\":\"WAT_INVALID\",\"completed\":true}";

        mockMvc.perform(put("/api/v1/onboarding-progress")
                        .with(csrf())
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ONBOARDING_INVALID_STEP_ID"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT — 400 ONBOARDING_INVALID_PAYLOAD when completed field missing")
    void putMissingCompletedReturns400() throws Exception {
        String body = "{\"stepId\":\"PROFILE_SETUP\"}";

        mockMvc.perform(put("/api/v1/onboarding-progress")
                        .with(csrf())
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ONBOARDING_INVALID_PAYLOAD"));
    }

    // ── GAP-554 — JWT tenant claim cross-check ──

    @Test
    @WithMockUser
    @DisplayName("GET — 403 TENANT_HEADER_JWT_MISMATCH when JWT tenantId claim differs from header (GAP-554)")
    void getRejectsMismatchedJwtTenantClaim() throws Exception {
        stubJwtTenantClaim(OTHER_TENANT_ID.toString());

        mockMvc.perform(get("/api/v1/onboarding-progress")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .header("Authorization", "Bearer dummy.jwt.token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("TENANT_HEADER_JWT_MISMATCH"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT — 403 TENANT_HEADER_JWT_MISMATCH when JWT tenantId claim differs from header (GAP-554)")
    void putRejectsMismatchedJwtTenantClaim() throws Exception {
        stubJwtTenantClaim(OTHER_TENANT_ID.toString());
        OnboardingProgressUpdateCommand cmd =
                new OnboardingProgressUpdateCommand(OnboardingStepId.PROFILE_SETUP, true);

        mockMvc.perform(put("/api/v1/onboarding-progress")
                        .with(csrf())
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .header("Authorization", "Bearer dummy.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("TENANT_HEADER_JWT_MISMATCH"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET — 200 when JWT tenantId claim matches header (happy path GAP-554)")
    void getAllowsMatchingJwtTenantClaim() throws Exception {
        stubJwtTenantClaim(TENANT_ID.toString());
        when(service.getProgress(eq(TENANT_ID))).thenReturn(sampleResponse(0));

        mockMvc.perform(get("/api/v1/onboarding-progress")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .header("Authorization", "Bearer dummy.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()));
    }

    @Test
    @WithMockUser
    @DisplayName("GET — 200 when JWT absent (no cross-check available — GAP-554 incremental scope)")
    void getAllowsWhenJwtAbsent() throws Exception {
        when(service.getProgress(eq(TENANT_ID))).thenReturn(sampleResponse(0));

        mockMvc.perform(get("/api/v1/onboarding-progress")
                        .header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET — 200 when JWT has no tenantId claim (claim absent → allow)")
    void getAllowsWhenJwtMissingTenantClaim() throws Exception {
        stubJwtTenantClaim(null);
        when(service.getProgress(eq(TENANT_ID))).thenReturn(sampleResponse(0));

        mockMvc.perform(get("/api/v1/onboarding-progress")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .header("Authorization", "Bearer no-claim.jwt.token"))
                .andExpect(status().isOk());
    }
}
