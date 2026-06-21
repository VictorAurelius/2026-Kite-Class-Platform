package com.kitehub.branding.controller;

import com.kitehub.branding.config.SecurityConfig;
import com.kitehub.branding.dto.LandingPageContent;
import com.kitehub.branding.service.ContentGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization regression tests for {@link ContentGenerationController}
 * (GAP-1526, OWASP A01). The {@code /content/generate} AI endpoint had NO {@code @PreAuthorize} —
 * any authenticated caller could trigger paid AI generation. The fix adds an OWNER-tier role gate.
 * (No per-instance ownership check applies — the request carries no tenant-scoped resource id.)
 */
@WebMvcTest(controllers = ContentGenerationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("rbac-test")
@DisplayName("ContentGenerationController @PreAuthorize role gate (GAP-1526, OWASP A01)")
class ContentGenerationControllerAuthzTest {

    private static final String BODY =
            "{\"orgName\":\"Trung tâm Sky\",\"language\":\"vi\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentGenerationService contentGenerationService;
    @MockitoBean
    private com.kitehub.branding.wizard.sse.SseTokenService sseTokenService;

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER POST /content/generate → not 403 (write allowed)")
    void owner_generate_allowed() throws Exception {
        when(contentGenerationService.generateLandingPageContent(any(), anyString(), anyString()))
                .thenReturn(Mono.just(new LandingPageContent()));
        mockMvc.perform(post("/api/platform/branding/content/generate")
                        .with(csrf())
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(result -> assertNotForbidden(result.getResponse().getStatus(),
                        "OWNER generate content"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("OWASP A01: STUDENT POST /content/generate → 403 (generate is OWNER-tier)")
    void student_generate_denied() throws Exception {
        mockMvc.perform(post("/api/platform/branding/content/generate")
                        .with(csrf())
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("OWASP A01: TEACHER (staff) POST /content/generate → 403 (not write-eligible)")
    void teacher_generate_denied() throws Exception {
        mockMvc.perform(post("/api/platform/branding/content/generate")
                        .with(csrf())
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    private static void assertNotForbidden(int statusCode, String label) {
        if (statusCode == 403) {
            throw new AssertionError(label + " must NOT be 403 (allowed role)");
        }
    }
}
