package com.kiteclass.core.module.enrollment.bulkimport.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EnrollmentTemplateGenerator}.
 */
class EnrollmentTemplateGeneratorTest {

    private final EnrollmentTemplateGenerator generator = new EnrollmentTemplateGenerator();

    @Test
    @DisplayName("Generates a non-empty xlsx with data + guide sheets")
    void generatesTwoSheets() throws IOException {
        byte[] bytes = generator.generateTemplate();

        assertThat(bytes).isNotEmpty();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheetName(0)).isEqualTo(EnrollmentTemplateGenerator.DATA_SHEET);
            assertThat(wb.getSheetName(1)).isEqualTo(EnrollmentTemplateGenerator.GUIDE_SHEET);
        }
    }

    @Test
    @DisplayName("Data sheet header row matches the 6 canonical columns in order")
    void dataSheetHeaderMatchesCanonicalColumns() throws IOException {
        byte[] bytes = generator.generateTemplate();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet(EnrollmentTemplateGenerator.DATA_SHEET);
            Row header = sheet.getRow(0);

            assertThat(header.getCell(0).getStringCellValue()).isEqualTo(EnrollmentXlsxParser.COL_STUDENT_EMAIL);
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo(EnrollmentXlsxParser.COL_STUDENT_PHONE);
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo(EnrollmentXlsxParser.COL_CLASS_CODE);
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo(EnrollmentXlsxParser.COL_TUITION_AMOUNT);
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo(EnrollmentXlsxParser.COL_DISCOUNT_PERCENT);
            assertThat(header.getCell(5).getStringCellValue()).isEqualTo(EnrollmentXlsxParser.COL_NOTE);
        }
    }

    @Test
    @DisplayName("Data sheet contains 2 example rows (email-resolve + phone-resolve)")
    void dataSheetHasTwoExampleRows() throws IOException {
        byte[] bytes = generator.generateTemplate();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet(EnrollmentTemplateGenerator.DATA_SHEET);
            // header (0) + 2 example rows (1,2) → lastRowNum == 2
            assertThat(sheet.getLastRowNum()).isEqualTo(2);

            Row example1 = sheet.getRow(1);
            assertThat(example1.getCell(0).getStringCellValue()).isEqualTo("an.nguyen@example.com");
            assertThat(example1.getCell(2).getStringCellValue()).isEqualTo("TOAN9A");

            Row example2 = sheet.getRow(2);
            assertThat(example2.getCell(1).getStringCellValue()).isEqualTo("0912345678");
            assertThat(example2.getCell(2).getStringCellValue()).isEqualTo("LY10B");
        }
    }

    @Test
    @DisplayName("Guide sheet has Vietnamese instructions")
    void guideSheetHasInstructions() throws IOException {
        byte[] bytes = generator.generateTemplate();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet guide = wb.getSheet(EnrollmentTemplateGenerator.GUIDE_SHEET);
            assertThat(guide.getRow(0).getCell(0).getStringCellValue())
                    .contains("HƯỚNG DẪN");
        }
    }
}
