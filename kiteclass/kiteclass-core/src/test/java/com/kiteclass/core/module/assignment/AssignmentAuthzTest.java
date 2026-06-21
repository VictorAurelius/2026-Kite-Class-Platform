package com.kiteclass.core.module.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.security.AuthorizationBean;
import com.kiteclass.core.module.assignment.controller.AssignmentController;
import com.kiteclass.core.module.assignment.dto.request.CreateAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.response.AssignmentResponse;
import com.kiteclass.core.module.assignment.service.AssignmentService;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GAP-1301 — Assignment authoring authz closure (BE authz, web slice).
 *
 * <p>Cross-flow sweep of GAP-1299: {@code AssignmentController} write endpoints read the
 * client-supplied {@code X-Teacher-Id} header as the acting-teacher identity. The gateway does NOT
 * control that header (GAP-814) → a caller could attribute ops to an arbitrary teacher, and a
 * STUDENT could act as a teacher entirely.
 *
 * <p>This {@code @WebMvcTest} slice verifies the controller authz contract with real method
 * security + a mocked service:
 * <ol>
 *   <li><strong>Role gate</strong> — {@code @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")}
 *       blocks STUDENT/PARENT (403); the service is never invoked.</li>
 *   <li><strong>Identity from token</strong> — the acting teacher is derived from the authenticated
 *       principal ({@code X-User-Reference-Id} → {@code UserContext}), NOT the client
 *       {@code X-Teacher-Id} header. A spoofed {@code X-Teacher-Id} is ignored: the service is
 *       invoked with the token reference id, not the spoofed value.</li>
 * </ol>
 *
 * <p>The ADMIN/OWNER service-layer bypass of the per-class MAIN_TEACHER check uses
 * {@code AuthorizationBean.isAdmin()} and is covered by unit tests in {@code AssignmentServiceTest}.
 */
@WebMvcTest(AssignmentController.class)
@AutoConfigureMockMvc
@Import({AssignmentAuthzTest.TestSecurityConfig.class, AssignmentAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("GAP-1301 — AssignmentController authz (role gate + X-Teacher-Id spoof closure)")
class AssignmentAuthzTest {

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
        AssignmentService assignmentService() {
            return Mockito.mock(AssignmentService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private AssignmentService assignmentService;
    @Autowired private ObjectMapper objectMapper;

    /** Bean-override mock for the {@code @authz.hasAccessToClass} SpEL on sibling read endpoints. */
    @MockitoBean(name = "authz")
    private AuthorizationBean authz;

    private static final Long CLASS_ID = 50L;
    private static final Long TEACHER_ID = 100L;

    @BeforeEach
    void seedContext() {
        Mockito.reset(assignmentService, authz);
        // GAP-1301: acting teacher derived from the authenticated principal (X-User-Reference-Id →
        // UserContext), NOT the dropped client X-Teacher-Id header. This @WebMvcTest slice does not
        // run the real TenantFilterInterceptor → seed the thread-local.
        UserContext.setCurrentReferenceId(TEACHER_ID);
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    private String createBody() throws Exception {
        return objectMapper.writeValueAsString(CreateAssignmentRequest.builder()
                .classId(CLASS_ID)
                .title("Homework Authz")
                .description("Authz test assignment")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(20))
                .allowLateSubmission(true)
                .latePenaltyPercent(BigDecimal.valueOf(10))
                .build());
    }

    // ── Layer 1: role gate — STUDENT / PARENT blocked entirely ──────────────

    @Test
    @DisplayName("STUDENT cannot create an assignment — 403; service NOT invoked")
    @WithMockUser(roles = "STUDENT")
    void student_cannotCreateAssignment() throws Exception {
        mockMvc.perform(post("/api/v1/assignments")
                        // spoofed header — must not matter
                        .header("X-Teacher-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden());
        Mockito.verifyNoInteractions(assignmentService);
    }

    @Test
    @DisplayName("PARENT cannot create an assignment — 403; service NOT invoked")
    @WithMockUser(roles = "PARENT")
    void parent_cannotCreateAssignment() throws Exception {
        mockMvc.perform(post("/api/v1/assignments")
                        .header("X-Teacher-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden());
        Mockito.verifyNoInteractions(assignmentService);
    }

    // ── Layer 2: identity from token — X-Teacher-Id spoof ignored ───────────

    @Test
    @DisplayName("GAP-1301 — TEACHER creates; spoofed X-Teacher-Id ignored, service gets token id")
    @WithMockUser(roles = "TEACHER")
    void teacher_spoofedHeaderIgnored_usesTokenIdentity() throws Exception {
        when(assignmentService.createAssignment(any(), eq(TEACHER_ID)))
                .thenReturn(AssignmentResponse.builder().id(1L).build());

        // UserContext = TEACHER_ID (token). The request spoofs X-Teacher-Id = 999 (different
        // teacher). The controller no longer reads it → service must be invoked with TEACHER_ID.
        mockMvc.perform(post("/api/v1/assignments")
                        .header("X-Teacher-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated());

        verify(assignmentService).createAssignment(any(), eq(TEACHER_ID));
        Mockito.verify(assignmentService, Mockito.never()).createAssignment(any(), eq(999L));
    }

    // ── GAP-1527 (OWASP A01): submission reads were unguarded → now teacher/staff-tier;
    //    POST /submit is now STUDENT-only so a teacher cannot spoof a student submission. ──

    @Test
    @DisplayName("GAP-1527 OWASP A01: STUDENT → denied GET /submissions/{id} (service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void getSubmissionById_student_denied() throws Exception {
        mockMvc.perform(get("/api/v1/assignments/submissions/{id}", 5L)
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000001"))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT getSubmissionById"));
        verifyNoInteractions(assignmentService);
    }

    @Test
    @DisplayName("GAP-1527: TEACHER → 200 on GET /{assignmentId}/submissions (read tier)")
    @WithMockUser(roles = "TEACHER")
    void getSubmissionsByAssignment_teacher_allowed() throws Exception {
        when(assignmentService.getSubmissionsByAssignment(9L))
                .thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(get("/api/v1/assignments/{assignmentId}/submissions", 9L)
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isOk());
        verify(assignmentService).getSubmissionsByAssignment(9L);
    }

    @Test
    @DisplayName("GAP-1527 OWASP A01: TEACHER → denied POST /submit (STUDENT-only; service NOT invoked)")
    @WithMockUser(roles = "TEACHER")
    void submit_teacher_denied() throws Exception {
        mockMvc.perform(post("/api/v1/assignments/submit")
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000001")
                        .header("X-User-Id", "55")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentId\":7,\"contentUrl\":\"https://x/y\"}"))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "TEACHER submit"));
        verifyNoInteractions(assignmentService);
    }

    @Test
    @DisplayName("GAP-1527: STUDENT → 201 on POST /submit (student can submit their own work)")
    @WithMockUser(roles = "STUDENT")
    void submit_student_allowed() throws Exception {
        when(assignmentService.submitAssignment(any(), eq(55L))).thenReturn(null);
        mockMvc.perform(post("/api/v1/assignments/submit")
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000001")
                        .header("X-User-Id", "55")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentId\":7,\"contentUrl\":\"https://x/y\"}"))
                .andExpect(status().isCreated());
        verify(assignmentService).submitAssignment(any(), eq(55L));
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
