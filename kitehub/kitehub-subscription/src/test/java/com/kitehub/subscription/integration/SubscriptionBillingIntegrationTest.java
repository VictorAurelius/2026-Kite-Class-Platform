package com.kitehub.subscription.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.dto.AdminConfirmPaymentRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the subscription billing flow.
 *
 * <p>Walks the SUB-20 manual VietQR gate end-to-end: create (→ PENDING/FREE/pendingTier)
 * → admin confirm payment (→ ACTIVE at the requested tier) → upgrade / cancel / read-active.</p>
 *
 * <h3>GAP-1064 — Testcontainers Postgres, not H2</h3>
 * <p>The tenant-aware HikariCP pool runs the Postgres-specific
 * {@code SELECT set_config('app.current_tenant_id', '', false), ...} on every connection
 * (RLS context — see {@code application.yml} {@code connection-init-sql}). H2 has no
 * {@code set_config} function, so a {@code @SpringBootTest} on the {@code test} (H2) profile
 * fails ApplicationContext load with {@code Function "SET_CONFIG" not found} — every test
 * ERRORs at boot instead of running. Real PostgreSQL has {@code set_config} + supports the
 * custom {@code app.*} GUC namespace, so this migrates to Testcontainers Postgres (mirroring
 * {@link SepayWebhookRollbackIsolationIT}). Real Flyway migrations build the
 * production-equivalent schema ({@code ddl-auto=validate}) so the SUB-20 nullable
 * {@code started_at} / {@code expires_at} columns a PENDING subscription leaves unset are
 * accepted (entity-generated {@code create-drop} schema drifts from the migration here and
 * rejects the PENDING insert with a not-null violation). Platform tables are not FORCE-RLS,
 * so the empty tenant GUC never blocks the controller writes.</p>
 *
 * <h3>GAP-1044 — auth-migrated + SUB-20-aligned + actually executed in CI</h3>
 * <ul>
 *   <li>{@code @WithMockUser(roles = "PLATFORM_ADMIN")} satisfies the Wave-80 GAP-562b
 *       {@code @PreAuthorize(OWNER_AUTHZ)} on the subscription mutations AND bypasses the
 *       Wave-security-2 {@code TenantOwnershipGuard} (platform admins manage every instance),
 *       so no {@code X-Tenant-Id} header is needed. PLATFORM_ADMIN is also the persona that
 *       drives the {@code /api/platform/admin/payments/{id}/confirm} reconciliation step.</li>
 *   <li>Assertions updated to the SUB-20 create gate (commit ac54a419): create returns
 *       {@code status=PENDING, tier=FREE, pendingTier=<requested>}; the tier flips to ACTIVE
 *       only after a platform admin confirms the pending VietQR payment.</li>
 *   <li>Renamed from {@code SubscriptionBillingIT} → {@code *IntegrationTest} so Spring Boot's
 *       default Surefire {@code <includes>} ({@code **}{@code /*Test.java}) actually runs it in
 *       CI's {@code ./mvnw clean test} (the project ships no maven-failsafe plugin, so {@code *IT}
 *       classes were silently never executed).</li>
 * </ul>
 *
 * @since 1.1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(roles = "PLATFORM_ADMIN")
@DisplayName("Subscription Billing Flow IT")
class SubscriptionBillingIntegrationTest {

    /**
     * Testcontainers Postgres (GAP-1064) — replaces the H2 {@code test} datasource so the
     * RLS {@code connection-init-sql} {@code set_config(...)} resolves. Alpine keeps pull time
     * minimal; the static container is shared across the class.
     */
    @Container
    @SuppressWarnings("resource") // Testcontainers @Container manages lifecycle (JUnit extension)
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("kitehub_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Real Flyway migrations build the production-equivalent schema (validate, not
        // create-drop): SUB-20 PENDING subscriptions leave started_at/expires_at NULL, which the
        // migration allows but the entity-generated create-drop schema rejects. Mirrors
        // SepayWebhookRollbackIsolationIT.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect",
            () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
    }

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

    // RabbitTemplate excluded via RabbitAutoConfiguration exclusion in the test profile,
    // but EmailServiceClient constructor requires it — provide a mock bean.
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

    private SubscriptionResponse createSubscription(UUID instanceId, PricingTier tier,
                                                    BillingCycle cycle) throws Exception {
        CreateSubscriptionRequest subRequest = CreateSubscriptionRequest.builder()
            .instanceId(instanceId)
            .tier(tier)
            .billingCycle(cycle)
            .build();

        MvcResult result = mockMvc.perform(post("/api/platform/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readValue(
            result.getResponse().getContentAsString(), SubscriptionResponse.class);
    }

    /**
     * Admin confirms the SUB-20 pending VietQR payment (UC-SUB-07) → service applies the
     * pending upgrade so the subscription flips PENDING → ACTIVE at the requested tier.
     */
    private void confirmPendingPayment(UUID paymentId) throws Exception {
        AdminConfirmPaymentRequest request = AdminConfirmPaymentRequest.builder()
            .transactionId("TEST-TXN-" + paymentId)
            .build();

        mockMvc.perform(post("/api/platform/admin/payments/{id}/confirm", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Create subscription → SUB-20 PENDING gate (tier stays FREE, requested tier is pending)")
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
            // SUB-20: manual VietQR gate — create does NOT activate. tier stays FREE, the
            // requested tier is recorded as pendingTier with a PENDING payment to confirm.
            .andExpect(jsonPath("$.status").value(SubscriptionStatus.PENDING.toString()))
            .andExpect(jsonPath("$.tier").value(PricingTier.FREE.toString()))
            .andExpect(jsonPath("$.pendingTier").value(PricingTier.BASIC.toString()))
            .andExpect(jsonPath("$.pendingPaymentId").isNotEmpty())
            .andReturn();

        SubscriptionResponse sub = objectMapper.readValue(
            result.getResponse().getContentAsString(), SubscriptionResponse.class);
        assertThat(sub.getId()).isNotNull();
        assertThat(sub.getPendingPaymentId()).isNotNull();
    }

    @Test
    @DisplayName("Upgrade after confirm: ACTIVE BASIC → upgrade creates pending PREMIUM; tier stays until admin confirms")
    void upgradeSubscriptionTier() throws Exception {
        InstanceResponse instance = createInstance("upgrade-test");

        // SUB-20 create → PENDING, then admin confirms the pending payment → ACTIVE BASIC.
        SubscriptionResponse created = createSubscription(
            instance.getId(), PricingTier.BASIC, BillingCycle.MONTHLY);
        confirmPendingPayment(created.getPendingPaymentId());

        // Upgrade to PREMIUM. Phase 1 BETA manual-payment: upgrade does NOT apply the new tier
        // immediately — it records pendingTier + a PENDING payment; tier flips only after admin
        // confirms (UC-SUB-07). The subscription must be ACTIVE for upgrade to be accepted.
        TierChangeRequest upgradeRequest = new TierChangeRequest();
        upgradeRequest.setNewTier(PricingTier.PREMIUM);

        mockMvc.perform(patch("/api/platform/subscriptions/{id}/upgrade", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(upgradeRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tier").value(PricingTier.BASIC.toString()))
            .andExpect(jsonPath("$.pendingTier").value(PricingTier.PREMIUM.toString()))
            .andExpect(jsonPath("$.pendingPaymentId").isNotEmpty());
    }

    @Test
    @DisplayName("Cancel a pending subscription")
    void cancelSubscription() throws Exception {
        InstanceResponse instance = createInstance("cancel-test");

        // SUB-20 create leaves the subscription PENDING; cancel is allowed from any non-CANCELLED state.
        SubscriptionResponse created = createSubscription(
            instance.getId(), PricingTier.BASIC, BillingCycle.MONTHLY);

        mockMvc.perform(delete("/api/platform/subscriptions/{id}?immediate=true", created.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Get active subscription for instance (after confirm)")
    void getActiveSubscriptionForInstance() throws Exception {
        InstanceResponse instance = createInstance("active-sub-test");

        // SUB-20 create → PENDING, then admin confirm → ACTIVE at the requested PREMIUM tier.
        SubscriptionResponse created = createSubscription(
            instance.getId(), PricingTier.PREMIUM, BillingCycle.ANNUALLY);
        confirmPendingPayment(created.getPendingPaymentId());

        mockMvc.perform(get("/api/platform/subscriptions/instance/{id}/active", instance.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tier").value(PricingTier.PREMIUM.toString()))
            .andExpect(jsonPath("$.status").value(SubscriptionStatus.ACTIVE.toString()))
            .andExpect(jsonPath("$.billingCycle").value(BillingCycle.ANNUALLY.toString()));
    }
}
