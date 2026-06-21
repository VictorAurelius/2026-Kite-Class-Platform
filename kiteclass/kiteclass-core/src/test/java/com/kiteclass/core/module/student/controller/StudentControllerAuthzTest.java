package com.kiteclass.core.module.student.controller;

import com.kiteclass.core.module.student.service.StudentService;
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

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization tests for {@link StudentController} (GAP-1491, OWASP A01).
 *
 * <p>Only {@code setStudentCredential} was guarded; create/read/update/delete were open.
 * The fix guards all student-management endpoints at the staff+teacher tier
 * ({@code hasAnyRole('OWNER','ADMIN','PRINCIPAL','TEACHER','STAFF','PLATFORM_ADMIN')}),
 * excluding STUDENT/PARENT from student PII + lifecycle.
 */
@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc
@Import({StudentControllerAuthzTest.TestSecurityConfig.class, StudentControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("StudentController @PreAuthorize role gate (GAP-1491, OWASP A01)")
class StudentControllerAuthzTest {

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
        StudentService studentService() {
            return Mockito.mock(StudentService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentService studentService;

    private static final String TENANT_HEADER = UUID.randomUUID().toString();
    private static final Long STUDENT_ID = 4L;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(studentService);
    }

    @Test
    @DisplayName("OWNER → 200 on GET /students/{id} (staff read tier, service invoked)")
    @WithMockUser(roles = "OWNER")
    void getById_owner_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", STUDENT_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk());
        verify(studentService).getStudentById(STUDENT_ID);
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied DELETE /students/{id} (service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void delete_student_denied() throws Exception {
        mockMvc.perform(delete("/api/v1/students/{id}", STUDENT_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT delete"));
        verifyNoInteractions(studentService);
    }

    @Test
    @DisplayName("OWASP A01: PARENT → denied GET /students/{id} (staff read excludes PARENT)")
    @WithMockUser(roles = "PARENT")
    void getById_parent_denied() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", STUDENT_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "PARENT read"));
        verifyNoInteractions(studentService);
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
