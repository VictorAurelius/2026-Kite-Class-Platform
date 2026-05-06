package com.kitehub.subscription.consent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.consent.dto.ConsentRequest;
import com.kitehub.subscription.consent.entity.ConsentRecord;
import com.kitehub.subscription.consent.service.ConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link ConsentController} using MockMvc standalone setup.
 *
 * <p>Per {@code feedback_webmvctest_mock_reset.md}: explicit Mockito.reset()
 * in {@code @BeforeEach} guards against mock-state leak between methods.</p>
 *
 * @since Wave 25 Bucket A — GAP-353b
 */
@DisplayName("ConsentController")
class ConsentControllerTest {

    private final ConsentService consentService = Mockito.mock(ConsentService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;
    private UUID visitorId;

    @BeforeEach
    void setUp() {
        // Clean mock state every method (per feedback_webmvctest_mock_reset.md).
        Mockito.reset(consentService);
        ConsentController controller = new ConsentController(consentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        visitorId = UUID.randomUUID();
    }

    private ConsentRecord sampleRecord(boolean revoked) {
        OffsetDateTime now = OffsetDateTime.now();
        return ConsentRecord.builder()
                .id(1L)
                .visitorId(visitorId)
                .essentialConsented(true)
                .analyticsConsented(true)
                .marketingConsented(false)
                .consentVersion(1)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plusMonths(12))
                .revokedAt(revoked ? now : null)
                .build();
    }

    @Test
    @DisplayName("POST /record returns 201 + body for valid request")
    void postRecordReturnsCreated() throws Exception {
        when(consentService.recordConsent(any(ConsentRequest.class))).thenReturn(sampleRecord(false));

        ConsentRequest req = ConsentRequest.builder()
                .visitorId(visitorId)
                .analyticsConsented(true)
                .marketingConsented(false)
                .build();

        mockMvc.perform(post("/api/v1/consent/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitorId").value(visitorId.toString()))
                .andExpect(jsonPath("$.analyticsConsented").value(true))
                .andExpect(jsonPath("$.essentialConsented").value(true));

        verify(consentService).recordConsent(any(ConsentRequest.class));
    }

    @Test
    @DisplayName("POST /record auto-populates ipAddress + userAgent when missing")
    void postRecordAutoPopulatesHeaders() throws Exception {
        when(consentService.recordConsent(any(ConsentRequest.class))).thenAnswer(inv -> {
            ConsentRequest passed = inv.getArgument(0);
            // Verify controller filled in headers before delegating to service.
            org.assertj.core.api.Assertions.assertThat(passed.getIpAddress()).isNotNull();
            org.assertj.core.api.Assertions.assertThat(passed.getUserAgent()).isEqualTo("test-ua/1.0");
            return sampleRecord(false);
        });

        ConsentRequest req = ConsentRequest.builder()
                .visitorId(visitorId)
                .analyticsConsented(false)
                .marketingConsented(false)
                .build();

        mockMvc.perform(post("/api/v1/consent/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "test-ua/1.0")
                        .header("X-Forwarded-For", "203.0.113.5, 10.0.0.1")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /record returns 400 when visitorId missing")
    void postRecordRejectsMissingVisitorId() throws Exception {
        String invalid = "{\"analyticsConsented\":true,\"marketingConsented\":false}";
        mockMvc.perform(post("/api/v1/consent/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());

        verify(consentService, never()).recordConsent(any());
    }

    @Test
    @DisplayName("GET /{visitorId} returns 200 + body when record exists")
    void getReturnsConsent() throws Exception {
        when(consentService.findLatest(visitorId)).thenReturn(Optional.of(sampleRecord(false)));

        mockMvc.perform(get("/api/v1/consent/{visitorId}", visitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitorId").value(visitorId.toString()))
                .andExpect(jsonPath("$.revoked").value(false));
    }

    @Test
    @DisplayName("GET /{visitorId} returns 404 when no record")
    void getReturnsNotFoundForUnknownVisitor() throws Exception {
        when(consentService.findLatest(visitorId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/consent/{visitorId}", visitorId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /{visitorId}/revoke returns 200 + revoked record")
    void postRevokeReturnsRevoked() throws Exception {
        when(consentService.revoke(visitorId)).thenReturn(sampleRecord(true));

        mockMvc.perform(post("/api/v1/consent/{visitorId}/revoke", visitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revoked").value(true))
                .andExpect(jsonPath("$.revokedAt").exists());
    }

    @Test
    @DisplayName("POST /{visitorId}/revoke returns 404 when no record exists")
    void postRevokeReturnsNotFoundForMissing() throws Exception {
        when(consentService.revoke(visitorId)).thenThrow(new NoSuchElementException("not found"));

        mockMvc.perform(post("/api/v1/consent/{visitorId}/revoke", visitorId))
                .andExpect(status().isNotFound());

        verify(consentService).revoke(eq(visitorId));
    }
}
