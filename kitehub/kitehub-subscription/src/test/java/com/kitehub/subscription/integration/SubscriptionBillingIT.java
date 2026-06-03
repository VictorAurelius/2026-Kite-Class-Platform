package com.kitehub.subscription.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.CreateSubscriptionRequest;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.SubscriptionResponse;
import com.kitehub.subscription.dto.TierChangeRequest;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for subscription billing flow.
 * Tests: create subscription → upgrade → cancel lifecycle.
 *
 * @since 1.1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Subscription Billing Flow IT")
class SubscriptionBillingIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        subscriptionRepository.deleteAll();
        instanceRepository.deleteAll();
    }

    private InstanceResponse createInstance(String subdomain) throws Exception {
        CreateInstanceRequest request = CreateInstanceRequest.builder()
            .subdomain(subdomain)
            .organizationName("Billing Test School")
            .ownerId(UUID.randomUUID())
            .tier(PricingTier.BASIC)
            .build();

        MvcResult result = mockMvc.perform(post("/api/platform/instances")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readValue(
            result.getResponse().getContentAsString(), InstanceResponse.class);
    }

    @Test
    @DisplayName("Create subscription for instance")
    void createSubscriptionForInstance() throws Exception {
        InstanceResponse instance = createInstance("billing-test");

        CreateSubscriptionRequest subRequest = CreateSubscriptionRequest.builder()
            .instanceId(instance.getId())
            .tier(PricingTier.BASIC)
            .billingCycle(BillingCycle.MONTHLY)
            .build();

        MvcResult result = mockMvc.perform(post("/api/platform/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tier").value("BASIC"))
            .andExpect(jsonPath("$.status").value(SubscriptionStatus.ACTIVE.toString()))
            .andReturn();

        SubscriptionResponse sub = objectMapper.readValue(
            result.getResponse().getContentAsString(), SubscriptionResponse.class);
        assertThat(sub.getId()).isNotNull();
        assertThat(sub.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Upgrade creates pending payment; tier stays current until admin confirms")
    void upgradeSubscriptionTier() throws Exception {
        InstanceResponse instance = createInstance("upgrade-test");

        // Create BASIC subscription
        CreateSubscriptionRequest subRequest = CreateSubscriptionRequest.builder()
            .instanceId(instance.getId())
            .tier(PricingTier.BASIC)
            .billingCycle(BillingCycle.MONTHLY)
            .build();

        MvcResult createResult = mockMvc.perform(post("/api/platform/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        SubscriptionResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), SubscriptionResponse.class);

        // Upgrade to PREMIUM
        TierChangeRequest upgradeRequest = new TierChangeRequest();
        upgradeRequest.setNewTier(PricingTier.PREMIUM);

        // Phase 1 BETA manual-payment: upgrade does NOT apply the new tier immediately.
        // It records pendingTier + a PENDING payment; tier flips only after admin confirms (UC-SUB-07).
        mockMvc.perform(patch("/api/platform/subscriptions/{id}/upgrade", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(upgradeRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tier").value("BASIC"))
            .andExpect(jsonPath("$.pendingTier").value("PREMIUM"))
            .andExpect(jsonPath("$.pendingPaymentId").isNotEmpty());
    }

    @Test
    @DisplayName("Cancel subscription")
    void cancelSubscription() throws Exception {
        InstanceResponse instance = createInstance("cancel-test");

        CreateSubscriptionRequest subRequest = CreateSubscriptionRequest.builder()
            .instanceId(instance.getId())
            .tier(PricingTier.BASIC)
            .billingCycle(BillingCycle.MONTHLY)
            .build();

        MvcResult createResult = mockMvc.perform(post("/api/platform/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        SubscriptionResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), SubscriptionResponse.class);

        // Cancel subscription
        mockMvc.perform(delete("/api/platform/subscriptions/{id}?immediate=true", created.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Get active subscription for instance")
    void getActiveSubscriptionForInstance() throws Exception {
        InstanceResponse instance = createInstance("active-sub-test");

        CreateSubscriptionRequest subRequest = CreateSubscriptionRequest.builder()
            .instanceId(instance.getId())
            .tier(PricingTier.PREMIUM)
            .billingCycle(BillingCycle.ANNUALLY)
            .build();

        mockMvc.perform(post("/api/platform/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subRequest)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/platform/subscriptions/instance/{id}/active", instance.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tier").value("PREMIUM"))
            .andExpect(jsonPath("$.billingCycle").value("YEARLY"));
    }
}
