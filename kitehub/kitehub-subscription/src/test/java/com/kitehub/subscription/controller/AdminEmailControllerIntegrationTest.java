package com.kitehub.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.dto.EmailConfigResponse;
import com.kitehub.subscription.dto.EmailStatsResponse;
import com.kitehub.subscription.dto.TriggerEmailRequest;
import com.kitehub.subscription.service.EmailAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GAP-940 — Spring Security {@code @PreAuthorize} integration tests for
 * {@link AdminEmailController}.
 *
 * <p>Class-level {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")} (per PR #2152
 * GAP-938) MUST flow through the Spring AOP proxy to fire on each method. Pure
 * Mockito tests that instantiate the controller via {@code @InjectMocks} bypass
 * this proxy and silently report PASS even when the annotation is missing or
 * wrong. This integration test loads {@link SecurityConfig} via {@code @Import}
 * so the real filter chain + AOP advice is exercised, locking the auth
 * invariant in CI per Wave flow-kh3 G1 walk evidence.</p>
 *
 * <p>Scope per controller endpoint × 3 RBAC cases (anonymous 401, non-admin
 * 403, PLATFORM_ADMIN 200). Sister precedent:
 * {@code MagicLinkCacheControlIntegrationTest}, {@code RoleGuardMatrixIT}.</p>
 *
 * @since Wave flow-kh3 (2026-06-04)
 */
@WebMvcTest(controllers = AdminEmailController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminEmailController — Spring Security @PreAuthorize integration")
class AdminEmailControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private EmailAdminService emailAdminService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(emailAdminService);
    }

    // ─── GET /api/platform/admin/emails/history ──────────────────────────

    @Nested
    @DisplayName("GET /history — list email send history")
    class GetHistory {

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → 401")
        void anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/platform/admin/emails/history"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER (non-admin) → 403")
        void ownerRole_returns403() throws Exception {
            mockMvc.perform(get("/api/platform/admin/emails/history"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN → 200")
        void platformAdmin_returns200() throws Exception {
            when(emailAdminService.getEmailHistory(any(), any(), any(), any(),
                    org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.anyInt()))
                    .thenReturn(new PageImpl<>(List.of()));
            mockMvc.perform(get("/api/platform/admin/emails/history"))
                    .andExpect(status().isOk());
        }
    }

    // ─── GET /api/platform/admin/emails/stats ────────────────────────────

    @Nested
    @DisplayName("GET /stats — aggregate email statistics")
    class GetStats {

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → 401")
        void anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/platform/admin/emails/stats"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("STAFF (non-admin) → 403")
        void staffRole_returns403() throws Exception {
            mockMvc.perform(get("/api/platform/admin/emails/stats"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN → 200")
        void platformAdmin_returns200() throws Exception {
            when(emailAdminService.getEmailStats())
                    .thenReturn(Mockito.mock(EmailStatsResponse.class));
            mockMvc.perform(get("/api/platform/admin/emails/stats"))
                    .andExpect(status().isOk());
        }
    }

    // ─── GET /api/platform/admin/emails/config ───────────────────────────

    @Nested
    @DisplayName("GET /config — current email config")
    class GetConfig {

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → 401")
        void anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/platform/admin/emails/config"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER (non-admin) → 403")
        void ownerRole_returns403() throws Exception {
            mockMvc.perform(get("/api/platform/admin/emails/config"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN → 200")
        void platformAdmin_returns200() throws Exception {
            when(emailAdminService.getEmailConfig())
                    .thenReturn(Mockito.mock(EmailConfigResponse.class));
            mockMvc.perform(get("/api/platform/admin/emails/config"))
                    .andExpect(status().isOk());
        }
    }

    // ─── POST /api/platform/admin/emails/trigger ─────────────────────────

    @Nested
    @DisplayName("POST /trigger — manually trigger email send")
    class TriggerEmail {

        private TriggerEmailRequest sampleRequest() {
            TriggerEmailRequest req = new TriggerEmailRequest();
            req.setInstanceId(UUID.randomUUID());
            req.setEmailType("TRIAL_WARNING_DAY_7");
            return req;
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → 401")
        void anonymous_returns401() throws Exception {
            mockMvc.perform(post("/api/platform/admin/emails/trigger")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER (non-admin) → 403")
        void ownerRole_returns403() throws Exception {
            mockMvc.perform(post("/api/platform/admin/emails/trigger")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN → 200")
        void platformAdmin_returns200() throws Exception {
            mockMvc.perform(post("/api/platform/admin/emails/trigger")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isOk());
        }
    }
}
