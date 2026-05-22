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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
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
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@Transactional
@WithMockUser(roles = "PLATFORM_ADMIN")
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
        registry.add("jwt.secret", () -> "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ01");
        registry.add("encryption.master-key", () -> "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        registry.add("database.lifecycle.enabled", () -> "false");
        registry.add("captcha.enabled", () -> "false");
        registry.add("kitehub.email-verification.enabled", () -> "false");
        registry.add("kitehub.email-service.enabled", () -> "false");
        // GAP-243: subscription module's S3Config requires region + (when not mock-mode)
        // a real S3 endpoint. mock-mode=true gates the real S3Client bean off via
        // @ConditionalOnProperty so subscription's BackupStorageService dependency chain
        // can resolve.
        registry.add("storage.s3.mock-mode", () -> "true");
        registry.add("storage.s3.region", () -> "ap-southeast-1");
        registry.add("storage.s3.bucket", () -> "kite-test-backups");
        registry.add("storage.s3.access-key", () -> "test-access-key");
        registry.add("storage.s3.secret-key", () -> "test-secret-key");
        // GAP-243: subscription module requires webhook.payment.secret + backup config
        // on context load.
        registry.add("webhook.payment.secret", () -> "test-webhook-secret-for-admin-context-load");
        registry.add("backup.retention-count", () -> "3");
        registry.add("backup.pg-dump-path", () -> "pg_dump");
    }

    // GAP-243: EmailServiceClient autowires RabbitTemplate. RabbitMQ broker not present in
    // test environment; @MockitoBean provides a Mockito proxy so context load succeeds.
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

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
        // GAP-126 — endpoint now returns Page<InstanceSummary> ({content, totalElements, ...})
        mockMvc.perform(get("/api/platform/admin/instances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(20))  // default page size
                .andExpect(jsonPath("$.content[0].organizationName").value("Test Org 1"))
                .andExpect(jsonPath("$.content[0].subdomain").value("testorg1"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[1].organizationName").value("Test Org 2"))
                .andExpect(jsonPath("$.content[1].status").value("TRIAL"));
    }

    @Test
    void testGetAllInstances_clampsPageSizeToMax100() throws Exception {
        // GAP-126 — caller-supplied size > 100 must be clamped to 100
        mockMvc.perform(get("/api/platform/admin/instances").param("size", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
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
        // GAP-285: dates must overlap the subscription's lifetime [now-30d, now+30d] from
        // setUp(). Hardcoded "2026-03-01"/"2026-03-31" was a time-bomb — once today's date
        // drifted past 2026-03-31 + 30d, the subscription window stopped overlapping the
        // query window and revenue dropped to 0. Use relative dates that always cover the
        // setup's active subscription.
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(60);
        LocalDate endDate = today;

        mockMvc.perform(get("/api/platform/admin/revenue")
                        .param("period", "MONTHLY")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("MONTHLY"))
                .andExpect(jsonPath("$.totalRevenue").value(500000))
                .andExpect(jsonPath("$.mrr").value(500000));
    }

    @Test
    void testGetAllSubscriptions() throws Exception {
        // GAP-126 — Page<Subscription> response shape
        mockMvc.perform(get("/api/platform/admin/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[0].tier").value("BASIC"))
                .andExpect(jsonPath("$.content[0].priceVnd").value(500000))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }
}
