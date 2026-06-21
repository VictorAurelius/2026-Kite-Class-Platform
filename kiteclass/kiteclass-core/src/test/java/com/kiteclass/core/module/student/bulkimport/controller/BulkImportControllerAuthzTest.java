package com.kiteclass.core.module.student.bulkimport.controller;

import com.kiteclass.core.module.student.bulkimport.dto.BulkImportResult;
import com.kiteclass.core.module.student.bulkimport.service.StudentBulkImportService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization tests for {@link BulkImportController} (GAP-1527, OWASP A01).
 *
 * <p>Before GAP-1527 the student bulk-import endpoints had NO {@code @PreAuthorize};
 * any authenticated caller (incl. STUDENT / PARENT) could preview, commit, or pull the
 * error report — bulk-creating students is an admin-tier operation. The fix guards
 * preview / commit / error-report with {@code hasAnyRole('OWNER','ADMIN','PRINCIPAL','STAFF')}.
 * The static {@code /template} download stays open (tenant-agnostic, no data).
 */
@WebMvcTest(BulkImportController.class)
@AutoConfigureMockMvc
@Import({BulkImportControllerAuthzTest.TestSecurityConfig.class, BulkImportControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("BulkImportController @PreAuthorize role gate (GAP-1527, OWASP A01)")
class BulkImportControllerAuthzTest {

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
        StudentBulkImportService studentBulkImportService() {
            return Mockito.mock(StudentBulkImportService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentBulkImportService service;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private MockMultipartFile xlsx() {
        return new MockMultipartFile("file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service);
    }

    @Test
    @DisplayName("STAFF → commit allowed (service invoked)")
    @WithMockUser(roles = "STAFF")
    void commit_staff_allowed() throws Exception {
        when(service.commit(any(), eq(TENANT_ID), isNull()))
                .thenReturn(new BulkImportResult(1L, 0, 0, 0, 0, java.util.List.of()));
        mockMvc.perform(multipart("/api/v1/students/bulk-import/commit")
                        .file(xlsx())
                        .header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isCreated());
        verify(service).commit(any(), eq(TENANT_ID), isNull());
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied commit (service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void commit_student_denied() throws Exception {
        mockMvc.perform(multipart("/api/v1/students/bulk-import/commit")
                        .file(xlsx())
                        .header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT commit"));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("OWASP A01: PARENT → denied preview (service NOT invoked)")
    @WithMockUser(roles = "PARENT")
    void preview_parent_denied() throws Exception {
        mockMvc.perform(multipart("/api/v1/students/bulk-import/preview")
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
