package com.kitehub.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.UpdateInstanceRequest;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for InstanceController.
 *
 * <p>Uses Testcontainers PostgreSQL (per GAP-544) to provide production-equivalent
 * database isolation instead of H2. Docker daemon required at runtime — CI runners
 * have Docker available; local dev needs Docker Desktop / kite-dev WSL.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("InstanceController Integration Tests")
class InstanceControllerIntegrationTest {

    /**
     * Testcontainers Postgres — replaces hardcoded localhost:5433 dependency
     * (GAP-544 Wave 79 Bucket E). Alpine image keeps pull time minimal.
     */
    @Container
    @SuppressWarnings("resource") // Testcontainers @Container manages lifecycle (JUnit extension)
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("kitehub_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
            () -> "org.hibernate.dialect.PostgreSQLDialect");
        // Override the application-test.yml H2 dialect when testcontainers active
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstanceRepository instanceRepository;

    // RabbitTemplate excluded via RabbitAutoConfiguration exclusion in test profile
    // but EmailServiceClient constructor requires it — provide mock bean
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        instanceRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create trial instance successfully")
    void shouldCreateTrialInstanceSuccessfully() throws Exception {
        // Given
        CreateInstanceRequest request = CreateInstanceRequest.builder()
            .subdomain("test-integration")
            .organizationName("Integration Test Org")
            .ownerId(UUID.randomUUID())
            .tier(PricingTier.BASIC)
            .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/platform/instances")
                // GAP-1525: createInstance is now PLATFORM_ADMIN-guarded; fixture setup
                // authenticates as platform-admin so own-instance/IDOR assertions (run as
                // OWNER via @WithMockUser) can build their world. Ownership comes from
                // request.ownerId, not the auth principal.
                .with(user("setup-admin").roles("PLATFORM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.subdomain").value(request.getSubdomain()))
            .andExpect(jsonPath("$.organizationName").value(request.getOrganizationName()))
            .andExpect(jsonPath("$.tier").value(request.getTier().toString()))
            .andExpect(jsonPath("$.status").value(InstanceStatus.TRIAL.toString()))
            .andExpect(jsonPath("$.isOnTrial").value(true))
            .andExpect(jsonPath("$.trialDaysLeft").exists())
            .andReturn();

        InstanceResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            InstanceResponse.class
        );

