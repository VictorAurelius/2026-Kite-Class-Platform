package com.kiteclass.core.module.student.bulkimport.service;

import com.kiteclass.core.module.student.bulkimport.dto.BulkImportRow;
import com.kiteclass.core.module.student.bulkimport.dto.RowError;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates an error-report xlsx: each failed input row + the error messages
 * describing why it was rejected.
 *
 * <p>Columns: original data columns + {@code error_message}. One row per error
 * (so a row with two errors appears twice, once per error message).
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Slf4j
@Component
public class ErrorReportGenerator {

    private static final String[] HEADERS = {
            "row_number", "name", "email", "phone", "date_of_birth",
            "gender", "address", "note", "field", "error_message"
    };

    /**
     * Generates an xlsx report containing one row per {@link RowError}.
     *
     * @param rows         all parsed rows from the original file (used to
     *                     recover the original values for each failed row)
     * @param errors       the list of row errors
     * @return xlsx bytes ready to stream back to the client
     */
    public byte[] generate(List<BulkImportRow> rows, List<RowError> errors) {
        // Index rows by rowNumber for O(1) lookup.
        Map<Integer, BulkImportRow> byRow = new HashMap<>();
        for (BulkImportRow r : rows) {
            byRow.put(r.rowNumber(), r);
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("errors");

            // Header styling — bold + yellow fill for visibility.
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            List<RowError> safeErrors = errors == null ? new ArrayList<>() : errors;
            for (RowError err : safeErrors) {
                BulkImportRow src = byRow.get(err.rowNumber());
                Row outRow = sheet.createRow(rowIndex++);
                outRow.createCell(0).setCellValue(err.rowNumber());
                outRow.createCell(1).setCellValue(src != null ? nullToEmpty(src.name()) : "");
                outRow.createCell(2).setCellValue(src != null ? nullToEmpty(src.email()) : "");
                outRow.createCell(3).setCellValue(src != null ? nullToEmpty(src.phone()) : "");
                outRow.createCell(4).setCellValue(src != null ? nullToEmpty(src.dateOfBirth()) : "");
                outRow.createCell(5).setCellValue(src != null ? nullToEmpty(src.gender()) : "");
                outRow.createCell(6).setCellValue(src != null ? nullToEmpty(src.address()) : "");
                outRow.createCell(7).setCellValue(src != null ? nullToEmpty(src.note()) : "");
                outRow.createCell(8).setCellValue(nullToEmpty(err.field()));
                outRow.createCell(9).setCellValue(nullToEmpty(err.message()));
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate error report xlsx", e);
            throw new BulkImportParseException("Không tạo được file báo cáo lỗi: " + e.getMessage(), e);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
