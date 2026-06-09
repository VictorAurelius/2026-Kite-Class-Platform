package com.kiteclass.core.module.student.bulkimport.controller;

import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.module.student.bulkimport.service.StudentBulkImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc tests for {@link BulkImportController} — GAP-1102 template download.
 *
 * <p>Focuses on the new {@code GET /template} endpoint: it must return 200 with
 * the xlsx content-type and an attachment {@code Content-Disposition} carrying the
 * Vietnamese filename {@code mau-import-hoc-vien.xlsx}.
 */
@WebMvcTest(BulkImportController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("BulkImportController Tests")
class BulkImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentBulkImportService service;

    @TestConfiguration
    static class Mocks {
        @Bean
        StudentBulkImportService studentBulkImportService() {
            return mock(StudentBulkImportService.class);
        }
    }

    @BeforeEach
    void resetMocks() {
        reset(service);
    }

    @Test
    @DisplayName("GET /template returns 200 + xlsx content-type + attachment disposition")
    void downloadTemplateReturnsXlsxAttachment() throws Exception {
        byte[] templateBytes = {1, 2, 3, 4, 5};
        when(service.generateTemplate()).thenReturn(templateBytes);

        mockMvc.perform(get("/api/v1/students/bulk-import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("mau-import-hoc-vien.xlsx")))
                .andExpect(content().bytes(templateBytes));
    }

    @Test
    @DisplayName("GET /template needs no X-Tenant-Id header (static template)")
    void downloadTemplateNeedsNoTenantHeader() throws Exception {
        when(service.generateTemplate()).thenReturn(new byte[]{9});

        // No X-Tenant-Id header supplied → still 200 (tenant-agnostic).
        mockMvc.perform(get("/api/v1/students/bulk-import/template"))
                .andExpect(status().isOk());
    }
}
