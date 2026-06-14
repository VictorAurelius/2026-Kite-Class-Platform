package com.kiteclass.core.module.grade.controller;

import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.security.AuthorizationBean;
import com.kiteclass.core.module.grade.service.GradeService;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GAP-1301 — Grade-component delete authz closure (BE authz).
 *
 * <p>Cross-flow sweep of GAP-1299: {@code GradeController.deleteComponent} forwarded the
 * client-supplied {@code X-Teacher-Id} header as the acting-teacher identity to the service.
 * The gateway does NOT control that header (GAP-814) → spoofable attribution, even though the
 * {@code @PreAuthorize("@authz.hasAccessToGradeComponent(#id)")} guard already bounds WHO can act.
 *
 * <p>This web-slice IT verifies, with real method security + a mocked service:
 * <ol>
 *   <li><strong>Resource gate</strong> — a caller without access to the component (e.g. STUDENT,
 *       or a teacher who does not own it) is denied; the service is never invoked.</li>
 *   <li><strong>Identity from token</strong> — for an authorized caller the acting teacher is
 *       derived from the authenticated principal ({@code X-User-Reference-Id} → {@code UserContext}),
 *       NOT the client {@code X-Teacher-Id} header. A spoofed {@code X-Teacher-Id} is ignored: the
 *       service is invoked with the token reference id, not the spoofed value.</li>
 * </ol>
 *
 * <p>ADMIN/OWNER bypass at the service layer ({@code GradeServiceImpl.validateTeacherPermission}
 * now uses the OWNER-inclusive {@code AuthorizationBean.isAdmin()}) is exercised end-to-end via the
 * same {@code AuthorizationBean.isAdmin()} mechanism verified in {@code AssignmentAuthzTest}.
 */
@WebMvcTest(GradeController.class)
@AutoConfigureMockMvc
@Import({GradeComponentAuthzTest.TestSecurityConfig.class,
        GradeComponentAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("GAP-1301 — GradeController.deleteComponent authz (X-Teacher-Id spoof closure)")
class GradeComponentAuthzTest {

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
        GradeService gradeService() {
            return Mockito.mock(GradeService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private GradeService gradeService;

    /**
     * Stub the authz bean so SpEL {@code @authz.hasAccessToGradeComponent(#id)} resolves without a
     * full JPA context. Registered as a {@code @MockitoBean} (bean-override) — not a
     * {@code @TestConfiguration @Bean Mockito.mock} — so Spring does not try to inject an
     * {@code EntityManager} into the mock in this JPA-less slice (GAP-1278 precedent).
     */
    @MockitoBean(name = "authz")
    private AuthorizationBean authz;

    private static final Long COMPONENT_ID = 55L;
    private static final Long TEACHER_ID = 5L;

    @BeforeEach
    void seedContext() {
        Mockito.reset(gradeService, authz);
        // GAP-1301: acting teacher derived from the authenticated principal (X-User-Reference-Id →
        // UserContext), NOT the dropped client X-Teacher-Id header. This @WebMvcTest slice does not
        // run the real TenantFilterInterceptor, so seed the thread-local directly.
        UserContext.setCurrentReferenceId(TEACHER_ID);
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    @DisplayName("non-owner (no access to component) — denied, service NOT invoked")
    @WithMockUser
    void nonOwner_isDenied() throws Exception {
        when(authz.hasAccessToGradeComponent(eq(COMPONENT_ID))).thenReturn(false);

        mockMvc.perform(delete("/api/v1/grades/components/{id}", COMPONENT_ID)
                        // spoofed header — must not matter
                        .header("X-Teacher-Id", "999"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 200 && status < 300) {
                        throw new AssertionError("Non-owner delete should NOT succeed, got " + status);
                    }
                });

        Mockito.verifyNoInteractions(gradeService);
    }

    @Test
    @DisplayName("GAP-1301 — owner deletes; spoofed X-Teacher-Id ignored, service gets token id")
    @WithMockUser
    void owner_spoofedHeaderIgnored_usesTokenIdentity() throws Exception {
        when(authz.hasAccessToGradeComponent(eq(COMPONENT_ID))).thenReturn(true);

        // UserContext = TEACHER_ID (token). The request spoofs X-Teacher-Id = 999 (different
        // teacher). The controller no longer reads it → service must be invoked with TEACHER_ID.
        mockMvc.perform(delete("/api/v1/grades/components/{id}", COMPONENT_ID)
                        .header("X-Teacher-Id", "999"))
                .andExpect(status().isNoContent());

        verify(gradeService).deleteComponent(eq(COMPONENT_ID), eq(TEACHER_ID));
        Mockito.verify(gradeService, Mockito.never()).deleteComponent(eq(COMPONENT_ID), eq(999L));
    }
}
