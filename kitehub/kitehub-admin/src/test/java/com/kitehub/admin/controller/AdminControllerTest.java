package com.kitehub.admin.controller;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for AdminController.
 * <p>
 * These tests require Docker to be running (Testcontainers).
 * Run locally with: ENABLE_INTEGRATION_TESTS=true mvn test
 *
 * @since 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
class AdminControllerTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers manages lifecycle
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("kitehub_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private Instance testInstance1;
    private Instance testInstance2;
    private Subscription testSubscription1;

    @BeforeEach
    void setUp() {
        // Clear data
        subscriptionRepository.deleteAll();
        instanceRepository.deleteAll();

        // Create test instances
        testInstance1 = new Instance();
        testInstance1.setOrganizationName("Test Org 1");
        testInstance1.setSubdomain("testorg1");
        testInstance1.setStatus(InstanceStatus.ACTIVE);
        testInstance1.setOwnerId(UUID.randomUUID());
        testInstance1.setTier(PricingTier.BASIC);
        testInstance1.setDatabaseUrl("jdbc:postgresql://localhost:5432/test1");
        testInstance1.setDatabaseUsername("test1");
        testInstance1.setDatabasePassword("password1");
        testInstance1 = instanceRepository.save(testInstance1);

        testInstance2 = new Instance();
        testInstance2.setOrganizationName("Test Org 2");
        testInstance2.setSubdomain("testorg2");
        testInstance2.setStatus(InstanceStatus.TRIAL);
        testInstance2.setOwnerId(UUID.randomUUID());
        testInstance2.setTier(PricingTier.FREE);
        testInstance2.setDatabaseUrl("jdbc:postgresql://localhost:5432/test2");
        testInstance2.setDatabaseUsername("test2");
        testInstance2.setDatabasePassword("password2");
        testInstance2.setTrialStartedAt(LocalDateTime.now());
        testInstance2.setTrialExpiresAt(LocalDateTime.now().plusDays(14));
        testInstance2 = instanceRepository.save(testInstance2);

        // Create test subscription
        testSubscription1 = new Subscription();
        testSubscription1.setInstanceId(testInstance1.getId());
        testSubscription1.setTier(PricingTier.BASIC);
        testSubscription1.setBillingCycle(BillingCycle.MONTHLY);
        testSubscription1.setPriceVnd(500000L);
        testSubscription1.setStatus(SubscriptionStatus.ACTIVE);
        testSubscription1.setStartedAt(LocalDateTime.now().minusDays(30));
        testSubscription1.setExpiresAt(LocalDateTime.now().plusDays(30));
        testSubscription1.setAutoRenew(true);
        testSubscription1 = subscriptionRepository.save(testSubscription1);
    }

    @Test
    void testGetDashboard() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/platform/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInstances").value(2))
                .andExpect(jsonPath("$.mrr").value(500000))
                .andExpect(jsonPath("$.instancesByStatus.ACTIVE").value(1))
                .andExpect(jsonPath("$.instancesByStatus.TRIAL").value(1))
                .andExpect(jsonPath("$.instancesByTier.BASIC").value(1));
    }

    @Test
    void testGetAllInstances() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/platform/admin/instances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].organizationName").value("Test Org 1"))
                .andExpect(jsonPath("$[0].subdomain").value("testorg1"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].organizationName").value("Test Org 2"))
                .andExpect(jsonPath("$[1].status").value("TRIAL"));
    }

    @Test
    void testSuspendInstance() throws Exception {
        // When & Then
        mockMvc.perform(patch("/api/platform/admin/instances/{id}/suspend", testInstance1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testInstance1.getId().toString()))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // Verify in database
        Instance updated = instanceRepository.findById(testInstance1.getId()).orElseThrow();
        assert updated.getStatus() == InstanceStatus.SUSPENDED;
    }

    @Test
    void testActivateInstance() throws Exception {
        // Given - suspend instance first
        testInstance1.setStatus(InstanceStatus.SUSPENDED);
        instanceRepository.save(testInstance1);

        // When & Then
        mockMvc.perform(patch("/api/platform/admin/instances/{id}/activate", testInstance1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testInstance1.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Verify in database
        Instance updated = instanceRepository.findById(testInstance1.getId()).orElseThrow();
        assert updated.getStatus() == InstanceStatus.ACTIVE;
    }

    @Test
    void testGetRevenue() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/platform/admin/revenue")
                        .param("period", "MONTHLY")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("MONTHLY"))
                .andExpect(jsonPath("$.totalRevenue").value(500000))
                .andExpect(jsonPath("$.mrr").value(500000));
    }

    @Test
    void testGetAllSubscriptions() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/platform/admin/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].tier").value("BASIC"))
                .andExpect(jsonPath("$[0].priceVnd").value(500000))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }
}
