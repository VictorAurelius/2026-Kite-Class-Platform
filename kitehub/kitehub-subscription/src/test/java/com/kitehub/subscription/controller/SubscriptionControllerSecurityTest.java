package com.kitehub.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.dto.CreateSubscriptionRequest;
import com.kitehub.subscription.dto.SubscriptionResponse;
import com.kitehub.subscription.service.SubscriptionRenewalService;
import com.kitehub.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC security tests for {@link SubscriptionController} (GAP-562b Wave 80 Bucket C).
 *
 * <p>Verifies STAFF role is rejected (403) on subscription mutations
 * (create / upgrade / downgrade / cancel / renew) and accepted (200) on
 * read endpoints; OWNER + legacy aliases retain full access; anonymous → 401.</p>
 */
@WebMvcTest(controllers = SubscriptionController.class)
@Import(SecurityConfig.class)
@DisplayName("SubscriptionController — RBAC security")
class SubscriptionControllerSecurityTest {

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

    /** Fixed instance id used as the gateway-trusted X-Tenant-Id in OWNER/STAFF happy paths. */
    private static final UUID INSTANCE_ID = UUID.fromString("22003e3c-0000-0000-0000-000000000001");

    @BeforeEach
    void beforeEach() {
        Mockito.reset(subscriptionService, renewalService);
    }

    private CreateSubscriptionRequest sampleCreateRequest() {
        return CreateSubscriptionRequest.builder()
                .instanceId(INSTANCE_ID)
                .tier(PricingTier.BASIC)
                .billingCycle(BillingCycle.MONTHLY)
                .build();
    }

    // ─── Mutation: create ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("STAFF create → 403")
    void staffCreate_returns403() throws Exception {
        mockMvc.perform(post("/api/platform/subscriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER create → 201")
    void ownerCreate_returns201() throws Exception {
        when(subscriptionService.createSubscription(any()))
                .thenReturn(new SubscriptionResponse());
        mockMvc.perform(post("/api/platform/subscriptions")
                        .with(csrf())
                        // GAP-1015: gateway-trusted tenant must match the create-request instanceId.
                        .header("X-Tenant-Id", INSTANCE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isCreated());
    }

    // ─── Mutation: upgrade / downgrade / cancel / renew ───────────────

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("STAFF upgrade → 403")
    void staffUpgrade_returns403() throws Exception {
        mockMvc.perform(patch("/api/platform/subscriptions/{id}/upgrade", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newTier\":\"PREMIUM\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("STAFF cancel → 403")
    void staffCancel_returns403() throws Exception {
        mockMvc.perform(delete("/api/platform/subscriptions/{id}", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("STAFF renew → 403")
    void staffRenew_returns403() throws Exception {
        mockMvc.perform(post("/api/platform/subscriptions/{id}/renew", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ─── Read: get / list-by-instance / expiring (STAFF allowed) ─────

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("STAFF read /{id} (own tenant) → 200")
    void staffGet_returns200() throws Exception {
        when(subscriptionService.getSubscription(any()))
                .thenReturn(SubscriptionResponse.builder().instanceId(INSTANCE_ID).build());
        mockMvc.perform(get("/api/platform/subscriptions/{id}", UUID.randomUUID())
                        // GAP-1015: trusted tenant matches the subscription's instance → allowed.
                        .header("X-Tenant-Id", INSTANCE_ID.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("STAFF read /expiring → 403 (GAP-1015: now admin-only)")
    void staffExpiring_returns403() throws Exception {
        mockMvc.perform(get("/api/platform/subscriptions/expiring"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("PLATFORM_ADMIN read /expiring → 200")
    void adminExpiring_returns200() throws Exception {
        when(subscriptionService.getExpiringSubscriptions()).thenReturn(List.of());
        mockMvc.perform(get("/api/platform/subscriptions/expiring"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Anonymous read → 401")
    void anonymousRead_returns401() throws Exception {
        mockMvc.perform(get("/api/platform/subscriptions/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
