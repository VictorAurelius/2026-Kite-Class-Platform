package com.kitehub.subscription.beta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.beta.dto.BetaApproveCommand;
import com.kitehub.subscription.beta.dto.BetaRejectCommand;
import com.kitehub.subscription.beta.dto.BetaRequestDto;
import com.kitehub.subscription.beta.dto.BetaSignupCommand;
import com.kitehub.subscription.beta.dto.BetaTokenValidationResponse;
import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.beta.service.BetaAccessService;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.RegisterResponse;
import com.kitehub.subscription.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for {@link BetaAccessController} (GAP-372 Wave 33 Bucket C; GAP-384 Wave 35 Bucket A).
 *
 * <p>Uses {@code @WebMvcTest} + {@code @Import(SecurityConfig.class)} so that
 * {@code @EnableMethodSecurity} engages and {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")}
 * actually fires on the admin endpoints. Per
 * {@code feedback_webmvctest_mock_reset.md}: explicit {@code Mockito.reset()}
 * in {@code @BeforeEach} guards against mock-state leak across methods.</p>
 */
@WebMvcTest(controllers = BetaAccessController.class)
@Import(SecurityConfig.class)
@DisplayName("BetaAccessController")
class BetaAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private BetaAccessService service;

    @MockitoBean
    private AuthService authService;

    /** Required because the application enables JPA auditing — slice context resolves auditing beans. */
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        Mockito.reset(service, authService);
    }

    // ── Public endpoints (no auth required) ───────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("POST /api/v1/auth/request-beta-access — 201 on valid payload")
    void submitRequestAcceptsValid() throws Exception {
        BetaRequestDto dto = new BetaRequestDto(
                "owner@example.com", "Owner Name", "ABC Center",
                "P2_CENTER_OWNER", "Friend referral", "", true);
        BetaAccessRequest saved = BetaAccessRequest.builder()
                .id(1L)
                .email(dto.email())
                .name(dto.name())
                .orgName(dto.orgName())
                .persona(dto.persona())
                .status(BetaAccessRequestStatus.PENDING)
                .consentGiven(true)
                .consentAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        // Controller now calls the (dto, ip) overload — stub that signature.
        when(service.submitRequest(any(), any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /request-beta-access — 400 when honeypot non-empty + service.recordHoneypotRejection() invoked (GAP-387 wire-up)")
    void submitRequestRejectsHoneypot() throws Exception {
        BetaRequestDto dto = new BetaRequestDto(
                "spam@example.com", "Bot", "Spam Inc",
                "P1_SOLO_TEACHER", null, "i-am-a-bot", true);

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        // GAP-388 388-A / GAP-387 — verify the controller-level @ExceptionHandler
        // wires the honeypot violation into recordHoneypotRejection so the
        // beta_honeypot_rejections_total counter actually increments + the
        // BetaHoneypotSpike alert rule has data to fire on.
        Mockito.verify(service, Mockito.times(1))
                .recordHoneypotRejection(eq("spam@example.com"), Mockito.anyString());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /request-beta-access — 400 when persona invalid")
    void submitRequestRejectsBadPersona() throws Exception {
        BetaRequestDto dto = new BetaRequestDto(
                "x@y.com", "X", "Y",
                "BADPERSONA", null, "", true);

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // Wave 105 Bucket A — A4 stored-XSS input hardening. DTO @Pattern bans
    // HTML structural chars (< > &) on name + orgName + referralSource so
    // hostile payloads never reach the outbox event payload, email render
    // path, or any future non-React surface.

    @Test
    @WithAnonymousUser
    @DisplayName("POST /request-beta-access — 400 when name contains HTML angle bracket (XSS A4)")
    void submitRequestRejectsXssInName() throws Exception {
        BetaRequestDto dto = new BetaRequestDto(
                "vy@example.com",
                "<script>alert(1)</script>",
                "Trung tâm Sky Education",
                "P2_CENTER_OWNER", null, "", true);

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /request-beta-access — 400 when orgName contains HTML angle bracket (XSS A4)")
    void submitRequestRejectsXssInOrgName() throws Exception {
        BetaRequestDto dto = new BetaRequestDto(
                "vy@example.com",
                "Trần Thị Hồng",
                "<img src=x onerror=alert(1)>",
                "P2_CENTER_OWNER", null, "", true);

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /request-beta-access — 201 when name contains VN diacritics + parentheses (no false positive)")
    void submitRequestAcceptsVietnameseDiacritics() throws Exception {
        // Sanity: VN tenant names with diacritics + parentheses must pass.
        // "Trung tâm Sky Education (Q.1)" + "Trần Thị Hồng" both legitimate.
        BetaRequestDto dto = new BetaRequestDto(
                "hong.tran@skyedu.vn",
                "Trần Thị Hồng",
                "Trung tâm Anh ngữ Sky Education (Q.1)",
                "P2_CENTER_OWNER", null, "", true);
        Mockito.when(service.submitRequest(Mockito.any(), Mockito.any()))
                .thenReturn(com.kitehub.subscription.beta.entity.BetaAccessRequest.builder()
                        .id(1L).email(dto.email()).name(dto.name()).orgName(dto.orgName())
                        .persona(dto.persona()).status(com.kitehub.subscription.beta.entity.BetaAccessRequestStatus.PENDING)
                        .createdAt(java.time.OffsetDateTime.now())
                        .updatedAt(java.time.OffsetDateTime.now())
                        .build());

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    // GAP-385 (Wave 35 Bucket B) — PDPL 2023 Art 11 consent enforcement.

    @Test
    @DisplayName("POST /request-beta-access — 400 BETA_CONSENT_REQUIRED when consent missing (null)")
    void submitRequestRejectsMissingConsent() throws Exception {
        // Send the JSON without the consentGiven field — Jackson maps to null,
        // @NotNull triggers BETA_CONSENT_REQUIRED.
        String body = "{\"email\":\"x@y.com\",\"name\":\"X\",\"orgName\":\"Y\","
                + "\"persona\":\"P2_CENTER_OWNER\",\"referralSource\":null,\"honeypot\":\"\"}";

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /request-beta-access — 400 BETA_CONSENT_REQUIRED when consent=false")
    void submitRequestRejectsFalseConsent() throws Exception {
        BetaRequestDto dto = new BetaRequestDto(
                "x@y.com", "X", "Y",
                "P2_CENTER_OWNER", null, "", false);

        mockMvc.perform(post("/api/v1/auth/request-beta-access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
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

    // ── Beta signup tenant wire-up (GAP-372 closure follow-up #1, Wave 45 Bucket A) ─

    @Test
    @WithAnonymousUser
    @DisplayName("POST /auth/beta-signup — happy path: completeBetaSignup → registerFromBetaInvite both invoked, 200")
    void completeBetaSignupProvisionsTenant() throws Exception {
        UUID token = UUID.randomUUID();
        BetaSignupCommand cmd = new BetaSignupCommand(token, "owner-pass-12345", "abc-school");
        BetaAccessRequest signedUp = BetaAccessRequest.builder()
                .id(42L)
                .email("invitee@example.com")
                .name("Invitee")
                .orgName("ABC School")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.SIGNED_UP)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        when(service.completeBetaSignup(any(BetaSignupCommand.class))).thenReturn(signedUp);
        when(authService.registerFromBetaInvite(eq("ABC School"), eq("abc-school"),
                eq("invitee@example.com"), eq("owner-pass-12345")))
                .thenReturn(RegisterResponse.builder()
                        .accessToken("at").refreshToken("rt")
                        .instance(InstanceResponse.builder().subdomain("abc-school").build())
                        .build());

        mockMvc.perform(post("/api/v1/auth/beta-signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("invitee@example.com"))
                .andExpect(jsonPath("$.status").value("SIGNED_UP"));

        Mockito.verify(authService, Mockito.times(1))
                .registerFromBetaInvite("ABC School", "abc-school",
                        "invitee@example.com", "owner-pass-12345");
        Mockito.verify(service, Mockito.never()).rollbackSignup(Mockito.anyLong());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /auth/beta-signup — registration conflict (subdomain taken) → rollbackSignup + 409")
    void completeBetaSignupRollsBackOnConflict() throws Exception {
        UUID token = UUID.randomUUID();
        BetaSignupCommand cmd = new BetaSignupCommand(token, "owner-pass-12345", "taken-subdomain");
        BetaAccessRequest signedUp = BetaAccessRequest.builder()
                .id(99L)
                .email("invitee@example.com")
                .name("Invitee")
                .orgName("ABC School")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.SIGNED_UP)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        when(service.completeBetaSignup(any(BetaSignupCommand.class))).thenReturn(signedUp);
        when(authService.registerFromBetaInvite(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Subdomain already exists"));

        mockMvc.perform(post("/api/v1/auth/beta-signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isConflict());

        Mockito.verify(service, Mockito.times(1)).rollbackSignup(99L);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /auth/beta-signup — invalid token → 404 + registerFromBetaInvite NOT invoked + no rollback")
    void completeBetaSignupRejectsInvalidToken() throws Exception {
        UUID token = UUID.randomUUID();
        BetaSignupCommand cmd = new BetaSignupCommand(token, "owner-pass-12345", "abc-school");
        when(service.completeBetaSignup(any(BetaSignupCommand.class)))
                .thenThrow(new IllegalArgumentException("Invalid invite token"));

        mockMvc.perform(post("/api/v1/auth/beta-signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isNotFound());

        Mockito.verify(authService, Mockito.never())
                .registerFromBetaInvite(any(), any(), any(), any());
        Mockito.verify(service, Mockito.never()).rollbackSignup(Mockito.anyLong());
    }

    // ── Admin endpoints — happy path (PLATFORM_ADMIN role) ────────────

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("POST /admin/beta-requests/{id}/approve — 200 on PENDING transition (PLATFORM_ADMIN)")
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("POST /admin/beta-requests/{id}/reject — 200 with rejection reason (PLATFORM_ADMIN)")
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Out of geography"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("GET /admin/beta-requests — paginated by status (PLATFORM_ADMIN)")
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

    // ── Admin endpoints — auth guard (GAP-384 Wave 35 Bucket A) ───────

    @Test
    @WithAnonymousUser
    @DisplayName("GAP-384 — unauthenticated GET /admin/beta-requests → 401")
    void unauthenticatedListReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/beta-requests").param("status", "PENDING"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GAP-384 — unauthenticated POST /admin/beta-requests/{id}/approve → 401")
    void unauthenticatedApproveReturns401() throws Exception {
        BetaApproveCommand cmd = new BetaApproveCommand("coord-001");
        mockMvc.perform(post("/api/v1/admin/beta-requests/7/approve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TENANT_USER")
    @DisplayName("GAP-384 — wrong role (TENANT_USER) on POST /admin/beta-requests/{id}/reject → 403")
    void wrongRoleRejectReturns403() throws Exception {
        BetaRejectCommand cmd = new BetaRejectCommand("coord-002", "Out of geography");
        mockMvc.perform(post("/api/v1/admin/beta-requests/9/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isForbidden());
    }
}
