package com.kiteclass.core.module.student.bulkimport.service;

import com.kiteclass.core.module.student.bulkimport.dto.BulkImportRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses an uploaded xlsx file into a list of {@link BulkImportRow}.
 *
 * <p>Expected header row (row 1, zero-indexed 0):
 * <pre>
 *   name | email | phone | date_of_birth | gender | address | note
 * </pre>
 *
 * <p>Column order is flexible — the parser resolves columns by header name
 * (case-insensitive, trim). Unknown headers are ignored; missing required
 * headers ({@code name}, {@code email}) cause {@link BulkImportParseException}.
 *
 * <p>Completely empty rows are skipped silently.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Slf4j
@Component
public class XlsxParser {

    /** Canonical header names (lower-case). */
    public static final String COL_NAME = "name";
    public static final String COL_EMAIL = "email";
    public static final String COL_PHONE = "phone";
    public static final String COL_DOB = "date_of_birth";
    public static final String COL_GENDER = "gender";
    public static final String COL_ADDRESS = "address";
    public static final String COL_NOTE = "note";

    private static final List<String> REQUIRED_HEADERS = Arrays.asList(COL_NAME, COL_EMAIL);

    /**
     * Parses the given xlsx input stream.
     *
     * @param inputStream the xlsx bytes
     * @return the parsed rows (empty if the sheet has only headers)
     * @throws BulkImportParseException on missing required headers, corrupt
     *                                  file, or any I/O error
     */
    public List<BulkImportRow> parse(InputStream inputStream) {
        List<BulkImportRow> rows = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BulkImportParseException("File xlsx rỗng (không có sheet)");
            }
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BulkImportParseException("Thiếu dòng tiêu đề (header row)");
            }

            Map<String, Integer> headerIndex = readHeaders(headerRow);
            assertRequiredHeaders(headerIndex);

            DataFormatter formatter = new DataFormatter(Locale.US);
            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowCompletelyEmpty(row, formatter)) {
                    continue;
                }
                rows.add(readRow(row, headerIndex, formatter));
            }
            return rows;
        } catch (IOException e) {
            throw new BulkImportParseException("Không đọc được file xlsx: " + e.getMessage(), e);
        }
    }

    private Map<String, Integer> readHeaders(Row headerRow) {
        Map<String, Integer> idx = new HashMap<>();
        DataFormatter formatter = new DataFormatter(Locale.US);
        short lastCell = headerRow.getLastCellNum();
        for (int c = 0; c < lastCell; c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) {
                continue;
            }
            String value = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                idx.put(value, c);
            }
        }
        return idx;
    }

    private void assertRequiredHeaders(Map<String, Integer> headers) {
        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_HEADERS) {
            if (!headers.containsKey(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new BulkImportParseException(
                    "Thiếu cột bắt buộc trong header: " + String.join(", ", missing));
        }
    }

    private boolean isRowCompletelyEmpty(Row row, DataFormatter formatter) {
        short last = row.getLastCellNum();
        for (int c = 0; c < last; c++) {
            Cell cell = row.getCell(c);
            if (cell == null) {
                continue;
            }
            String value = formatCell(cell, formatter);
            if (value != null && !value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private BulkImportRow readRow(Row row, Map<String, Integer> headers, DataFormatter formatter) {
        int rowNumber = row.getRowNum() + 1; // 1-indexed for end users
        return new BulkImportRow(
                rowNumber,
                readCell(row, headers, COL_NAME, formatter),
                readCell(row, headers, COL_EMAIL, formatter),
                readCell(row, headers, COL_PHONE, formatter),
                readCell(row, headers, COL_DOB, formatter),
                readCell(row, headers, COL_GENDER, formatter),
                readCell(row, headers, COL_ADDRESS, formatter),
                readCell(row, headers, COL_NOTE, formatter)
        );
    }

    private String readCell(Row row, Map<String, Integer> headers, String columnKey, DataFormatter formatter) {
        Integer idx = headers.get(columnKey);
        if (idx == null) {
            return null;
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return null;
        }
        String value = formatCell(cell, formatter);
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * Formats a cell consistently:
     * <ul>
     *   <li>Numeric dates → dd/MM/yyyy</li>
     *   <li>Plain numerics → integer form when possible ({@code 1234567890} not
     *       {@code 1.23456789E9}) so phone numbers survive round-trip</li>
     *   <li>Everything else → {@link DataFormatter#formatCellValue(Cell)}, trimmed</li>
     * </ul>
     */
    private String formatCell(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("dd/MM/yyyy").format(cell.getDateCellValue());
                }
                double d = cell.getNumericCellValue();
                // Integer-valued numerics → render without decimal / scientific
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return Long.toString((long) d);
                }
                return Double.toString(d);
            }
            case STRING -> {
                return cell.getStringCellValue().trim();
            }
            case BOOLEAN -> {
                return Boolean.toString(cell.getBooleanCellValue());
            }
            case FORMULA, BLANK, ERROR, _NONE -> {
                return formatter.formatCellValue(cell).trim();
            }
            default -> {
                return formatter.formatCellValue(cell).trim();
            }
        }
    }
}
