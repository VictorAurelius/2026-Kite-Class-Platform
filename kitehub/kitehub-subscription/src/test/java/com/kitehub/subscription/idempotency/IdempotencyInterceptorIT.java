package com.kitehub.subscription.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.idempotency.interceptor.IdempotencyHandlerInterceptor;
import com.kitehub.subscription.idempotency.repository.IdempotencyKeyRepository;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link IdempotencyHandlerInterceptor} wired on
 * {@code POST /api/platform/instances} (GAP-536 Wave onboarding-polish-2 Bucket C).
 *
 * <p>Covers three scenarios:
 * <ol>
 *   <li>Same key + same body → 1 DB row in {@code idempotency_keys} + 2nd
 *       response = cached body with replay header</li>
 *   <li>Same key + different body → 2nd returns HTTP 422 idempotency conflict</li>
 *   <li>No {@code Idempotency-Key} header → handler runs normally, no row</li>
 * </ol></p>
 *
 * <p>Uses Testcontainers PostgreSQL (GAP-536 live verify, per
 * {@code postgres-specific-type-testcontainers.md}) instead of H2. The H2 test
 * profile cannot start the full {@code @SpringBootTest} context because a
 * Hibernate connection initializer issues {@code SELECT set_config('app.current_tenant_id', ...)}
 * (Postgres RLS tenant-isolation setup) on every JDBC connection open — H2 has no
 * {@code set_config()} function so {@code SessionFactory} fails to build. Real
 * Postgres via Testcontainers resolves this, allowing the interceptor's
 * idempotent double-submit guarantee to be proven end-to-end against a
 * production-equivalent database. Docker daemon required at runtime.</p>
 *
 * @since Wave onboarding-polish-2 Bucket C — GAP-536
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Idempotency Interceptor IT — POST /api/platform/instances")
class IdempotencyInterceptorIT {

    /**
     * Testcontainers Postgres — production-equivalent DB so the RLS
     * {@code set_config(...)} connection initializer + {@code idempotency_keys}
     * schema work (mirrors {@code InstanceControllerIntegrationTest} GAP-544 pattern).
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
        // Override application-test.yml H2 dialect when testcontainers active
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private InstanceRepository instanceRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void cleanUp() {
        idempotencyKeyRepository.deleteAll();
        instanceRepository.deleteAll();
    }

    @Test
    @DisplayName("Same key + same body → 1 DB row + 2nd response replays cached body")
    void sameKeySameBodyReplaysCachedResponse() throws Exception {
        String key = UUID.randomUUID().toString();
        CreateInstanceRequest request = CreateInstanceRequest.builder()
                .subdomain("idempotency-replay")
                .organizationName("Replay Test School")
                .ownerId(UUID.randomUUID())
                .tier(PricingTier.BASIC)
                .build();
        String body = objectMapper.writeValueAsString(request);

        // 1st POST — handler runs, response cached
        MvcResult first = mockMvc.perform(post("/api/platform/instances")
                        .header(IdempotencyHandlerInterceptor.HEADER_IDEMPOTENCY_KEY, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subdomain").value("idempotency-replay"))
                .andReturn();

        String firstResponseBody = first.getResponse().getContentAsString();
        assertThat(firstResponseBody).isNotBlank();

        // After 1st request → exactly 1 row in idempotency_keys for this endpoint
        assertThat(idempotencyKeyRepository.findByKeyAndEndpoint(
                key, IdempotencyHandlerInterceptor.ENDPOINT_POST_INSTANCES))
                .isPresent();
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1L);

        // Verify only 1 instance created in the underlying repo
        long instanceCountAfterFirst = instanceRepository.count();
        assertThat(instanceCountAfterFirst).isEqualTo(1L);

        // 2nd POST same key + same body → replay cached response, NO new instance
        MvcResult second = mockMvc.perform(post("/api/platform/instances")
                        .header(IdempotencyHandlerInterceptor.HEADER_IDEMPOTENCY_KEY, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Idempotency-Replay", "true"))
                .andReturn();

        String secondResponseBody = second.getResponse().getContentAsString();
        assertThat(secondResponseBody).isEqualTo(firstResponseBody);

        // Critical: no duplicate instance created
        assertThat(instanceRepository.count()).isEqualTo(instanceCountAfterFirst);
        // Critical: still exactly 1 idempotency row (race-safe save handled too)
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Same key + different body → HTTP 422 idempotency conflict")
    void sameKeyDifferentBodyReturns422() throws Exception {
        String key = UUID.randomUUID().toString();
        CreateInstanceRequest first = CreateInstanceRequest.builder()
                .subdomain("conflict-first")
                .organizationName("First School")
                .ownerId(UUID.randomUUID())
                .tier(PricingTier.BASIC)
                .build();

        mockMvc.perform(post("/api/platform/instances")
                        .header(IdempotencyHandlerInterceptor.HEADER_IDEMPOTENCY_KEY, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Same key, DIFFERENT body → conflict
        CreateInstanceRequest different = CreateInstanceRequest.builder()
                .subdomain("conflict-different")
                .organizationName("Different School")
                .ownerId(UUID.randomUUID())
                .tier(PricingTier.PREMIUM)
                .build();

        mockMvc.perform(post("/api/platform/instances")
                        .header(IdempotencyHandlerInterceptor.HEADER_IDEMPOTENCY_KEY, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(different)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("idempotency_conflict"));

        // Still exactly 1 instance + 1 idempotency row from the first successful call
        assertThat(instanceRepository.count()).isEqualTo(1L);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("No Idempotency-Key header → handler proceeds normally, no cache row")
    void missingHeaderProceedsNormally() throws Exception {
        CreateInstanceRequest request = CreateInstanceRequest.builder()
                .subdomain("no-key-header")
                .organizationName("No Header School")
                .ownerId(UUID.randomUUID())
                .tier(PricingTier.BASIC)
                .build();

        mockMvc.perform(post("/api/platform/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subdomain").value("no-key-header"));

        // No idempotency row should have been written
        assertThat(idempotencyKeyRepository.count()).isZero();
        // Instance still created
        assertThat(instanceRepository.count()).isEqualTo(1L);
    }
}
