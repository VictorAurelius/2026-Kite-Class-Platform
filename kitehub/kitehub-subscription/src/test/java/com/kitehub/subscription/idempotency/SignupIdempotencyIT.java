package com.kitehub.subscription.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.dto.RegisterRequest;
import com.kitehub.subscription.idempotency.interceptor.IdempotencyHandlerInterceptor;
import com.kitehub.subscription.idempotency.repository.IdempotencyKeyRepository;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.UserRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests cho {@link IdempotencyHandlerInterceptor} áp dụng vào
 * {@code POST /api/auth/register} (GAP-730 Wave local-doable-10 Bucket A).
 *
 * <p>Mirrors {@code IdempotencyInterceptorIT} pattern (GAP-536) cho signup flow.
 * Cover 2 scenarios bắt buộc per gap §AC + sister rule
 * {@code pre-handoff-self-test-completeness.md} §2.1 auth-gated flow:
 * <ol>
 *   <li>Same key + same body → 1 instance row + 2nd response replays cached body
 *       (no duplicate signup)</li>
 *   <li>No Idempotency-Key header → handler runs normally (backward-compat)</li>
 * </ol></p>
 *
 * <p>Uses Testcontainers PostgreSQL per {@code postgres-specific-type-testcontainers.md}
 * (H2 không support RLS {@code set_config} mà Hibernate session initializer dùng).</p>
 *
 * @since Wave local-doable-10 Bucket A — GAP-730
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Signup Idempotency IT — POST /api/auth/register")
class SignupIdempotencyIT {

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
    private ObjectMapper objectMapper;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void cleanUp() {
        idempotencyKeyRepository.deleteAll();
        userRepository.deleteAll();
        instanceRepository.deleteAll();
    }

    @Test
    @DisplayName("Same key + same body → 1 instance + 2nd response replays (no duplicate signup)")
    void sameKeySameBodyReplaysCachedResponse() throws Exception {
        String key = UUID.randomUUID().toString();
        RegisterRequest request = buildRequest("idempotent-school", "owner@idempotent-school.test");
        String body = objectMapper.writeValueAsString(request);

        // 1st POST — handler runs, response cached
        MvcResult first = mockMvc.perform(post("/api/auth/register")
                        .header(IdempotencyHandlerInterceptor.HEADER_IDEMPOTENCY_KEY, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String firstResponseBody = first.getResponse().getContentAsString();
        assertThat(firstResponseBody).isNotBlank();

        // Sau 1st request → exactly 1 row trong idempotency_keys cho endpoint signup
        assertThat(idempotencyKeyRepository.findByKeyAndEndpoint(
                key, IdempotencyHandlerInterceptor.ENDPOINT_POST_SIGNUP))
                .isPresent();
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1L);

        // Verify 1 instance + 1 user created (signup side-effect)
        long instanceCountAfterFirst = instanceRepository.count();
        long userCountAfterFirst = userRepository.count();
        assertThat(instanceCountAfterFirst).isEqualTo(1L);
        assertThat(userCountAfterFirst).isEqualTo(1L);

        // 2nd POST same key + same body → replay cached response, KHÔNG provision instance/user mới
        MvcResult second = mockMvc.perform(post("/api/auth/register")
                        .header(IdempotencyHandlerInterceptor.HEADER_IDEMPOTENCY_KEY, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Idempotency-Replay", "true"))
                .andReturn();

        String secondResponseBody = second.getResponse().getContentAsString();
        assertThat(secondResponseBody).isEqualTo(firstResponseBody);

        // Critical: KHÔNG duplicate instance/user
        assertThat(instanceRepository.count()).isEqualTo(instanceCountAfterFirst);
        assertThat(userRepository.count()).isEqualTo(userCountAfterFirst);
        // Still exactly 1 idempotency row
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("No Idempotency-Key header → handler proceeds normally, no cache row")
    void missingHeaderProceedsNormally() throws Exception {
        RegisterRequest request = buildRequest("no-key-signup", "owner@no-key-signup.test");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Không idempotency row được write
        assertThat(idempotencyKeyRepository.count()).isZero();
        // Instance + user vẫn được create
        assertThat(instanceRepository.count()).isEqualTo(1L);
        assertThat(userRepository.count()).isEqualTo(1L);
    }

    private RegisterRequest buildRequest(String subdomain, String email) {
        RegisterRequest request = new RegisterRequest();
        request.setOrganizationName("Idempotent Test School");
        request.setSubdomain(subdomain);
        request.setOwnerEmail(email);
        request.setOwnerPassword("TestPass123!");
        return request;
    }
}
