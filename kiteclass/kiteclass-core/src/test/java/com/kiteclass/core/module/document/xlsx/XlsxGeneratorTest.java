package com.kiteclass.core.module.document.xlsx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentGenerationTestBase;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class XlsxGeneratorTest extends DocumentGenerationTestBase {

    private final XlsxGenerator generator = new XlsxGenerator();

    private static Map<String, Object> sampleAttendanceData() {
        return Map.of(
                "weekStart", "2026-04-20",
                "className", "10A1",
                "students", List.of(
                        Map.of("id", "S001", "name", "Nguyễn Văn Đức"),
                        Map.of("id", "S002", "name", "Trần Thị Ánh"),
                        Map.of("id", "S003", "name", "Lê Minh Hằng")),
                "attendance", Map.of(
                        "S001", Map.of("Thứ 2", "P", "Thứ 3", "P", "Thứ 4", "A", "Thứ 5", "P", "Thứ 6", "P", "Thứ 7", "L"),
                        "S002", Map.of("Thứ 2", "P", "Thứ 3", "P", "Thứ 4", "P", "Thứ 5", "P", "Thứ 6", "P", "Thứ 7", "P"),
                        "S003", Map.of("Thứ 2", "A", "Thứ 3", "A", "Thứ 4", "P", "Thứ 5", "P", "Thứ 6", "P", "Thứ 7", "P")));
    }

    @Test
    void format_returns_xlsx() {
        assertThat(generator.format()).isEqualTo(DocumentFormat.XLSX);
    }

    @Test
    void generate_returns_non_empty_xlsx_bytes() {
        DocumentRequest req = sampleRequest(DocumentFormat.XLSX, "attendance", sampleAttendanceData());

        DocumentResponse resp = generator.generate(req);

        assertThat(resp.bytes()).isNotEmpty();
        assertThat(resp.mimeType()).isEqualTo(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(resp.filename()).endsWith(".xlsx");
        // xlsx is a ZIP — magic number PK
        assertThat(resp.bytes()[0]).isEqualTo((byte) 'P');
        assertThat(resp.bytes()[1]).isEqualTo((byte) 'K');
    }

    @Test
    void generated_xlsx_has_title_with_class_and_week() throws Exception {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.XLSX, "attendance", sampleAttendanceData()));

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(resp.bytes()))) {
            Sheet sheet = wb.getSheetAt(0);
            String title = sheet.getRow(0).getCell(0).getStringCellValue();
            assertThat(title).contains("10A1").contains("2026-04-20");
        }
    }

    @Test
    void generated_xlsx_has_vietnamese_header_and_student_names() throws Exception {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.XLSX, "attendance", sampleAttendanceData()));

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(resp.bytes()))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(2);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Học sinh");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Thứ 2");
            assertThat(header.getCell(7).getStringCellValue()).isEqualTo("Có mặt");
            assertThat(header.getCell(9).getStringCellValue()).isEqualTo("Tỷ lệ");

            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Nguyễn Văn Đức");
            assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Trần Thị Ánh");
            assertThat(sheet.getRow(5).getCell(0).getStringCellValue()).isEqualTo("Lê Minh Hằng");
        }
    }

    @Test
    void totals_columns_are_formulas_not_values() throws Exception {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.XLSX, "attendance", sampleAttendanceData()));

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(resp.bytes()))) {
            Sheet sheet = wb.getSheetAt(0);
            Row first = sheet.getRow(3);
            assertThat(first.getCell(7).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(first.getCell(7).getCellFormula()).startsWith("COUNTIF(");
            assertThat(first.getCell(8).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(first.getCell(9).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(first.getCell(9).getCellFormula()).contains("IFERROR");
        }
    }

    @Test
    void summary_row_sums_daily_columns() throws Exception {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.XLSX, "attendance", sampleAttendanceData()));

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(resp.bytes()))) {
            Sheet sheet = wb.getSheetAt(0);
            int summaryRowIdx = 3 + 3; // DATA_START_ROW + 3 students
            Row summary = sheet.getRow(summaryRowIdx);
            assertThat(summary.getCell(0).getStringCellValue()).isEqualTo("Tổng cộng");
            assertThat(summary.getCell(1).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(summary.getCell(1).getCellFormula()).startsWith("COUNTIF(");
            assertThat(summary.getCell(7).getCellFormula()).startsWith("SUM(");
        }
    }

    @Test
    void filename_contains_week_start() {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.XLSX, "attendance", sampleAttendanceData()));

        assertThat(resp.filename()).contains("2026-04-20");
    }

    @Test
    void unknown_template_id_throws() {
        DocumentRequest req = sampleRequest(DocumentFormat.XLSX, "does-not-exist", Map.of());

        assertThatThrownBy(() -> generator.generate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does-not-exist");
    }

    @Test
    void generate_rejects_null_request() {
        assertThatThrownBy(() -> generator.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request");
    }

    @Test
    void generate_rejects_request_with_mismatched_format() {
        DocumentRequest req = DocumentRequest.builder()
                .format(DocumentFormat.PDF)
                .templateId("attendance")
                .tenantId(SAMPLE_TENANT_ID)
                .data(sampleAttendanceData())
                .build();

        assertThatThrownBy(() -> generator.generate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("format");
    }

    @Test
    void branding_primary_color_paints_header_row_fill() throws Exception {
        Map<String, Object> data = new HashMap<>(sampleAttendanceData());
        data.put("branding.primaryColor", "#2563EB");

        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.XLSX, "attendance", data));

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(resp.bytes()))) {
            Sheet sheet = wb.getSheetAt(0);
            XSSFCell headerCell = (XSSFCell) sheet.getRow(2).getCell(0);
            XSSFColor fill = headerCell.getCellStyle().getFillForegroundColorColor();
            assertThat(fill).isNotNull();
            byte[] rgb = fill.getRGB();
            assertThat(rgb).isNotNull();
            assertThat(rgb[0] & 0xFF).isEqualTo(0x25);
            assertThat(rgb[1] & 0xFF).isEqualTo(0x63);
            assertThat(rgb[2] & 0xFF).isEqualTo(0xEB);
        }
    }

    @Test
    void attendance_render_under_soft_cap_for_regression_canary() {
        // GAP-216 — soft-cap regression canary, NOT the SLO. XLSX is faster than PDF
        // (no font load, no template parse); 2s ceiling sufficient. True p95 measurement
        // requires JMH suite (deferred to follow-up GAP-750).
        DocumentRequest req = sampleRequest(DocumentFormat.XLSX, "attendance", sampleAttendanceData());

        long startNs = System.nanoTime();
        DocumentResponse resp = generator.generate(req);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        assertThat(resp.bytes()).isNotEmpty();
        assertThat(elapsedMs)
                .as("XLSX render took %d ms — should stay under 2000 ms soft cap", elapsedMs)
                .isLessThan(2000);
    }

    @Test
    void empty_student_list_yields_workbook_with_only_header_and_summary() throws Exception {
        Map<String, Object> data = Map.of(
                "weekStart", "2026-04-20",
                "className", "Empty",
                "students", List.of(),
                "attendance", Map.of());

        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.XLSX, "attendance", data));

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(resp.bytes()))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(3)).isEqualTo(sheet.getRow(3)); // summary row right after header
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Tổng cộng");
        }
    }
}
