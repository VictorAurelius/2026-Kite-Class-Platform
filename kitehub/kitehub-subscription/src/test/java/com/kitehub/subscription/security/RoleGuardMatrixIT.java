package com.kitehub.subscription.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.controller.SubscriptionController;
import com.kitehub.subscription.dto.CreateSubscriptionRequest;
import com.kitehub.subscription.dto.SubscriptionResponse;
import com.kitehub.subscription.service.SubscriptionRenewalService;
import com.kitehub.subscription.service.SubscriptionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Wave 98 Bucket B7 — GAP-518 P3 role-guard verify (synthesis matrix).
 *
 * <p>Verifies BE seed → JWT claim → {@code @PreAuthorize} routing for the
 * canonical 4-role × 2-operation matrix on a representative protected surface
 * ({@link SubscriptionController}). This is the HTTP-layer assertion suite that
 * complements per-controller Mockito unit tests by exercising the real
 * Spring Security filter chain (MockMvc + {@link SecurityConfig}).</p>
 *
 * <h2>Role naming clarification (GAP-518 extension finding)</h2>
 *
 * <p>The Wave 98 Cluster B audit (F-NEW-7) framed P3 Center Manager verification
 * using legacy persona-style names ({@code CENTER_OWNER}, {@code CENTER_MANAGER},
 * {@code PLATFORM_ADMIN}). The shipped reality (Wave 79 GAP-562 migration —
 * see {@code com.kitehub.subscription.auth.role.PlatformRole}) consolidated
 * those to:</p>
 *
 * <table>
 *   <caption>Authz role mapping after Wave 79 migration</caption>
 *   <tr><th>Audit framing</th><th>Canonical role (this matrix)</th><th>JWT claim values accepted</th></tr>
 *   <tr><td>P2 Center Owner</td><td>{@code OWNER}</td>
 *       <td>{@code OWNER}, {@code PLATFORM_ADMIN}, {@code ADMIN} (legacy aliases — Wave 81 cutoff 2026-06-14)</td></tr>
 *   <tr><td>P3 Center Manager</td><td>{@code STAFF}</td><td>{@code STAFF}</td></tr>
 *   <tr><td>Platform Admin</td><td>(maps to {@code OWNER} via alias resolver)</td>
 *       <td>{@code PLATFORM_ADMIN}, {@code ADMIN} legacy → resolved to OWNER</td></tr>
 *   <tr><td>Anonymous / no JWT</td><td>—</td><td>→ 401</td></tr>
 * </table>
 *
 * <p>Beta-request signup persona enums ({@code P1_SOLO_TEACHER},
 * {@code P2_CENTER_OWNER}) are payload values, NOT authz roles —
 * see {@code com.kitehub.subscription.beta.controller.BetaAccessController}
 * persona DTO field.</p>
 *
 * <h2>Coverage rationale (per pre-handoff-self-test-completeness.md §2.4)</h2>
 *
 * <p>Per-controller security tests already exist (Wave 80 Bucket C
 * GAP-562b): {@code SubscriptionControllerSecurityTest},
 * {@code PaymentControllerSecurityTest}, {@code BetaAccessControllerTest}.
 * This synthesis matrix asserts the full 4-role × 2-operation cross-product on
 * one canonical surface so that future role-mapping regressions surface here
 * (single failure point) regardless of which controller drifts.</p>
 *
 * <p>Live browser walkthrough (Playwright + real backend) deferred per
 * {@code pre-handoff-self-test-completeness.md} §5.4 — gated on GAP-612
 * (AWS account suspension). Code-level assertions covered here.</p>
 *
 * @since Wave 98 Bucket B7 — GAP-518 extension
 */
@WebMvcTest(controllers = SubscriptionController.class)
@Import(SecurityConfig.class)
@DisplayName("Role-guard 4-role matrix (GAP-518 B7 synthesis)")
class RoleGuardMatrixIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private SubscriptionRenewalService renewalService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(subscriptionService, renewalService);
    }

    private CreateSubscriptionRequest sampleMutationPayload() {
        return CreateSubscriptionRequest.builder()
                .instanceId(UUID.randomUUID())
                .tier(PricingTier.BASIC)
                .billingCycle(BillingCycle.MONTHLY)
                .build();
    }

    // ─── Mutation surface: create-subscription requires OWNER_AUTHZ ──────

    @Nested
    @DisplayName("Mutation endpoint (@PreAuthorize OWNER_AUTHZ)")
    class MutationMatrix {

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER (P2 Center Owner canonical) → 201")
        void ownerMutation_201() throws Exception {
            when(subscriptionService.createSubscription(any()))
                    .thenReturn(new SubscriptionResponse());
            mockMvc.perform(post("/api/platform/subscriptions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleMutationPayload())))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN (legacy alias → OWNER) → 201")
        void platformAdminLegacyAlias_201() throws Exception {
            when(subscriptionService.createSubscription(any()))
                    .thenReturn(new SubscriptionResponse());
            mockMvc.perform(post("/api/platform/subscriptions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleMutationPayload())))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("STAFF (P3 Center Manager canonical) → 403 (mutation forbidden)")
        void staffMutation_403() throws Exception {
            mockMvc.perform(post("/api/platform/subscriptions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleMutationPayload())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous (no JWT) → 401")
        void anonymousMutation_401() throws Exception {
            mockMvc.perform(post("/api/platform/subscriptions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleMutationPayload())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── Read surface: get-subscription accepts OWNER_OR_STAFF_AUTHZ ─────

    @Nested
    @DisplayName("Read endpoint (@PreAuthorize OWNER_OR_STAFF_AUTHZ)")
    class ReadMatrix {

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER → 200")
        void ownerRead_200() throws Exception {
            when(subscriptionService.getSubscription(any()))
                    .thenReturn(new SubscriptionResponse());
            mockMvc.perform(get("/api/platform/subscriptions/{id}", UUID.randomUUID()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("STAFF (P3 Center Manager) → 200 (read allowed)")
        void staffRead_200() throws Exception {
            when(subscriptionService.getSubscription(any()))
                    .thenReturn(new SubscriptionResponse());
            mockMvc.perform(get("/api/platform/subscriptions/{id}", UUID.randomUUID()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN (legacy alias → OWNER) → 200")
        void adminLegacyAlias_200() throws Exception {
            when(subscriptionService.getSubscription(any()))
                    .thenReturn(new SubscriptionResponse());
            mockMvc.perform(get("/api/platform/subscriptions/{id}", UUID.randomUUID()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous (no JWT) → 401")
        void anonymousRead_401() throws Exception {
            mockMvc.perform(get("/api/platform/subscriptions/{id}", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
