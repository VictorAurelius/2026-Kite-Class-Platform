package com.kitehub.subscription.impersonation;

import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.impersonation.dto.ImpersonationEndResponse;
import com.kitehub.subscription.impersonation.dto.ImpersonationStartResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
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
 * MockMvc tests for {@link ImpersonationController} (GAP-040 Wave 79 F-bis).
 *
 * <p>Verifies the PLATFORM_ADMIN role guard fires (defense in depth) and the
 * response shape matches the DTO contract. Uses {@code @WebMvcTest} +
 * {@code @Import(SecurityConfig.class)} so {@code @EnableMethodSecurity}
 * engages — same pattern as {@code BetaAccessControllerTest}.</p>
 */
@WebMvcTest(controllers = ImpersonationController.class)
@Import(SecurityConfig.class)
@DisplayName("ImpersonationController")
class ImpersonationControllerTest {

    private static final String ADMIN_USER_ID = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImpersonationService service;

    /** Required because the application enables JPA auditing — slice context resolves auditing beans. */
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        Mockito.reset(service);
    }

    @Test
    @WithMockUser(username = ADMIN_USER_ID, roles = "PLATFORM_ADMIN")
    @DisplayName("POST /api/v1/admin/impersonate/{slug}: 200 + token response for PLATFORM_ADMIN")
    void start_as_platform_admin_returns_200() throws Exception {
        UUID tenantId = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(30);
        ImpersonationStartResponse resp = new ImpersonationStartResponse(
                7L, "fake.jwt.token", tenantId, "acme", expiresAt);
        when(service.startImpersonation(any(), eq("acme"), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/admin/impersonate/acme").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(7))
                .andExpect(jsonPath("$.impersonationToken").value("fake.jwt.token"))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.tenantSlug").value("acme"));
    }

    @Test
    @WithMockUser(username = "tenant-user", roles = "TENANT_OWNER")
    @DisplayName("POST /api/v1/admin/impersonate/{slug}: 403 when caller lacks PLATFORM_ADMIN role")
    void start_as_non_admin_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/impersonate/acme").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /api/v1/admin/impersonate/{slug}: 401 when anonymous")
    void start_anonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/impersonate/acme").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = ADMIN_USER_ID, roles = "PLATFORM_ADMIN")
    @DisplayName("POST /api/v1/admin/impersonate/end: 200 + MANUAL_EXIT reason")
    void end_as_platform_admin_returns_200() throws Exception {
        ImpersonationEndResponse resp = new ImpersonationEndResponse(
                42L, OffsetDateTime.now(), ImpersonationAuditEntry.EndedReason.MANUAL_EXIT);
        when(service.endImpersonation(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/admin/impersonate/end").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(42))
                .andExpect(jsonPath("$.endedReason").value("MANUAL_EXIT"));
    }

    @Test
    @WithMockUser(username = "x", roles = "TENANT_OWNER")
    @DisplayName("POST /api/v1/admin/impersonate/end: 403 for non-admin")
    void end_as_non_admin_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/impersonate/end").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_USER_ID, roles = "PLATFORM_ADMIN")
    @DisplayName("GET /api/v1/admin/impersonate/audit-log: 200 + paginated rows")
    void audit_log_as_platform_admin_returns_200() throws Exception {
        ImpersonationAuditEntry entry = ImpersonationAuditEntry.builder()
                .id(1L)
                .adminUserId(UUID.fromString(ADMIN_USER_ID))
                .tenantId(UUID.randomUUID())
                .tenantSlug("acme")
                .startedAt(OffsetDateTime.now())
                .build();
        when(service.listAuditLog(any())).thenReturn(new PageImpl<>(List.of(entry)));

        mockMvc.perform(get("/api/v1/admin/impersonate/audit-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].tenantSlug").value("acme"));
    }

    @Test
    @WithMockUser(username = "x", roles = "TENANT_OWNER")
    @DisplayName("GET /api/v1/admin/impersonate/audit-log: 403 for non-admin")
    void audit_log_as_non_admin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/impersonate/audit-log"))
                .andExpect(status().isForbidden());
    }
}
