package com.kitehub.subscription.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.beta.dto.BetaRequestDto;
import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import com.kitehub.subscription.idempotency.interceptor.IdempotencyHandlerInterceptor;
import com.kitehub.subscription.idempotency.repository.IdempotencyKeyRepository;
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
 * {@code POST /api/v1/auth/request-beta-access} (GAP-730 Wave local-doable-10 Bucket A).
 *
 * <p>Mirrors {@code IdempotencyInterceptorIT} pattern (GAP-536) cho beta-request flow.
 * 2 scenarios bắt buộc:
 * <ol>
 *   <li>Same key + same body → 1 beta_access_requests row + 2nd response replays
 *       cached body (no duplicate request)</li>
 *   <li>No Idempotency-Key header → handler runs normally (backward-compat)</li>
 * </ol></p>
 *
 * @since Wave local-doable-10 Bucket A — GAP-730
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Beta Request Idempotency IT — POST /api/v1/auth/request-beta-access")
class BetaRequestIdempotencyIT {

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
    private BetaAccessRequestRepository betaAccessRequestRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void cleanUp() {
        idempotencyKeyRepository.deleteAll();
        betaAccessRequestRepository.deleteAll();
    }

    @Test
    @DisplayName("Same key + same body → 1 beta request row + 2nd response replays")
    void sameKeySameBodyReplaysCachedResponse() throws Exception {
        String key = UUID.randomUUID().toString();
        BetaRequestDto dto = buildDto("teacher-idempotent@example.test");
        String body = objectMapper.writeValueAsString(dto);

        // 1st POST — handler runs, persist + response cached
        MvcResult first = mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .header(IdempotencyHandlerInterceptor.HEADER_IDEMPOTENCY_KEY, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String firstResponseBody = first.getResponse().getContentAsString();
        assertThat(firstResponseBody).isNotBlank();

        // Sau 1st request → 1 row trong idempotency_keys cho endpoint beta-request
        assertThat(idempotencyKeyRepository.findByKeyAndEndpoint(
                key, IdempotencyHandlerInterceptor.ENDPOINT_POST_BETA_REQUEST))
                .isPresent();
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1L);

        // 1 beta request row persisted
        long betaCountAfterFirst = betaAccessRequestRepository.count();
        assertThat(betaCountAfterFirst).isEqualTo(1L);

        // 2nd POST same key + same body → replay cached response, KHÔNG insert mới
        MvcResult second = mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .header(IdempotencyHandlerInterceptor.HEADER_IDEMPOTENCY_KEY, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Idempotency-Replay", "true"))
                .andReturn();

        String secondResponseBody = second.getResponse().getContentAsString();
        assertThat(secondResponseBody).isEqualTo(firstResponseBody);

        // Critical: KHÔNG duplicate beta request row
        assertThat(betaAccessRequestRepository.count()).isEqualTo(betaCountAfterFirst);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("No Idempotency-Key header → handler proceeds normally, no cache row")
    void missingHeaderProceedsNormally() throws Exception {
        BetaRequestDto dto = buildDto("no-key-beta@example.test");

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Không idempotency row được write
        assertThat(idempotencyKeyRepository.count()).isZero();
        // Beta request row vẫn được persist
        assertThat(betaAccessRequestRepository.count()).isEqualTo(1L);
    }

    private BetaRequestDto buildDto(String email) {
        return new BetaRequestDto(
                email,
                "Idempotent Tester",
                "Test Center",
                "P2_CENTER_OWNER",
                "google-search",
                "", // honeypot must be empty
                Boolean.TRUE // consentGiven PDPL Art 11
        );
    }
}
