package com.kiteclass.core.module.enrollment.bulkimport.controller;

import com.kiteclass.core.module.enrollment.bulkimport.service.EnrollmentBulkImportService;
import com.kiteclass.core.module.enrollment.bulkimport.service.EnrollmentTemplateGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

/**
 * Web-layer test for {@link EnrollmentBulkImportController} GET /template.
 *
 * <p>Standalone MockMvc (no Spring context / security) — the service is mocked
 * and the real {@link EnrollmentTemplateGenerator} produces valid xlsx bytes.
 */
class EnrollmentBulkImportControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EnrollmentBulkImportService service = Mockito.mock(EnrollmentBulkImportService.class);
        EnrollmentTemplateGenerator generator = new EnrollmentTemplateGenerator();
        EnrollmentBulkImportController controller =
                new EnrollmentBulkImportController(service, generator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /template returns 200 with an xlsx attachment")
    void downloadTemplateReturnsXlsx() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/bulk-import/template"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("mau-import-ghi-danh.xlsx")));
    }
}
