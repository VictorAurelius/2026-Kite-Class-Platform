package com.kiteclass.core.module.k12.controller;

import com.kiteclass.core.module.k12.service.SubjectGradeService;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization tests for {@link SubjectGradeController} (GAP-1491, OWASP A01).
 *
 * <p>The K-12 gradebook lifecycle endpoints were unguarded — a STUDENT/PARENT could submit,
 * review, or publish grades. The fix adds a coarse method-level guard: submit/review =
 * teacher tier; publish/bulk-publish = principal tier (fine-grained Tổ trưởng vs Hiệu
 * trưởng RBAC remains GAP-058/360.2 scope).
 */
@WebMvcTest(SubjectGradeController.class)
@AutoConfigureMockMvc
@Import({SubjectGradeControllerAuthzTest.TestSecurityConfig.class, SubjectGradeControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("SubjectGradeController @PreAuthorize role gate (GAP-1491, OWASP A01)")
class SubjectGradeControllerAuthzTest {

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
        @Bean
        @Primary
        SubjectGradeService subjectGradeService() {
            return Mockito.mock(SubjectGradeService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectGradeService subjectGradeService;

    private static final Long GRADE_ID = 7L;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(subjectGradeService);
    }

    @Test
    @DisplayName("TEACHER → 200 on POST /grades/subjects/{id}/submit-for-review (teacher tier, service invoked)")
    @WithMockUser(roles = "TEACHER")
    void submit_teacher_allowed() throws Exception {
        mockMvc.perform(post("/api/v1/grades/subjects/{id}/submit-for-review", GRADE_ID)
                        .header("X-User-Reference-Id", "1"))
                .andExpect(status().isOk());
        verify(subjectGradeService).submitForReview(GRADE_ID, 1L);
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied POST /grades/subjects/{id}/publish (service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void publish_student_denied() throws Exception {
        mockMvc.perform(post("/api/v1/grades/subjects/{id}/publish", GRADE_ID)
                        .header("X-User-Reference-Id", "1"))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT publish"));
        verifyNoInteractions(subjectGradeService);
    }

    @Test
    @DisplayName("OWASP A01: PARENT → denied POST /grades/subjects/{id}/submit-for-review (service NOT invoked)")
    @WithMockUser(roles = "PARENT")
    void submit_parent_denied() throws Exception {
        mockMvc.perform(post("/api/v1/grades/subjects/{id}/submit-for-review", GRADE_ID)
                        .header("X-User-Reference-Id", "1"))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "PARENT submit"));
        verifyNoInteractions(subjectGradeService);
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
