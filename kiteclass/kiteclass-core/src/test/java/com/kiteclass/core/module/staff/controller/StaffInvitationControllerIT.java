package com.kiteclass.core.module.staff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.StaffInvitationStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.staff.dto.AcceptStaffInviteRequest;
import com.kiteclass.core.module.staff.dto.AcceptStaffInviteResult;
import com.kiteclass.core.module.staff.dto.InviteStaffRequest;
import com.kiteclass.core.module.staff.dto.StaffInvitationResponse;
import com.kiteclass.core.module.staff.service.StaffInvitationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice tests for {@link StaffInvitationController} (Wave meta-6 Bucket A — GAP-772).
 *
 * <p>Endpoint coverage matrix (8 tests):
 * <ul>
 *   <li>POST {@code /api/v1/staff-invitations}              — happy path 201 + missing user 401
 *       + wrong role 403 + invalid body 400</li>
 *   <li>GET  {@code /api/v1/staff-invitations}              — happy path 200 (Owner list)</li>
 *   <li>DELETE {@code /api/v1/staff-invitations/{id}}       — happy path 200 + 404 not found</li>
 *   <li>POST {@code /api/v1/staff-invitations/{token}/accept} — happy path 200 (public, no auth)</li>
 * </ul>
 *
 * <p>Wave meta-6 follow-up — GAP-782 Bucket A item 2 test coverage. Mirrors the
 * web-slice pattern in {@code AttendanceClassBatchControllerIT} for role-guarded
 * endpoints + {@code DocumentGenerationControllerTest} for TenantContext binding.
 *
 * @since 2026-05-28 (Wave meta-6 follow-up — GAP-782)
 */
