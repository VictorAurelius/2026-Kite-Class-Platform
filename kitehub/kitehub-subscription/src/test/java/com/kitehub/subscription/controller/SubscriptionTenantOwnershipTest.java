package com.kitehub.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.dto.CreateSubscriptionRequest;
import com.kitehub.subscription.dto.DomainVerifyResponse;
import com.kitehub.subscription.dto.SubscriptionResponse;
import com.kitehub.subscription.service.DomainService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-tenant IDOR regression tests — GAP-1015 (subscription) + GAP-1023 (domain).
 *
 * <p>The {@code @PreAuthorize} role gate passes for OWNER; these tests prove the new
 * {@code TenantOwnershipGuard} layer denies (403) when the gateway-trusted {@code X-Tenant-Id}
 * does not match the resource's instance, while platform admins bypass.</p>
 */
@WebMvcTest(controllers = {SubscriptionController.class, DomainController.class})
@Import(SecurityConfig.class)
@DisplayName("Subscription + Domain — cross-tenant ownership (GAP-1015 / GAP-1023)")
class SubscriptionTenantOwnershipTest {

    private static final UUID OWN_INSTANCE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_INSTANCE = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private SubscriptionRenewalService renewalService;

    @MockitoBean
    private DomainService domainService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void reset() {
        Mockito.reset(subscriptionService, renewalService, domainService);
    }

    // ─── GAP-1015 subscription lifecycle ─────────────────────────────────

    @Nested
    @DisplayName("GAP-1015 subscription")
    class SubscriptionIdor {

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER GET subscription of another tenant → 403 (no body leak)")
        void ownerGetCrossTenant_403() throws Exception {
            when(subscriptionService.getSubscription(any()))
                    .thenReturn(SubscriptionResponse.builder().instanceId(OTHER_INSTANCE).build());
            mockMvc.perform(get("/api/platform/subscriptions/{id}", UUID.randomUUID())
                            .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER cancel subscription of another tenant → 403 (mutation never runs)")
        void ownerCancelCrossTenant_403() throws Exception {
            when(subscriptionService.getSubscription(any()))
                    .thenReturn(SubscriptionResponse.builder().instanceId(OTHER_INSTANCE).build());
            mockMvc.perform(delete("/api/platform/subscriptions/{id}", UUID.randomUUID())
                            .with(csrf())
                            .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                    .andExpect(status().isForbidden());
            verify(subscriptionService, never()).cancelSubscription(any(), Mockito.anyBoolean());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER create subscription for another tenant's instance → 403")
        void ownerCreateCrossTenant_403() throws Exception {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .instanceId(OTHER_INSTANCE)
                    .tier(PricingTier.BASIC)
                    .billingCycle(BillingCycle.MONTHLY)
                    .build();
            mockMvc.perform(post("/api/platform/subscriptions")
                            .with(csrf())
                            .header("X-Tenant-Id", OWN_INSTANCE.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
            verify(subscriptionService, never()).createSubscription(any());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER GET own subscription → 200")
        void ownerGetOwn_200() throws Exception {
            when(subscriptionService.getSubscription(any()))
                    .thenReturn(SubscriptionResponse.builder().instanceId(OWN_INSTANCE).build());
            mockMvc.perform(get("/api/platform/subscriptions/{id}", UUID.randomUUID())
                            .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN GET any subscription (no X-Tenant-Id) → 200 bypass")
        void adminGetAny_200() throws Exception {
            when(subscriptionService.getSubscription(any()))
                    .thenReturn(SubscriptionResponse.builder().instanceId(OTHER_INSTANCE).build());
            mockMvc.perform(get("/api/platform/subscriptions/{id}", UUID.randomUUID()))
                    .andExpect(status().isOk());
        }
    }

    // ─── GAP-1023 domain ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GAP-1023 domain")
    class DomainIdor {

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER GET domain of another instance → 403")
        void ownerGetDomainCrossTenant_403() throws Exception {
            mockMvc.perform(get("/api/instances/{id}/domain", OTHER_INSTANCE)
                            .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                    .andExpect(status().isForbidden());
            verify(domainService, never()).getDomainStatus(any());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER DELETE domain of another instance → 403 (destructive op blocked)")
        void ownerDeleteDomainCrossTenant_403() throws Exception {
            mockMvc.perform(delete("/api/instances/{id}/domain", OTHER_INSTANCE)
                            .with(csrf())
                            .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                    .andExpect(status().isForbidden());
            verify(domainService, never()).removeCustomDomain(any());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER GET own domain → 200")
        void ownerGetOwnDomain_200() throws Exception {
            when(domainService.getDomainStatus(any())).thenReturn(DomainVerifyResponse.builder().build());
            mockMvc.perform(get("/api/instances/{id}/domain", OWN_INSTANCE)
                            .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN DELETE any instance's domain → bypass (service invoked)")
        void adminDeleteDomain_bypass() throws Exception {
            mockMvc.perform(delete("/api/instances/{id}/domain", OTHER_INSTANCE)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
            verify(domainService).removeCustomDomain(OTHER_INSTANCE);
        }
    }
}