        assertThat(response.getId()).isNotNull();
        assertThat(response.getTrialExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return 400 when subdomain is invalid")
    void shouldReturn400WhenSubdomainInvalid() throws Exception {
        // Given
        CreateInstanceRequest request = CreateInstanceRequest.builder()
            .subdomain("TEST_INVALID") // Uppercase not allowed
            .organizationName("Test Org")
            .ownerId(UUID.randomUUID())
            .tier(PricingTier.BASIC)
            .build();

        // When & Then
        mockMvc.perform(post("/api/platform/instances")
                // GAP-1525: createInstance is now PLATFORM_ADMIN-guarded; fixture setup
                // authenticates as platform-admin so own-instance/IDOR assertions (run as
                // OWNER via @WithMockUser) can build their world. Ownership comes from
                // request.ownerId, not the auth principal.
                .with(user("setup-admin").roles("PLATFORM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get instance by ID successfully")
    void shouldGetInstanceByIdSuccessfully() throws Exception {
        // Given
        CreateInstanceRequest createRequest = CreateInstanceRequest.builder()
            .subdomain("test-get-by-id")
            .organizationName("Get By ID Test")
            .ownerId(UUID.randomUUID())
            .tier(PricingTier.BASIC)
            .build();

        MvcResult createResult = mockMvc.perform(post("/api/platform/instances")
                // GAP-1525: createInstance is now PLATFORM_ADMIN-guarded; fixture setup
                // authenticates as platform-admin so own-instance/IDOR assertions (run as
                // OWNER via @WithMockUser) can build their world. Ownership comes from
                // request.ownerId, not the auth principal.
                .with(user("setup-admin").roles("PLATFORM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        InstanceResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            InstanceResponse.class
        );

        // When & Then
        mockMvc.perform(get("/api/platform/instances/{id}", created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(created.getId().toString()))
            .andExpect(jsonPath("$.subdomain").value(createRequest.getSubdomain()));
    }

    @Test
    @DisplayName("Should get instance by subdomain successfully")
    void shouldGetInstanceBySubdomainSuccessfully() throws Exception {
        // Given
        CreateInstanceRequest createRequest = CreateInstanceRequest.builder()
            .subdomain("test-get-by-subdomain")
            .organizationName("Get By Subdomain Test")
            .ownerId(UUID.randomUUID())
            .tier(PricingTier.BASIC)
            .build();

        mockMvc.perform(post("/api/platform/instances")
                // GAP-1525: createInstance is now PLATFORM_ADMIN-guarded; fixture setup
                // authenticates as platform-admin so own-instance/IDOR assertions (run as
                // OWNER via @WithMockUser) can build their world. Ownership comes from
                // request.ownerId, not the auth principal.
                .with(user("setup-admin").roles("PLATFORM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/platform/instances/subdomain/{subdomain}", createRequest.getSubdomain()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subdomain").value(createRequest.getSubdomain()))
            .andExpect(jsonPath("$.organizationName").value(createRequest.getOrganizationName()));
    }

    @Test
    @DisplayName("Should delete instance successfully")
    @WithMockUser(roles = "PLATFORM_ADMIN")  // GAP-1025: deleteInstance now @PreAuthorize PLATFORM_ADMIN/ADMIN
    void shouldDeleteInstanceSuccessfully() throws Exception {
        // Given
        CreateInstanceRequest createRequest = CreateInstanceRequest.builder()
            .subdomain("test-delete")
            .organizationName("Delete Test")
            .ownerId(UUID.randomUUID())
            .tier(PricingTier.BASIC)
            .build();

        MvcResult createResult = mockMvc.perform(post("/api/platform/instances")
                // GAP-1525: createInstance is now PLATFORM_ADMIN-guarded; fixture setup
                // authenticates as platform-admin so own-instance/IDOR assertions (run as
                // OWNER via @WithMockUser) can build their world. Ownership comes from
                // request.ownerId, not the auth principal.
                .with(user("setup-admin").roles("PLATFORM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        InstanceResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            InstanceResponse.class
        );

        // When & Then
        mockMvc.perform(delete("/api/platform/instances/{id}", created.getId()))
            .andExpect(status().isNoContent());

        // Verify instance is soft deleted
        mockMvc.perform(get("/api/platform/instances/{id}", created.getId()))
            .andExpect(status().isNotFound()); // Deleted instance returns 404
    }

    @Test
    @DisplayName("Should not allow duplicate subdomain")
    void shouldNotAllowDuplicateSubdomain() throws Exception {
        // Given
        CreateInstanceRequest request = CreateInstanceRequest.builder()
            .subdomain("duplicate-test")
            .organizationName("Duplicate Test")
            .ownerId(UUID.randomUUID())
            .tier(PricingTier.BASIC)
            .build();

        // Create first instance
        mockMvc.perform(post("/api/platform/instances")
                // GAP-1525: createInstance is now PLATFORM_ADMIN-guarded; fixture setup
                // authenticates as platform-admin so own-instance/IDOR assertions (run as
                // OWNER via @WithMockUser) can build their world. Ownership comes from
                // request.ownerId, not the auth principal.
                .with(user("setup-admin").roles("PLATFORM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Try to create second instance with same subdomain
        mockMvc.perform(post("/api/platform/instances")
                // GAP-1525: createInstance is now PLATFORM_ADMIN-guarded; fixture setup
                // authenticates as platform-admin so own-instance/IDOR assertions (run as
                // OWNER via @WithMockUser) can build their world. Ownership comes from
                // request.ownerId, not the auth principal.
                .with(user("setup-admin").roles("PLATFORM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ── GAP-1050: residual cross-tenant IDOR — update (PUT/PATCH) + owner enumeration ──

    /** Helper: create an instance with a chosen ownerId, return the persisted response. */
    private InstanceResponse createInstance(String subdomain, UUID ownerId) throws Exception {
        CreateInstanceRequest req = CreateInstanceRequest.builder()
            .subdomain(subdomain)
            .organizationName("Org " + subdomain)
            .ownerId(ownerId)
            .tier(PricingTier.BASIC)
            .build();
        MvcResult res = mockMvc.perform(post("/api/platform/instances")
                // GAP-1525: createInstance is now PLATFORM_ADMIN-guarded; fixture setup
                // authenticates as platform-admin so own-instance/IDOR assertions (run as
                // OWNER via @WithMockUser) can build their world. Ownership comes from
                // request.ownerId, not the auth principal.
                .with(user("setup-admin").roles("PLATFORM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), InstanceResponse.class);
    }

    @Test
    @DisplayName("PUT update own instance (X-Tenant-Id == instance id) → 200")
    @WithMockUser(roles = "OWNER")
    void shouldUpdateOwnInstance() throws Exception {
        InstanceResponse created = createInstance("idor-update-own", UUID.randomUUID());
        UpdateInstanceRequest update = UpdateInstanceRequest.builder()
            .organizationName("Renamed Org").build();

        mockMvc.perform(put("/api/platform/instances/{id}", created.getId())
                .header("X-Tenant-Id", created.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organizationName").value("Renamed Org"));
    }

    @Test
    @DisplayName("PUT update ANOTHER tenant's instance (X-Tenant-Id != instance id) → 403 IDOR blocked")
    @WithMockUser(roles = "OWNER")
    void shouldRejectCrossTenantUpdate() throws Exception {
        InstanceResponse victim = createInstance("idor-update-victim", UUID.randomUUID());
        UpdateInstanceRequest update = UpdateInstanceRequest.builder()
            .organizationName("Hijacked Name").build();

        mockMvc.perform(put("/api/platform/instances/{id}", victim.getId())
                .header("X-Tenant-Id", UUID.randomUUID().toString()) // attacker's own instance id
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isForbidden());

        // Verify victim untouched
        mockMvc.perform(get("/api/platform/instances/{id}", victim.getId()))
            .andExpect(jsonPath("$.organizationName").value("Org idor-update-victim"));
    }

    @Test
    @DisplayName("PATCH update any instance as PLATFORM_ADMIN (no tenant header) → 200 bypass")
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void shouldAllowAdminPatchAnyInstance() throws Exception {
        InstanceResponse created = createInstance("idor-update-admin", UUID.randomUUID());
        UpdateInstanceRequest update = UpdateInstanceRequest.builder()
            .organizationName("Admin Renamed").build();

        mockMvc.perform(patch("/api/platform/instances/{id}", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organizationName").value("Admin Renamed"));
    }

    @Test
    @DisplayName("GET /owner/{self} (X-User-Id == ownerId) → 200")
    @WithMockUser(roles = "OWNER")
    void shouldListOwnInstancesByOwner() throws Exception {
        UUID ownerId = UUID.randomUUID();
        createInstance("idor-owner-self", ownerId);

        mockMvc.perform(get("/api/platform/instances/owner/{ownerId}", ownerId)
                .header("X-User-Id", ownerId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /owner/{otherUser} (X-User-Id != ownerId) → 403 enumeration blocked")
    @WithMockUser(roles = "OWNER")
    void shouldRejectCrossUserEnumeration() throws Exception {
        UUID victimOwner = UUID.randomUUID();
        createInstance("idor-owner-victim", victimOwner);

        mockMvc.perform(get("/api/platform/instances/owner/{ownerId}", victimOwner)
                .header("X-User-Id", UUID.randomUUID().toString())) // attacker's own user id
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /owner/{anyUser} as PLATFORM_ADMIN → 200 bypass")
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void shouldAllowAdminEnumerateAnyOwner() throws Exception {
        UUID ownerId = UUID.randomUUID();
        createInstance("idor-owner-admin", ownerId);

        mockMvc.perform(get("/api/platform/instances/owner/{ownerId}", ownerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }
}
