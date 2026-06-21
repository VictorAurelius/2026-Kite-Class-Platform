package com.kitehub.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.UpgradeRequest;
import com.kitehub.subscription.service.InstancePurgeService;
import com.kitehub.subscription.service.InstanceService;
import com.kitehub.subscription.service.TrialService;
import com.kitehub.subscription.service.TrialToPaidService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
 * GAP-1525 (OWASP A01) RBAC + IDOR tests for the two endpoints that GAP-1491's
 * cluster did not cover:
 * <ul>
 *   <li>{@code POST /api/platform/instances} ({@link InstanceController#createInstance})
 *       — platform-admin-only (was unguarded while every sibling had a guard).</li>
 *   <li>{@code POST /api/platform/instances/{id}/upgrade}
 *       ({@link TrialToPaidController#upgrade}) — OWNER role + own-instance ownership
 *       (was unguarded → any authed user could drive any tenant's trial→paid).</li>
 * </ul>
 *
 * <p>Imports the real {@link SecurityConfig} (carries {@code @EnableMethodSecurity}) so
 * {@code @PreAuthorize} is actually enforced — mirrors {@code PaymentControllerSecurityTest}.</p>
 */
@WebMvcTest(controllers = {InstanceController.class, TrialToPaidController.class})
@Import(SecurityConfig.class)
@DisplayName("GAP-1525 — Instance create + Trial→Paid upgrade authz")
class TrialAndInstanceAuthzSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private InstanceService instanceService;
    @MockitoBean
    private TrialService trialService;
    @MockitoBean
    private InstancePurgeService instancePurgeService;
    @MockitoBean
    private TrialToPaidService trialToPaidService;
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private String createInstanceBody() throws Exception {
        return mapper.writeValueAsString(CreateInstanceRequest.builder()
                .subdomain("authz-test-" + System.nanoTime())
                .organizationName("Authz IT School")
                .ownerId(UUID.randomUUID())
                .tier(PricingTier.BASIC)
                .build());
    }

    private String upgradeBody() throws Exception {
        return mapper.writeValueAsString(UpgradeRequest.builder()
                .tier(PricingTier.BASIC)
                .billingCycle("MONTHLY")
                .paymentMethodId("pm_authz_test")
                .build());
    }

    @Nested
    @DisplayName("POST /api/platform/instances — platform-admin only")
    class CreateInstance {

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("STAFF → 403")
        void staff_returns403() throws Exception {
            mockMvc.perform(post("/api/platform/instances").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(createInstanceBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER → 403 (instance creation is platform-admin scope)")
        void owner_returns403() throws Exception {
            mockMvc.perform(post("/api/platform/instances").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(createInstanceBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → 401")
        void anonymous_returns401() throws Exception {
            mockMvc.perform(post("/api/platform/instances").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(createInstanceBody()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN → 201")
        void platformAdmin_returns201() throws Exception {
            when(instanceService.createTrialInstance(any())).thenReturn(null);
            mockMvc.perform(post("/api/platform/instances").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(createInstanceBody()))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("POST /api/platform/instances/{id}/upgrade — OWNER + own-instance")
    class Upgrade {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Wrong role (STUDENT) → 403")
        void wrongRole_returns403() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(post("/api/platform/instances/" + id + "/upgrade").with(csrf())
                            .header("X-Tenant-Id", id.toString())
                            .contentType(MediaType.APPLICATION_JSON).content(upgradeBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER upgrading OWN instance (X-Tenant-Id == id) → 202")
        void ownerOwnInstance_returns202() throws Exception {
            UUID id = UUID.randomUUID();
            when(trialToPaidService.initiateUpgrade(any(), any())).thenReturn(null);
            mockMvc.perform(post("/api/platform/instances/" + id + "/upgrade").with(csrf())
                            .header("X-Tenant-Id", id.toString())
                            .contentType(MediaType.APPLICATION_JSON).content(upgradeBody()))
                    .andExpect(status().isAccepted());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER upgrading ANOTHER tenant's instance (X-Tenant-Id != id) → 403 (IDOR blocked)")
        void ownerCrossTenant_returns403() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(post("/api/platform/instances/" + id + "/upgrade").with(csrf())
                            .header("X-Tenant-Id", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON).content(upgradeBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN bypasses ownership → 202")
        void platformAdmin_returns202() throws Exception {
            UUID id = UUID.randomUUID();
            when(trialToPaidService.initiateUpgrade(any(), any())).thenReturn(null);
            mockMvc.perform(post("/api/platform/instances/" + id + "/upgrade").with(csrf())
                            .header("X-Tenant-Id", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON).content(upgradeBody()))
                    .andExpect(status().isAccepted());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → 401")
        void anonymous_returns401() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(post("/api/platform/instances/" + id + "/upgrade").with(csrf())
                            .header("X-Tenant-Id", id.toString())
                            .contentType(MediaType.APPLICATION_JSON).content(upgradeBody()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
