package com.kitehub.subscription.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.dto.ForceConvertRequest;
import com.kitehub.subscription.dto.RollbackRequest;
import com.kitehub.subscription.dto.RollbackResponse;
import com.kitehub.subscription.dto.UpgradeResponse;
import com.kitehub.subscription.service.TrialToPaidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GAP-940 — Spring Security {@code @PreAuthorize} integration tests for
 * {@link AdminMigrationController}.
 *
 * <p>Per-method {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")} annotations
 * (per PR #2152 GAP-938) only fire when the controller bean flows through the
 * Spring AOP proxy. Pure Mockito tests that {@code @InjectMocks} the controller
 * bypass the proxy and report PASS without the annotation actually being
 * enforced. This integration test loads {@link SecurityConfig} via
 * {@code @Import} so the full filter chain + AOP advice executes, locking the
 * UC-T2P-02 + UC-T2P-05 ops migration auth invariants in CI.</p>
 *
 * @since Wave flow-kh3 (2026-06-04)
 */
@WebMvcTest(controllers = AdminMigrationController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminMigrationController — Spring Security @PreAuthorize integration")
class AdminMigrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private TrialToPaidService trialToPaidService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(trialToPaidService);
    }

    // ─── POST /api/platform/admin/instances/{id}/force-convert ───────────

    @Nested
    @DisplayName("POST /instances/{id}/force-convert — UC-T2P-05")
    class ForceConvert {

        private ForceConvertRequest sampleRequest() {
            ForceConvertRequest req = new ForceConvertRequest();
            req.setTier(PricingTier.BASIC);
            req.setBillingCycle("MONTHLY");
            req.setInvoiceRef("INV-2026-0001");
            req.setReason("Wire transfer verified by finance ops");
            return req;
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → 401")
        void anonymous_returns401() throws Exception {
            mockMvc.perform(post("/api/platform/admin/instances/{id}/force-convert", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER (non-admin) → 403")
        void ownerRole_returns403() throws Exception {
            mockMvc.perform(post("/api/platform/admin/instances/{id}/force-convert", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("STAFF (non-admin) → 403")
        void staffRole_returns403() throws Exception {
            mockMvc.perform(post("/api/platform/admin/instances/{id}/force-convert", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN → 202 ACCEPTED")
        void platformAdmin_returns202() throws Exception {
            when(trialToPaidService.forceConvert(any(), any(), any(), any()))
                    .thenReturn(Mockito.mock(UpgradeResponse.class));
            mockMvc.perform(post("/api/platform/admin/instances/{id}/force-convert", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isAccepted());
        }
    }

    // ─── POST /api/platform/admin/instances/{id}/rollback-migration ──────

    @Nested
    @DisplayName("POST /instances/{id}/rollback-migration — UC-T2P-02")
    class RollbackMigration {

        private RollbackRequest sampleRequest() {
            RollbackRequest req = new RollbackRequest();
            req.setReason("Customer reported unauthorized upgrade — rolling back within 24h window");
            return req;
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → 401")
        void anonymous_returns401() throws Exception {
            mockMvc.perform(post("/api/platform/admin/instances/{id}/rollback-migration", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER (non-admin) → 403")
        void ownerRole_returns403() throws Exception {
            mockMvc.perform(post("/api/platform/admin/instances/{id}/rollback-migration", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN → 200")
        void platformAdmin_returns200() throws Exception {
            when(trialToPaidService.rollback(any(), any()))
                    .thenReturn(Mockito.mock(RollbackResponse.class));
            mockMvc.perform(post("/api/platform/admin/instances/{id}/rollback-migration", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isOk());
        }
    }
}
