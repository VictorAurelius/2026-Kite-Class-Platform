package com.kitehub.subscription.dsar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.dsar.dto.DsarRequest;
import com.kitehub.subscription.dsar.entity.DsarRightType;
import com.kitehub.subscription.dsar.entity.DsarStatus;
import com.kitehub.subscription.dsar.entity.DsarTicket;
import com.kitehub.subscription.dsar.service.DsarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for {@link DsarController}.
 *
 * <p>Per {@code feedback_webmvctest_mock_reset.md}: explicit Mockito.reset() in
 * {@code @BeforeEach} guards against mock-state leak between methods.</p>
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
@DisplayName("DsarController")
class DsarControllerTest {

    private final DsarService dsarService = Mockito.mock(DsarService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(dsarService);
        DsarController controller = new DsarController(dsarService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private DsarRequest sampleRequest() {
        return DsarRequest.builder()
                .rightType(DsarRightType.ACCESS)
                .requesterEmail("subject@example.com")
                .requesterName("Nguyen Test")
                .nationalIdLast4("1234")
                .scope("Profile + email logs")
                .reason("Personal review")
                .build();
    }

    private DsarTicket sampleTicket(DsarStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        return DsarTicket.builder()
                .id(1L)
                .ticketUuid(UUID.randomUUID())
                .rightType(DsarRightType.ACCESS)
                .requesterEmail("subject@example.com")
                .requesterName("Nguyen Test")
                .nationalIdLast4("1234")
                .status(status)
                .slaDeadline(now.plusDays(20))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("POST /request returns 201 with redacted body for valid request")
    void postRequestReturnsCreated() throws Exception {
        DsarTicket ticket = sampleTicket(DsarStatus.PENDING);
        when(dsarService.submitRequest(any(DsarRequest.class))).thenReturn(ticket);

        mockMvc.perform(post("/api/v1/dsar/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(ticket.getTicketUuid().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requesterEmail").doesNotExist())
                .andExpect(jsonPath("$.nationalIdLast4").doesNotExist())
                .andExpect(jsonPath("$.resolution").doesNotExist());
    }

    @Test
    @DisplayName("POST /request returns 400 when honeypot rejected")
    void postRequestRejectsHoneypot() throws Exception {
        when(dsarService.submitRequest(any(DsarRequest.class)))
                .thenThrow(new IllegalArgumentException("invalid request"));

        DsarRequest bot = sampleRequest();
        bot.setCompanyWebsite("http://spam.example");

        mockMvc.perform(post("/api/v1/dsar/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bot)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /request returns 400 on validation error (missing fields)")
    void postRequestValidationError() throws Exception {
        // Missing rightType, email, name, nationalIdLast4 → @Valid fails
        String body = "{\"scope\":\"x\"}";
        mockMvc.perform(post("/api/v1/dsar/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        verify(dsarService, never()).submitRequest(any(DsarRequest.class));
    }

    @Test
    @DisplayName("GET /{ticketId} returns 200 with redacted state when found")
    void getTicketReturnsOk() throws Exception {
        DsarTicket ticket = sampleTicket(DsarStatus.IN_REVIEW);
        when(dsarService.getTicket(ticket.getTicketUuid())).thenReturn(Optional.of(ticket));

        mockMvc.perform(get("/api/v1/dsar/{id}", ticket.getTicketUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticket.getTicketUuid().toString()))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.requesterEmail").doesNotExist());
    }

    @Test
    @DisplayName("GET /{ticketId} returns 404 when not found")
    void getTicketReturnsNotFound() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(dsarService.getTicket(uuid)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/dsar/{id}", uuid))
                .andExpect(status().isNotFound());
    }
}
