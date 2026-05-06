package com.kitehub.subscription.consent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.consent.dto.ConsentRequest;
import com.kitehub.subscription.consent.dto.ConsentResponse;
import com.kitehub.subscription.consent.repository.ConsentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the consent endpoints.
 *
 * <p>Uses the {@code application-test.yml} profile (H2 in-memory + Flyway
 * disabled + JPA {@code create-drop}) — same convention as
 * {@code InstanceProvisioningIT}. The V25 Flyway migration is exercised
 * separately by Flyway's own validation in non-test profiles; here we
 * exercise the JPA round-trip + endpoint wiring.</p>
 *
 * @since Wave 25 Bucket A — GAP-353b
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Consent endpoints IT")
class ConsentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConsentRecordRepository repository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Full lifecycle: record → get → revoke → re-record")
    void fullLifecycle() throws Exception {
        UUID visitorId = UUID.randomUUID();
        ConsentRequest first = ConsentRequest.builder()
                .visitorId(visitorId)
                .analyticsConsented(true)
                .marketingConsented(false)
                .build();

        // 1. POST /record (insert)
        MvcResult created = mockMvc.perform(post("/api/v1/consent/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitorId").value(visitorId.toString()))
                .andExpect(jsonPath("$.analyticsConsented").value(true))
                .andExpect(jsonPath("$.essentialConsented").value(true))
                .andReturn();

        ConsentResponse firstBody = objectMapper.readValue(
                created.getResponse().getContentAsString(), ConsentResponse.class);
        assertThat(firstBody.getId()).isNotNull();
        assertThat(firstBody.getExpiresAt()).isAfter(firstBody.getCreatedAt());

        // 2. GET /{visitorId}
        mockMvc.perform(get("/api/v1/consent/{visitorId}", visitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstBody.getId().intValue()))
                .andExpect(jsonPath("$.revoked").value(false));

        // 3. POST /record again (idempotent update — same id, flipped marketing)
        ConsentRequest second = ConsentRequest.builder()
                .visitorId(visitorId)
                .analyticsConsented(true)
                .marketingConsented(true)
                .build();
        mockMvc.perform(post("/api/v1/consent/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(firstBody.getId().intValue()))
                .andExpect(jsonPath("$.marketingConsented").value(true));

        assertThat(repository.count()).isEqualTo(1);

        // 4. POST /{visitorId}/revoke
        mockMvc.perform(post("/api/v1/consent/{visitorId}/revoke", visitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revoked").value(true))
                .andExpect(jsonPath("$.analyticsConsented").value(false))
                .andExpect(jsonPath("$.marketingConsented").value(false));

        // 5. POST /record after revoke → NEW record (audit trail honesty).
        mockMvc.perform(post("/api/v1/consent/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated());

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("GET unknown visitor returns 404")
    void getUnknownReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/consent/{visitorId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /revoke unknown visitor returns 404")
    void revokeUnknownReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/consent/{visitorId}/revoke", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /record without visitorId returns 400")
    void postRecordWithoutVisitorIdReturns400() throws Exception {
        String body = "{\"analyticsConsented\":true,\"marketingConsented\":false}";
        mockMvc.perform(post("/api/v1/consent/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
