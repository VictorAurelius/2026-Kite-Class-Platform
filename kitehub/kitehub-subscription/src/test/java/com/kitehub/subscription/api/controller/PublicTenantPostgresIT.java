package com.kitehub.subscription.api.controller;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.VerticalType;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for {@link PublicTenantController} backed by real PostgreSQL
 * via Testcontainers (Wave tenant-domain-1 Bucket B — GAP-813).
 *
 * <p>Per {@code .claude/rules/postgres-specific-type-testcontainers.md} §1
 * mandate — {@link Instance} entity uses {@code uuid} PK (Postgres-native) and
 * the {@code subdomain} UNIQUE index, so this IT exercises real Postgres
 * binding to catch any regression that H2 would silently accept.</p>
 *
 * <p>Fixture: 3 rows (sky ACTIVE / pioneer ACTIVE / lapsed SUSPENDED) per
 * {@code documents/01-business/kitehub/marketing/api-contract.md} §9.1.3
 * sample data convention.</p>
 *
 * @since Wave tenant-domain-1 Bucket B (GAP-813)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("PublicTenantController — Postgres integration (GAP-813)")
class PublicTenantPostgresIT {

    @Container
    @SuppressWarnings("resource")
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
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstanceRepository instanceRepository;

    /**
     * RabbitMQ is excluded from test profile but the ApplicationContext still
     * wires {@code EmailServiceClient} (depends on {@code RabbitTemplate}); mock
     * it so {@code @SpringBootTest} bootstraps. Public tenant resolve never
     * touches the broker.
     */
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private Instance seed(InstanceStatus status, String slug, String orgName) {
        Instance instance = new Instance();
        instance.setSubdomain(slug);
        instance.setOrganizationName(orgName);
        instance.setOwnerId(UUID.randomUUID());
        instance.setTier(PricingTier.FREE);
        instance.setStatus(status);
        instance.setVerticalType(VerticalType.CENTER);
        instance.setDatabaseUrl("jdbc:postgresql://localhost:5432/" + slug);
        instance.setDatabaseUsername("test_" + slug);
        instance.setDatabasePassword("test_" + slug + "_password");
        instance.setTrialStartedAt(LocalDateTime.now());
        instance.setTrialExpiresAt(LocalDateTime.now().plusDays(14));
        return instanceRepository.save(instance);
    }

    @BeforeEach
    void cleanAndSeed() {
        instanceRepository.deleteAll();
        instanceRepository.flush();
        seed(InstanceStatus.ACTIVE, "sky", "Trung tâm Anh ngữ Sky Education");
        seed(InstanceStatus.ACTIVE, "pioneer", "Trung tâm Pioneer");
        seed(InstanceStatus.SUSPENDED, "lapsed", "Lapsed Center");
        instanceRepository.flush();
    }

    @Test
    @DisplayName("GET /api/v1/public/tenants/by-subdomain/sky → 200 + DTO")
    void resolve_activeTenant_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/public/tenants/by-subdomain/sky"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.subdomain", equalTo("sky")))
                .andExpect(jsonPath("$.name", equalTo("Trung tâm Anh ngữ Sky Education")))
                .andExpect(jsonPath("$.status", equalTo("ACTIVE")));
    }

    @Test
    @DisplayName("Anonymous lookup of second ACTIVE tenant works (no cross-tenant leak)")
    void resolve_secondActiveTenant_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/public/tenants/by-subdomain/pioneer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subdomain", equalTo("pioneer")))
                .andExpect(jsonPath("$.name", equalTo("Trung tâm Pioneer")))
                .andExpect(jsonPath("$.status", equalTo("ACTIVE")));
    }

    @Test
    @DisplayName("Unknown slug → 404 + TENANT_NOT_FOUND")
    void resolve_unknownSlug_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/tenants/by-subdomain/ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("TENANT_NOT_FOUND")));
    }

    @Test
    @DisplayName("SUSPENDED tenant → 410 + TENANT_SUSPENDED + status field")
    void resolve_suspendedTenant_returns410() throws Exception {
        mockMvc.perform(get("/api/v1/public/tenants/by-subdomain/lapsed"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error", equalTo("TENANT_SUSPENDED")))
                .andExpect(jsonPath("$.status", equalTo("SUSPENDED")));
    }

    @Test
    @DisplayName("UPPERCASE slug → 400 INVALID_SLUG_FORMAT (no DB query)")
    void resolve_uppercaseSlug_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/public/tenants/by-subdomain/Sky"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("INVALID_SLUG_FORMAT")));
    }

    @Test
    @DisplayName("Slug with underscore → 400 INVALID_SLUG_FORMAT")
    void resolve_underscoreSlug_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/public/tenants/by-subdomain/sky_education"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("INVALID_SLUG_FORMAT")));
    }

    @Test
    @DisplayName("Soft-deleted tenant invisible (deleted=true rows excluded by repository)")
    void resolve_softDeletedTenant_returns404() throws Exception {
        Instance ghosted = seed(InstanceStatus.ACTIVE, "willvanish", "About To Vanish");
        ghosted.setDeleted(true);
        instanceRepository.save(ghosted);
        instanceRepository.flush();

        mockMvc.perform(get("/api/v1/public/tenants/by-subdomain/willvanish"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("TENANT_NOT_FOUND")));
    }
}
