package com.kiteclass.core.module.enrollment.bulkimport.controller;

import com.kiteclass.core.module.enrollment.bulkimport.dto.EnrollmentBulkResult;
import com.kiteclass.core.module.enrollment.bulkimport.service.EnrollmentBulkImportService;
import com.kiteclass.core.module.enrollment.bulkimport.service.EnrollmentTemplateGenerator;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization tests for {@link EnrollmentBulkImportController} (GAP-1527, OWASP A01).
 *
 * <p>Before GAP-1527 the bulk-enroll endpoints had NO {@code @PreAuthorize}. The fix guards
 * preview / commit with {@code hasAnyRole('OWNER','ADMIN','PRINCIPAL','STAFF','TEACHER')}
 * (TEACHER kept — enrolling students into their own classes is a teacher concern).
 * STUDENT / PARENT are denied. The static {@code /template} download stays open.
 */
@WebMvcTest(EnrollmentBulkImportController.class)
@AutoConfigureMockMvc
@Import({EnrollmentBulkImportControllerAuthzTest.TestSecurityConfig.class,
        EnrollmentBulkImportControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("EnrollmentBulkImportController @PreAuthorize role gate (GAP-1527, OWASP A01)")
class EnrollmentBulkImportControllerAuthzTest {

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
        EnrollmentBulkImportService enrollmentBulkImportService() {
            return Mockito.mock(EnrollmentBulkImportService.class);
        }

        @Bean
        @Primary
        EnrollmentTemplateGenerator enrollmentTemplateGenerator() {
            return Mockito.mock(EnrollmentTemplateGenerator.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnrollmentBulkImportService service;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private MockMultipartFile xlsx() {
        return new MockMultipartFile("file", "enroll.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service);
    }

    @Test
    @DisplayName("TEACHER → commit allowed (service invoked)")
    @WithMockUser(roles = "TEACHER")
    void commit_teacher_allowed() throws Exception {
        when(service.commit(any(), eq(TENANT_ID)))
                .thenReturn(new EnrollmentBulkResult(0, 0, 0, List.of()));
        mockMvc.perform(multipart("/api/v1/enrollments/bulk-import/commit")
                        .file(xlsx())
                        .header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isCreated());
        verify(service).commit(any(), eq(TENANT_ID));
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied commit (service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void commit_student_denied() throws Exception {
        mockMvc.perform(multipart("/api/v1/enrollments/bulk-import/commit")
                        .file(xlsx())
                        .header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT commit"));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("OWASP A01: PARENT → denied preview (service NOT invoked)")
    @WithMockUser(roles = "PARENT")
    void preview_parent_denied() throws Exception {
        mockMvc.perform(multipart("/api/v1/enrollments/bulk-import/preview")
                        .file(xlsx())
                        .header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "PARENT preview"));
        verifyNoInteractions(service);
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
