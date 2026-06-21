package com.kiteclass.core.module.teacher.controller;

import com.kiteclass.core.module.teacher.service.TeacherService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization tests for {@link TeacherController} (GAP-1491, OWASP A01).
 *
 * <p>Only {@code setTeacherCredential} was guarded; create/read/update/delete were open.
 * The fix guards HR mutations (create/update/delete) at the admin tier
 * ({@code hasAnyRole('OWNER','ADMIN','PRINCIPAL','PLATFORM_ADMIN')} — NOT TEACHER, so a
 * teacher cannot delete/create teachers laterally) while the staff-directory reads use the
 * broader staff+teacher tier.
 */
@WebMvcTest(TeacherController.class)
@AutoConfigureMockMvc
@Import({TeacherControllerAuthzTest.TestSecurityConfig.class, TeacherControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("TeacherController @PreAuthorize role gate (GAP-1491, OWASP A01)")
class TeacherControllerAuthzTest {

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
        TeacherService teacherService() {
            return Mockito.mock(TeacherService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TeacherService teacherService;

    private static final Long TEACHER_ID = 6L;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(teacherService);
    }

    @Test
    @DisplayName("TEACHER → 200 on GET /teachers/{id} (staff-directory read tier, service invoked)")
    @WithMockUser(roles = "TEACHER")
    void getById_teacher_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/{id}", TEACHER_ID))
                .andExpect(status().isOk());
        verify(teacherService).getTeacherById(TEACHER_ID);
    }

    @Test
    @DisplayName("OWASP A01: TEACHER → denied DELETE /teachers/{id} (HR mutation is admin-only)")
    @WithMockUser(roles = "TEACHER")
    void delete_teacher_denied() throws Exception {
        mockMvc.perform(delete("/api/v1/teachers/{id}", TEACHER_ID))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "TEACHER delete"));
        verifyNoInteractions(teacherService);
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied DELETE /teachers/{id} (service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void delete_student_denied() throws Exception {
        mockMvc.perform(delete("/api/v1/teachers/{id}", TEACHER_ID))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT delete"));
        verifyNoInteractions(teacherService);
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
