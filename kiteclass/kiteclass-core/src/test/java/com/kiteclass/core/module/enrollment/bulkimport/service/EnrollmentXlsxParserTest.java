package com.kiteclass.core.module.enrollment.bulkimport.service;

import com.kiteclass.core.module.enrollment.bulkimport.dto.EnrollmentBulkRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link EnrollmentXlsxParser}, including a round-trip through the
 * generated template.
 */
class EnrollmentXlsxParserTest {

    private final EnrollmentXlsxParser parser = new EnrollmentXlsxParser();
    private final EnrollmentTemplateGenerator generator = new EnrollmentTemplateGenerator();

    @Test
    @DisplayName("Round-trips the generated template: headers resolve + 2 example rows parse")
    void roundTripTemplate() {
        byte[] template = generator.generateTemplate();

        List<EnrollmentBulkRow> rows = parser.parse(new ByteArrayInputStream(template));

        assertThat(rows).hasSize(2);

        // Row 1 (sheet row 2) — email-resolve variant
        EnrollmentBulkRow r1 = rows.get(0);
        assertThat(r1.rowNumber()).isEqualTo(2);
        assertThat(r1.studentEmail()).isEqualTo("an.nguyen@example.com");
        assertThat(r1.studentPhone()).isNull();
        assertThat(r1.classCode()).isEqualTo("TOAN9A");
        assertThat(r1.tuitionAmount()).isEqualTo("1500000");
        assertThat(r1.discountPercent()).isEqualTo("0");
        assertThat(r1.note()).isNull();

        // Row 2 (sheet row 3) — phone-resolve variant
        EnrollmentBulkRow r2 = rows.get(1);
        assertThat(r2.rowNumber()).isEqualTo(3);
        assertThat(r2.studentEmail()).isNull();
        assertThat(r2.studentPhone()).isEqualTo("0912345678");
        assertThat(r2.classCode()).isEqualTo("LY10B");
        assertThat(r2.tuitionAmount()).isEqualTo("2000000");
        assertThat(r2.discountPercent()).isEqualTo("10");
        assertThat(r2.note()).isEqualTo("Học sinh chuyển lớp");
    }

    @Test
    @DisplayName("Parses a valid xlsx with all 6 columns")
    void parsesValidFile() throws IOException {
        byte[] xlsx = buildXlsx(new String[][]{
                {"student_email", "student_phone", "class_code", "tuition_amount", "discount_percent", "note"},
                {"hong.tran@skyedu.vn", "", "TOAN9A", "1500000", "0", "Ghi danh học kỳ 1"},
        });

        List<EnrollmentBulkRow> rows = parser.parse(new ByteArrayInputStream(xlsx));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).studentEmail()).isEqualTo("hong.tran@skyedu.vn");
        assertThat(rows.get(0).classCode()).isEqualTo("TOAN9A");
        assertThat(rows.get(0).tuitionAmount()).isEqualTo("1500000");
        assertThat(rows.get(0).note()).isEqualTo("Ghi danh học kỳ 1");
    }

    @Test
    @DisplayName("Resolves columns case-insensitively + ignores unknown columns")
    void caseInsensitiveHeadersAndIgnoresExtra() throws IOException {
        byte[] xlsx = buildXlsx(new String[][]{
                {"STUDENT_EMAIL", "Class_Code", "tuition_amount", "unused_col"},
                {"a@test.vn", "LY10B", "2000000", "ignored"},
        });

        List<EnrollmentBulkRow> rows = parser.parse(new ByteArrayInputStream(xlsx));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).studentEmail()).isEqualTo("a@test.vn");
        assertThat(rows.get(0).classCode()).isEqualTo("LY10B");
        assertThat(rows.get(0).tuitionAmount()).isEqualTo("2000000");
    }

    @Test
    @DisplayName("Throws when required header 'class_code' is missing")
    void throwsOnMissingClassCode() throws IOException {
        byte[] xlsx = buildXlsx(new String[][]{
                {"student_email", "tuition_amount"},
                {"a@test.vn", "1500000"},
        });

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(xlsx)))
                .isInstanceOf(EnrollmentBulkImportParseException.class)
                .extracting("args")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.ARRAY)
                .anyMatch(arg -> arg != null && arg.toString().contains("class_code"));
    }

    @Test
    @DisplayName("Throws when neither student_email nor student_phone header present")
    void throwsOnMissingStudentResolver() throws IOException {
        byte[] xlsx = buildXlsx(new String[][]{
                {"class_code", "tuition_amount"},
                {"TOAN9A", "1500000"},
        });

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(xlsx)))
                .isInstanceOf(EnrollmentBulkImportParseException.class)
                .extracting("args")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.ARRAY)
                .anyMatch(arg -> arg != null && arg.toString().contains("student_email"));
    }

    @Test
    @DisplayName("Skips completely empty rows between data rows")
    void skipsEmptyRows() throws IOException {
        byte[] xlsx = buildXlsx(new String[][]{
                {"student_email", "class_code", "tuition_amount"},
                {"a@test.vn", "TOAN9A", "1500000"},
                {"", "", ""},
                {"b@test.vn", "LY10B", "2000000"},
        });

        List<EnrollmentBulkRow> rows = parser.parse(new ByteArrayInputStream(xlsx));

        assertThat(rows).extracting(EnrollmentBulkRow::studentEmail)
                .containsExactly("a@test.vn", "b@test.vn");
    }

    @Test
    @DisplayName("Preserves phone numbers as integer strings (no scientific notation)")
    void preservesPhoneFromNumericCell() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("student_phone");
            header.createCell(1).setCellValue("class_code");
            header.createCell(2).setCellValue("tuition_amount");

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue(912345678d); // numeric → POI defaults scientific
            data.createCell(1).setCellValue("TOAN9A");
            data.createCell(2).setCellValue(1500000d);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            List<EnrollmentBulkRow> rows = parser.parse(new ByteArrayInputStream(out.toByteArray()));
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).studentPhone()).isEqualTo("912345678");
            assertThat(rows.get(0).tuitionAmount()).isEqualTo("1500000");
        }
    }

    // --------------------------------------------------------------- helpers

    private static byte[] buildXlsx(String[][] rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                String[] cols = rows[r];
                for (int c = 0; c < cols.length; c++) {
                    row.createCell(c).setCellValue(cols[c]);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
