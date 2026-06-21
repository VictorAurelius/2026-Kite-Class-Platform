package com.kitehub.branding.controller;

import com.kitehub.branding.config.SecurityConfig;
import com.kitehub.branding.service.TemplateGalleryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization + cross-tenant IDOR regression tests for the
 * {@link TemplateGalleryController#applyTemplate} mutation (GAP-1526, OWASP A01).
 * Apply had NO {@code @PreAuthorize} and bound nothing — any caller could apply a template into any
 * tenant's instance via the client {@code X-Instance-Id} header. The fix adds OWNER-tier authz +
 * a {@link com.kitehub.branding.security.TenantOwnershipGuard} check binding X-Instance-Id to the
 * gateway-trusted X-Tenant-Id. (List/detail reads stay public — catalogue carries no tenant data.)
 */
@WebMvcTest(controllers = TemplateGalleryController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("rbac-test")
@DisplayName("TemplateGalleryController.applyTemplate @PreAuthorize + IDOR (GAP-1526, OWASP A01)")
class TemplateGalleryControllerAuthzTest {

    private static final UUID OWN_INSTANCE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_INSTANCE = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID TEMPLATE_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TemplateGalleryService templateService;
    @MockitoBean
    private com.kitehub.branding.wizard.sse.SseTokenService sseTokenService;

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER apply template to own instance → not 403 (write allowed)")
    void owner_applyOwnInstance_allowed() throws Exception {
        when(templateService.applyTemplate(any(), any())).thenReturn(Optional.of("{}"));
        mockMvc.perform(post("/api/platform/branding/templates/{id}/apply", TEMPLATE_ID)
                        .with(csrf())
                        .header("X-Instance-Id", OWN_INSTANCE.toString())
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(result -> assertNotForbidden(result.getResponse().getStatus(),
                        "OWNER apply own instance"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("OWASP A01: STUDENT apply template → 403 (apply is OWNER-tier)")
    void student_apply_denied() throws Exception {
        mockMvc.perform(post("/api/platform/branding/templates/{id}/apply", TEMPLATE_ID)
                        .with(csrf())
                        .header("X-Instance-Id", OWN_INSTANCE.toString())
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWASP A01 IDOR: OWNER apply template to ANOTHER tenant's instance → 403")
    void owner_applyCrossTenant_denied() throws Exception {
        mockMvc.perform(post("/api/platform/branding/templates/{id}/apply", TEMPLATE_ID)
                        .with(csrf())
                        .header("X-Instance-Id", OTHER_INSTANCE.toString())
                        .header("X-Tenant-Id", OWN_INSTANCE.toString()))
                .andExpect(status().isForbidden());
    }

    private static void assertNotForbidden(int statusCode, String label) {
        if (statusCode == 403) {
            throw new AssertionError(label + " must NOT be 403 (allowed role + own tenant)");
        }
    }
}
