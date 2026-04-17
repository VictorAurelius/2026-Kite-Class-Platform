package com.kiteclass.core.module.student.bulkimport.service;

import com.kiteclass.core.module.student.bulkimport.dto.BulkImportRow;
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
 * Unit tests for {@link XlsxParser}.
 */
class XlsxParserTest {

    private final XlsxParser parser = new XlsxParser();

    @Test
    @DisplayName("Parses a valid xlsx with all 7 columns")
    void parsesValidFile() throws IOException {
        byte[] xlsx = buildXlsx(new String[][]{
                {"name", "email", "phone", "date_of_birth", "gender", "address", "note"},
                {"Nguyen Van A", "a@test.com", "0901234567", "15/05/2010", "MALE", "HCM", "tốt"},
                {"Tran Thi B", "b@test.com", "0902345678", "20/06/2011", "FEMALE", "HN", ""}
        });

        List<BulkImportRow> rows = parser.parse(new ByteArrayInputStream(xlsx));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).rowNumber()).isEqualTo(2);
        assertThat(rows.get(0).name()).isEqualTo("Nguyen Van A");
        assertThat(rows.get(0).email()).isEqualTo("a@test.com");
        assertThat(rows.get(0).phone()).isEqualTo("0901234567");
        assertThat(rows.get(0).dateOfBirth()).isEqualTo("15/05/2010");
        assertThat(rows.get(0).gender()).isEqualTo("MALE");
        assertThat(rows.get(1).name()).isEqualTo("Tran Thi B");
        assertThat(rows.get(1).note()).isNull();
    }

    @Test
    @DisplayName("Throws when required header 'name' is missing")
    void throwsOnMissingRequiredHeaders() throws IOException {
        byte[] xlsx = buildXlsx(new String[][]{
                {"email", "phone"},
                {"a@test.com", "0901234567"}
        });

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(xlsx)))
                .isInstanceOf(BulkImportParseException.class)
                .extracting("args")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.ARRAY)
                .anyMatch(arg -> arg != null && arg.toString().contains("name"));
    }

    @Test
    @DisplayName("Ignores extra unknown columns")
    void ignoresExtraColumns() throws IOException {
        byte[] xlsx = buildXlsx(new String[][]{
                {"name", "email", "unused_col", "phone"},
                {"Alice", "alice@test.com", "ignored", "0911111111"}
        });

        List<BulkImportRow> rows = parser.parse(new ByteArrayInputStream(xlsx));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).name()).isEqualTo("Alice");
        assertThat(rows.get(0).email()).isEqualTo("alice@test.com");
        assertThat(rows.get(0).phone()).isEqualTo("0911111111");
    }

    @Test
    @DisplayName("Skips completely empty rows between data rows")
    void skipsEmptyRows() throws IOException {
        byte[] xlsx = buildXlsx(new String[][]{
                {"name", "email"},
                {"Alice", "alice@test.com"},
                {"", ""}, // empty row
                {"Bob", "bob@test.com"}
        });

        List<BulkImportRow> rows = parser.parse(new ByteArrayInputStream(xlsx));

        assertThat(rows).extracting(BulkImportRow::name).containsExactly("Alice", "Bob");
    }

    @Test
    @DisplayName("Preserves phone numbers as integer strings (no scientific notation)")
    void preservesPhoneNumbersFromNumericCells() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("name");
            header.createCell(1).setCellValue("email");
            header.createCell(2).setCellValue("phone");

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("Alice");
            data.createCell(1).setCellValue("a@test.com");
            // numeric cell — POI defaults to scientific for large doubles
            data.createCell(2).setCellValue(901234567d);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            List<BulkImportRow> rows = parser.parse(new ByteArrayInputStream(out.toByteArray()));
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).phone()).isEqualTo("901234567");
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
