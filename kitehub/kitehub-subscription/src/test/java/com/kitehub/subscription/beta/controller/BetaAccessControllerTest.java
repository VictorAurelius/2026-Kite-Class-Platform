package com.kitehub.subscription.beta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.beta.dto.BetaApproveCommand;
import com.kitehub.subscription.beta.dto.BetaRejectCommand;
import com.kitehub.subscription.beta.dto.BetaRequestDto;
import com.kitehub.subscription.beta.dto.BetaTokenValidationResponse;
import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.beta.service.BetaAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for {@link BetaAccessController} (GAP-372 Wave 33 Bucket C).
 *
 * <p>Per {@code feedback_webmvctest_mock_reset.md}: explicit {@code Mockito.reset()}
 * in {@code @BeforeEach} guards against mock-state leak across methods.</p>
 */
@DisplayName("BetaAccessController")
class BetaAccessControllerTest {

    private final BetaAccessService service = Mockito.mock(BetaAccessService.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(service);
        BetaAccessController controller = new BetaAccessController(service);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/request-beta-access — 201 on valid payload")
    void submitRequestAcceptsValid() throws Exception {
        BetaRequestDto dto = new BetaRequestDto(
                "owner@example.com", "Owner Name", "ABC Center",
                "P2_CENTER_OWNER", "Friend referral", "");
        BetaAccessRequest saved = BetaAccessRequest.builder()
                .id(1L)
                .email(dto.email())
                .name(dto.name())
                .orgName(dto.orgName())
                .persona(dto.persona())
                .status(BetaAccessRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        when(service.submitRequest(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /request-beta-access — 400 when honeypot non-empty")
    void submitRequestRejectsHoneypot() throws Exception {
        BetaRequestDto dto = new BetaRequestDto(
                "spam@example.com", "Bot", "Spam Inc",
                "P1_SOLO_TEACHER", null, "i-am-a-bot");

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /request-beta-access — 400 when persona invalid")
    void submitRequestRejectsBadPersona() throws Exception {
        BetaRequestDto dto = new BetaRequestDto(
                "x@y.com", "X", "Y",
                "BADPERSONA", null, "");

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /admin/beta-requests/{id}/approve — 200 on PENDING transition")
    void approvePromotesPendingToApproved() throws Exception {
        BetaApproveCommand cmd = new BetaApproveCommand("coord-001");
        BetaAccessRequest approved = BetaAccessRequest.builder()
                .id(7L)
                .email("inv@example.com")
                .name("Inv")
                .orgName("Inv Org")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.APPROVED)
                .approverId("coord-001")
                .approvedAt(OffsetDateTime.now())
                .inviteToken(UUID.randomUUID())
                .inviteTokenExpiry(OffsetDateTime.now().plusHours(24))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        when(service.approveRequest(eq(7L), any())).thenReturn(approved);

        mockMvc.perform(post("/api/v1/admin/beta-requests/7/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /admin/beta-requests/{id}/reject — 200 with rejection reason")
    void rejectMarksRejected() throws Exception {
        BetaRejectCommand cmd = new BetaRejectCommand("coord-002", "Out of geography");
        BetaAccessRequest rejected = BetaAccessRequest.builder()
                .id(9L)
                .email("nope@example.com")
                .name("Nope")
                .orgName("Nope Inc")
                .persona("P1_SOLO_TEACHER")
                .status(BetaAccessRequestStatus.REJECTED)
                .approverId("coord-002")
                .rejectedAt(OffsetDateTime.now())
                .rejectionReason("Out of geography")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        when(service.rejectRequest(eq(9L), any())).thenReturn(rejected);

        mockMvc.perform(post("/api/v1/admin/beta-requests/9/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Out of geography"));
    }

    @Test
    @DisplayName("GET /auth/beta-signup/validate — 200 valid + 404 invalid")
    void validateTokenSurfacesErrorCode() throws Exception {
        UUID validToken = UUID.randomUUID();
        UUID expiredToken = UUID.randomUUID();
        when(service.validateToken(validToken)).thenReturn(
                BetaTokenValidationResponse.ok("ok@x.com", "Ok", "Ok Inc", "P2_CENTER_OWNER"));
        when(service.validateToken(expiredToken)).thenReturn(
                BetaTokenValidationResponse.invalid("TOKEN_EXPIRED"));

        mockMvc.perform(get("/api/v1/auth/beta-signup/validate")
                        .param("token", validToken.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("ok@x.com"));

        mockMvc.perform(get("/api/v1/auth/beta-signup/validate")
                        .param("token", expiredToken.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errorCode").value("TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("GET /admin/beta-requests — paginated by status")
    void listsByStatus() throws Exception {
        BetaAccessRequest a = BetaAccessRequest.builder()
                .id(1L).email("a@x.com").name("A").orgName("AO")
                .persona("P1_SOLO_TEACHER")
                .status(BetaAccessRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();
        Page<BetaAccessRequest> page = new PageImpl<>(List.of(a));
        when(service.listByStatus(eq(BetaAccessRequestStatus.PENDING), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/beta-requests").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("a@x.com"));
    }
}