@WebMvcTest(StaffInvitationController.class)
@AutoConfigureMockMvc
@Import({StaffInvitationControllerIT.TestSecurityConfig.class,
        StaffInvitationControllerIT.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("StaffInvitationController IT")
class StaffInvitationControllerIT {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary
        StaffInvitationService invitationService() {
            return Mockito.mock(StaffInvitationService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private StaffInvitationService invitationService;
    @Autowired private ObjectMapper objectMapper;

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Long INVITER_ID = 7L;
    private static final String STAFF_EMAIL = "hong.tran@skyedu.vn";
    private static final String ROLE_STAFF = "STAFF";
    private static final String FULL_NAME = "Trần Thị Hồng";
    private static final String STRONG_PASSWORD = "Str0ng!Pass";

    @BeforeEach
    void bindContext() {
        // @TestConfiguration beans are JVM-singleton; reset to avoid invocation
        // pollution across tests sharing the same mock instance.
        Mockito.reset(invitationService);

        // Controller reads UUID + Long via TenantContext / UserContext ThreadLocal
        // (populated in production by TenantFilterInterceptor from X-Tenant-Id +
        // X-User-Id headers). Tests set them directly per DocumentGenerationControllerTest pattern.
        TenantContext.setCurrentTenant(TENANT);
        UserContext.setCurrentUser(INVITER_ID);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        UserContext.clear();
    }

    // ——— POST /api/v1/staff-invitations ————————————————————————

    @Test
    @DisplayName("POST /api/v1/staff-invitations — 201 returns invitation with token (Owner)")
    @WithMockUser(roles = "OWNER")
    void invite_happyPath_owner() throws Exception {
        StaffInvitationResponse stub = new StaffInvitationResponse(
                100L, STAFF_EMAIL, ROLE_STAFF,
                /* token */ "11111111-2222-3333-4444-555555555555",
                StaffInvitationStatus.PENDING,
                Instant.parse("2026-06-04T00:00:00Z"),
                INVITER_ID, null, null,
                Instant.parse("2026-05-28T00:00:00Z"));
        when(invitationService.invite(eq(TENANT), eq(STAFF_EMAIL), eq(ROLE_STAFF), eq(INVITER_ID)))
                .thenReturn(stub);

        String body = objectMapper.writeValueAsString(
                new InviteStaffRequest(STAFF_EMAIL, ROLE_STAFF));

        mockMvc.perform(post("/api/v1/staff-invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(STAFF_EMAIL))
                .andExpect(jsonPath("$.data.role").value(ROLE_STAFF))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.token").value("11111111-2222-3333-4444-555555555555"));

        verify(invitationService).invite(eq(TENANT), eq(STAFF_EMAIL), eq(ROLE_STAFF), eq(INVITER_ID));
    }

    @Test
    @DisplayName("POST /api/v1/staff-invitations — 403 when caller has wrong role (STAFF tries to invite)")
    @WithMockUser(roles = "STAFF")
    void invite_wrongRole_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(
                new InviteStaffRequest(STAFF_EMAIL, ROLE_STAFF));

        mockMvc.perform(post("/api/v1/staff-invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        verify(invitationService, Mockito.never())
                .invite(any(UUID.class), any(String.class), any(String.class), any(Long.class));
    }

    @Test
    @DisplayName("POST /api/v1/staff-invitations — 400 on validation failure (invalid role)")
    @WithMockUser(roles = "OWNER")
    void invite_invalidRole_returns400() throws Exception {
        // Role must be STAFF / TEACHER / MANAGER per InviteStaffRequest @Pattern
        String body = objectMapper.writeValueAsString(
                new InviteStaffRequest(STAFF_EMAIL, "BLEH"));

        mockMvc.perform(post("/api/v1/staff-invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(invitationService, Mockito.never())
                .invite(any(UUID.class), any(String.class), any(String.class), any(Long.class));
    }

    @Test
    @DisplayName("POST /api/v1/staff-invitations — 401 when UserContext is missing (gateway upstream skip)")
    @WithMockUser(roles = "OWNER")
    void invite_missingUserContext_returns401() throws Exception {
        // Production: gateway always populates UserContext via TenantFilterInterceptor.
        // If the chain skips it (mis-routed request), controller raises AUTH_REQUIRED.
        UserContext.clear();

        String body = objectMapper.writeValueAsString(
                new InviteStaffRequest(STAFF_EMAIL, ROLE_STAFF));

        mockMvc.perform(post("/api/v1/staff-invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(invitationService, Mockito.never())
                .invite(any(UUID.class), any(String.class), any(String.class), any(Long.class));
    }

    // ——— GET /api/v1/staff-invitations ————————————————————————

    @Test
    @DisplayName("GET /api/v1/staff-invitations — 200 lists PENDING invitations (Admin)")
    @WithMockUser(roles = "ADMIN")
    void list_happyPath_admin() throws Exception {
        StaffInvitationResponse row = new StaffInvitationResponse(
                10L, STAFF_EMAIL, ROLE_STAFF,
                /* token omitted on list */ null,
                StaffInvitationStatus.PENDING,
                Instant.parse("2026-06-04T00:00:00Z"),
                INVITER_ID, null, null,
                Instant.parse("2026-05-28T00:00:00Z"));
        when(invitationService.listForTenant(eq(TENANT))).thenReturn(List.of(row));

        mockMvc.perform(get("/api/v1/staff-invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].email").value(STAFF_EMAIL))
                // Token MUST be omitted on list payload to reduce leak surface.
                .andExpect(jsonPath("$.data[0].token").doesNotExist());

        verify(invitationService).listForTenant(eq(TENANT));
    }

    // ——— DELETE /api/v1/staff-invitations/{id} ————————————————————

    @Test
    @DisplayName("DELETE /api/v1/staff-invitations/{id} — 200 revokes successfully (PLATFORM_ADMIN)")
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void revoke_happyPath() throws Exception {
        Mockito.doNothing().when(invitationService).revoke(eq(TENANT), eq(10L));

        mockMvc.perform(delete("/api/v1/staff-invitations/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lời mời đã bị hủy"));

        verify(invitationService).revoke(eq(TENANT), eq(10L));
    }

    @Test
    @DisplayName("DELETE /api/v1/staff-invitations/{id} — 404 when row is missing")
    @WithMockUser(roles = "OWNER")
    void revoke_notFound() throws Exception {
        Mockito.doThrow(new BusinessException("STAFF_INVITATION_NOT_FOUND", HttpStatus.NOT_FOUND))
                .when(invitationService).revoke(eq(TENANT), eq(404L));

        mockMvc.perform(delete("/api/v1/staff-invitations/{id}", 404L))
                .andExpect(status().isNotFound());

        verify(invitationService).revoke(eq(TENANT), eq(404L));
    }

    // ——— POST /api/v1/staff-invitations/{token}/accept ————————————

    @Test
    @DisplayName("POST /api/v1/staff-invitations/{token}/accept — 200 returns credential payload (public, X-Tenant-Id header)")
    void accept_happyPath_publicEndpoint() throws Exception {
        // Public endpoint — no Spring Security principal needed; gateway populates
        // tenant via X-Tenant-Id header (the controller parses it directly).
        // Clear UserContext to demonstrate no auth required.
        UserContext.clear();

        AcceptStaffInviteResult stub = new AcceptStaffInviteResult(
                10L, TENANT, STAFF_EMAIL, FULL_NAME, ROLE_STAFF,
                Instant.parse("2026-05-28T00:00:00Z"));
        when(invitationService.accept(eq(TENANT), eq("tok"), any(AcceptStaffInviteRequest.class)))
                .thenReturn(stub);

        String body = objectMapper.writeValueAsString(
                new AcceptStaffInviteRequest(FULL_NAME, STRONG_PASSWORD));

        mockMvc.perform(post("/api/v1/staff-invitations/{token}/accept", "tok")
                        .header("X-Tenant-Id", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invitationId").value(10))
                .andExpect(jsonPath("$.data.email").value(STAFF_EMAIL))
                .andExpect(jsonPath("$.data.role").value(ROLE_STAFF))
                // VN diacritic round-trip per vn-localization-audit-checklist.md §5
                // — full name preserved through controller → JSON serialize → response.
                .andExpect(jsonPath("$.data.fullName").value("Trần Thị Hồng"));

        verify(invitationService).accept(eq(TENANT), eq("tok"), any(AcceptStaffInviteRequest.class));
    }
}
