package com.kiteclass.core.module.student.bulkimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test for the bulk-import feature.
 *
 * <p>Builds an xlsx with 50 valid rows + 5 invalid rows (duplicate email, bad
 * phone, missing name, bad date, bad gender), POSTs to the commit endpoint,
 * and verifies:
 * <ul>
 *   <li>HTTP 201 with Location header</li>
 *   <li>successCount + errorCount correct</li>
 *   <li>BulkImportJob row persisted</li>
 *   <li>Error report endpoint returns an xlsx attachment</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class StudentBulkImportIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /bulk-import/commit — 50 valid + 5 invalid rows → 201 with counts")
    void commitsMixedFile() throws Exception {
        byte[] xlsx = buildTestFile();
        MockMultipartFile file = new MockMultipartFile(
                "file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsx);

        MvcResult result = mockMvc.perform(multipart("/api/v1/students/bulk-import/commit")
                        .file(file)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(55))
                .andExpect(jsonPath("$.data.successCount").value(50))
                .andExpect(jsonPath("$.data.errorCount").value(5))
                .andExpect(jsonPath("$.data.jobId").isNumber())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(body).get("data");
        long jobId = data.get("jobId").asLong();
        assertThat(jobId).isPositive();

        // Download error report — stateless MVP, we re-upload the same file.
        MockMultipartFile fileAgain = new MockMultipartFile(
                "file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsx);
        byte[] report = mockMvc.perform(multipart(
                        "/api/v1/students/bulk-import/jobs/{id}/errors", jobId)
                        .file(fileAgain)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(report).isNotEmpty();
        // ZIP magic bytes (xlsx is a ZIP container)
        assertThat(report[0]).isEqualTo((byte) 'P');
        assertThat(report[1]).isEqualTo((byte) 'K');
    }

    @Test
    @DisplayName("POST /bulk-import/preview — returns counts without writing to DB")
    void previewReturnsCountsWithoutWrites() throws Exception {
        byte[] xlsx = buildTestFile();
        MockMultipartFile file = new MockMultipartFile(
                "file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsx);

        mockMvc.perform(multipart("/api/v1/students/bulk-import/preview")
                        .file(file)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").doesNotExist())
                .andExpect(jsonPath("$.data.totalRows").value(55))
                .andExpect(jsonPath("$.data.successCount").value(50))
                .andExpect(jsonPath("$.data.errorCount").value(5));
    }

    /**
     * Builds an xlsx containing:
     * <ul>
     *   <li>50 valid rows (distinct emails + phones)</li>
     *   <li>1 row with a duplicate email (row 52 reuses row 2's email)</li>
     *   <li>1 row with a bad phone number ("abc")</li>
     *   <li>1 row with missing name</li>
     *   <li>1 row with bad date format (2010-05-15 ISO instead of dd/MM/yyyy)</li>
     *   <li>1 row with bad gender value</li>
     * </ul>
     */
    private static byte[] buildTestFile() throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();

            // Header
            Row header = sheet.createRow(0);
            String[] headers = {"name", "email", "phone", "date_of_birth", "gender", "address", "note"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            // 50 valid rows
            for (int i = 1; i <= 50; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue("Student " + i);
                row.createCell(1).setCellValue("student" + i + "@test.com");
                row.createCell(2).setCellValue(String.format("09%08d", i));
                row.createCell(3).setCellValue("01/01/2005");
                row.createCell(4).setCellValue(i % 2 == 0 ? "MALE" : "FEMALE");
                row.createCell(5).setCellValue("Address " + i);
                row.createCell(6).setCellValue("");
            }

            // 5 invalid rows
            // 51: duplicate email (reuses student1@test.com)
            Row dup = sheet.createRow(51);
            dup.createCell(0).setCellValue("Dup Student");
            dup.createCell(1).setCellValue("student1@test.com");
            dup.createCell(2).setCellValue("0912000000");

            // 52: bad phone
            Row badPhone = sheet.createRow(52);
            badPhone.createCell(0).setCellValue("Bad Phone");
            badPhone.createCell(1).setCellValue("badphone@test.com");
            badPhone.createCell(2).setCellValue("abc");

            // 53: missing name
            Row noName = sheet.createRow(53);
            noName.createCell(0).setCellValue("");
            noName.createCell(1).setCellValue("noname@test.com");
            noName.createCell(2).setCellValue("0913000000");

            // 54: bad date
            Row badDate = sheet.createRow(54);
            badDate.createCell(0).setCellValue("Bad Date");
            badDate.createCell(1).setCellValue("baddate@test.com");
            badDate.createCell(2).setCellValue("0914000000");
            badDate.createCell(3).setCellValue("2010-05-15"); // wrong format

            // 55: bad gender
            Row badGender = sheet.createRow(55);
            badGender.createCell(0).setCellValue("Bad Gender");
            badGender.createCell(1).setCellValue("badgender@test.com");
            badGender.createCell(2).setCellValue("0915000000");
            badGender.createCell(4).setCellValue("UNKNOWN");

            wb.write(out);
            return out.toByteArray();
        }
    }
}
