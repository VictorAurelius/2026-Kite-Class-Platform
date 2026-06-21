package com.kitehub.branding.lifecycle;

import com.kitehub.branding.config.SecurityConfig;
import com.kitehub.branding.lifecycle.repository.BrandingInstanceStateRepository;
import com.kitehub.branding.lifecycle.repository.BrandingLifecycleEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization + cross-tenant IDOR regression tests for {@link LifecycleEventsController}
 * (GAP-1526, OWASP A01). The tenant-facing {@code deploy-status} + {@code lifecycle/events} endpoints
 * had NO {@code @PreAuthorize} and NO tenant binding — anyone could read any tenant's deploy/lifecycle
 * state by instanceId. The fix adds READ-tier authz + a
 * {@link com.kitehub.branding.security.TenantOwnershipGuard} check on the path instanceId.
 */
@WebMvcTest(controllers = LifecycleEventsController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("rbac-test")
@DisplayName("LifecycleEventsController @PreAuthorize + IDOR ownership (GAP-1526, OWASP A01)")
class LifecycleEventsControllerAuthzTest {

    private static final UUID OWN_INSTANCE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_TENANT = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandingLifecycleEventRepository eventRepo;
    @MockitoBean
    private BrandingInstanceStateRepository stateRepo;
    // NB: real auto-configured ObjectMapper — mocking it breaks Spring MVC Jackson converters.
    @MockitoBean
    private com.kitehub.branding.wizard.sse.SseTokenService sseTokenService;

    @BeforeEach
    void setUp() {
        when(eventRepo.findByInstanceIdSince(any(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(stateRepo.findById(eq(OWN_INSTANCE))).thenReturn(Optional.empty());
    }

    // ---- lifecycle/events --------------------------------------------------

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER GET own-tenant lifecycle events → not 403 (read allowed)")
    void owner_getEventsOwnInstance_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/branding/instances/{id}/lifecycle/events", OWN_INSTANCE)
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(result -> assertNotForbidden(result.getResponse().getStatus(),
                        "OWNER read own lifecycle events"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("OWASP A01: STUDENT GET lifecycle events → 403 (read is staff-tier)")
    void student_getEvents_denied() throws Exception {
        mockMvc.perform(get("/api/v1/branding/instances/{id}/lifecycle/events", OWN_INSTANCE)
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWASP A01 IDOR: OWNER GET another tenant's lifecycle events → 403")
    void owner_getEventsCrossTenant_denied() throws Exception {
        mockMvc.perform(get("/api/v1/branding/instances/{id}/lifecycle/events", OWN_INSTANCE)
                        .header("X-Tenant-Id", OTHER_TENANT.toString()))
                .andExpect(status().isForbidden());
    }

    // ---- deploy-status -----------------------------------------------------

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("STAFF (TEACHER) GET own-tenant deploy-status → not 403 (staff read allowed)")
    void teacher_getDeployStatusOwn_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/branding/instances/{id}/deploy-status", OWN_INSTANCE)
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(result -> assertNotForbidden(result.getResponse().getStatus(),
                        "TEACHER read own deploy-status"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWASP A01 IDOR: OWNER GET another tenant's deploy-status → 403")
    void owner_getDeployStatusCrossTenant_denied() throws Exception {
        mockMvc.perform(get("/api/v1/branding/instances/{id}/deploy-status", OWN_INSTANCE)
                        .header("X-Tenant-Id", OTHER_TENANT.toString()))
                .andExpect(status().isForbidden());
    }

    private static void assertNotForbidden(int statusCode, String label) {
        if (statusCode == 403) {
            throw new AssertionError(label + " must NOT be 403 (allowed role + own tenant)");
        }
    }
}
