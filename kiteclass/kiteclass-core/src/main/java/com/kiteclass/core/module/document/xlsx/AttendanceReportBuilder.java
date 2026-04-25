package com.kiteclass.core.module.document.xlsx;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import com.kiteclass.core.module.document.branding.HexColorUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Builds the weekly attendance xlsx report — one row per student, one column per weekday, trailing
 * summary columns with formulas.
 *
 * <p>Formula-first (per {@code BR-DOC-XLSX-001}): P/A/L/E cells are user inputs (blue), counting
 * columns contain formulas (black), percentage column contains cross-reference formulas (green).
 */
final class AttendanceReportBuilder {

    private static final String[] DAYS = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};
    private static final int FIRST_DAY_COL = 1;
    private static final int LAST_DAY_COL = FIRST_DAY_COL + DAYS.length - 1;
    private static final int TOTAL_PRESENT_COL = LAST_DAY_COL + 1;
    private static final int TOTAL_ABSENT_COL = TOTAL_PRESENT_COL + 1;
    private static final int PERCENT_COL = TOTAL_ABSENT_COL + 1;
    private static final int HEADER_ROW = 2;
    private static final int DATA_START_ROW = 3;

    DocumentResponse build(DocumentRequest request) {
        Map<String, Object> data = request.data();
        String weekStart = asString(data.get("weekStart"), "");
        String className = asString(data.get("className"), "");
        List<Map<String, Object>> students = asStudentList(data.get("students"));
        Map<String, Map<String, String>> attendance = asAttendanceMap(data.get("attendance"));

        byte[] primaryRgb = HexColorUtil.toRgbBytes(asString(data.get("branding.primaryColor"), null));

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Điểm danh");

            Styles styles = new Styles(wb, primaryRgb);

            writeTitle(sheet, styles, className, weekStart);
            writeHeaderRow(sheet, styles);
            writeStudentRows(sheet, styles, students, attendance);
            writeSummaryRow(sheet, styles, students.size());
            applyColumnWidths(sheet);
            sheet.createFreezePane(1, DATA_START_ROW);

            wb.write(out);
            String filename = buildFilename(weekStart);
            return DocumentResponse.of(out.toByteArray(), DocumentFormat.XLSX, filename);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build attendance xlsx", ex);
        }
    }

    private static void writeTitle(XSSFSheet sheet, Styles styles, String className, String weekStart) {
        XSSFRow row = sheet.createRow(0);
        XSSFCell cell = row.createCell(0);
        String title = "Báo cáo điểm danh — Lớp " + className + " — Tuần " + weekStart;
        cell.setCellValue(title);
        cell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, PERCENT_COL));
    }

    private static void writeHeaderRow(XSSFSheet sheet, Styles styles) {
        XSSFRow row = sheet.createRow(HEADER_ROW);
        write(row, 0, "Học sinh", styles.header);
        for (int i = 0; i < DAYS.length; i++) {
            write(row, FIRST_DAY_COL + i, DAYS[i], styles.header);
        }
        write(row, TOTAL_PRESENT_COL, "Có mặt", styles.header);
        write(row, TOTAL_ABSENT_COL, "Vắng", styles.header);
        write(row, PERCENT_COL, "Tỷ lệ", styles.header);
    }

    private static void writeStudentRows(
            XSSFSheet sheet,
            Styles styles,
            List<Map<String, Object>> students,
            Map<String, Map<String, String>> attendance) {
        for (int i = 0; i < students.size(); i++) {
            Map<String, Object> student = students.get(i);
            String id = asString(student.get("id"), "");
            String name = asString(student.get("name"), "");
            Map<String, String> dayStatus = attendance.getOrDefault(id, Map.of());

            XSSFRow row = sheet.createRow(DATA_START_ROW + i);
            write(row, 0, name, styles.input);
            for (int d = 0; d < DAYS.length; d++) {
                String status = dayStatus.getOrDefault(DAYS[d], "");
                write(row, FIRST_DAY_COL + d, status, styles.input);
            }

            int rowOneBased = row.getRowNum() + 1;
            String rangeA1 = cellRef(FIRST_DAY_COL, rowOneBased) + ":" + cellRef(LAST_DAY_COL, rowOneBased);
            setFormula(row, TOTAL_PRESENT_COL, "COUNTIF(" + rangeA1 + ",\"P\")", styles.formula);
            setFormula(row, TOTAL_ABSENT_COL, "COUNTIF(" + rangeA1 + ",\"A\")", styles.formula);
            setFormula(
                    row,
                    PERCENT_COL,
                    "IFERROR("
                            + cellRef(TOTAL_PRESENT_COL, rowOneBased) + "/("
                            + cellRef(TOTAL_PRESENT_COL, rowOneBased) + "+"
                            + cellRef(TOTAL_ABSENT_COL, rowOneBased) + "),0)",
                    styles.percent);
        }
    }

    private static void writeSummaryRow(XSSFSheet sheet, Styles styles, int studentCount) {
        int summaryRowIdx = DATA_START_ROW + studentCount;
        XSSFRow row = sheet.createRow(summaryRowIdx);
        write(row, 0, "Tổng cộng", styles.header);
        int dataStartOneBased = DATA_START_ROW + 1;
        int dataEndOneBased = summaryRowIdx;
        for (int c = FIRST_DAY_COL; c <= LAST_DAY_COL; c++) {
            String range = cellRef(c, dataStartOneBased) + ":" + cellRef(c, dataEndOneBased);
            setFormula(row, c, "COUNTIF(" + range + ",\"P\")", styles.formula);
        }
        String presentRange =
                cellRef(TOTAL_PRESENT_COL, dataStartOneBased) + ":" + cellRef(TOTAL_PRESENT_COL, dataEndOneBased);
        setFormula(row, TOTAL_PRESENT_COL, "SUM(" + presentRange + ")", styles.formula);
        String absentRange =
                cellRef(TOTAL_ABSENT_COL, dataStartOneBased) + ":" + cellRef(TOTAL_ABSENT_COL, dataEndOneBased);
        setFormula(row, TOTAL_ABSENT_COL, "SUM(" + absentRange + ")", styles.formula);
        setFormula(
                row,
                PERCENT_COL,
                "IFERROR("
                        + cellRef(TOTAL_PRESENT_COL, summaryRowIdx + 1) + "/("
                        + cellRef(TOTAL_PRESENT_COL, summaryRowIdx + 1) + "+"
                        + cellRef(TOTAL_ABSENT_COL, summaryRowIdx + 1) + "),0)",
                styles.percent);
    }

    private static void applyColumnWidths(XSSFSheet sheet) {
        sheet.setColumnWidth(0, 8000);
        for (int c = FIRST_DAY_COL; c <= LAST_DAY_COL; c++) {
            sheet.setColumnWidth(c, 2500);
        }
        sheet.setColumnWidth(TOTAL_PRESENT_COL, 3000);
        sheet.setColumnWidth(TOTAL_ABSENT_COL, 3000);
        sheet.setColumnWidth(PERCENT_COL, 3000);
    }

    private static String cellRef(int colZeroBased, int rowOneBased) {
        return columnLetter(colZeroBased) + rowOneBased;
    }

    private static String columnLetter(int colZeroBased) {
        StringBuilder sb = new StringBuilder();
        int n = colZeroBased;
        do {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }

    private static void write(XSSFRow row, int col, String value, CellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void setFormula(XSSFRow row, int col, String formula, CellStyle style) {
        XSSFCell cell = row.createCell(col, CellType.FORMULA);
        cell.setCellFormula(formula);
        cell.setCellStyle(style);
    }

    private static String buildFilename(String weekStart) {
        if (weekStart == null || weekStart.isBlank()) {
            return "attendance.xlsx";
        }
        return "attendance-" + weekStart.replace("/", "-") + ".xlsx";
    }

    private static String asString(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asStudentList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                out.add((Map<String, Object>) map);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, String>> asAttendanceMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return (Map<String, Map<String, String>>) map;
    }

    /** Style bundle — constructed once per workbook, reused across all cells. */
    private static final class Styles {
        final CellStyle title;
        final CellStyle header;
        final CellStyle input;
        final CellStyle formula;
        final CellStyle percent;

        /**
         * @param wb          target workbook; XSSFColor requires the workbook's indexed color map.
         * @param primaryRgb  optional 3-byte RGB — when non-null the header row fills with the
         *                    tenant primary colour (Sub-PR 5.5 branding integration) and header
         *                    text switches to white for contrast. Null ⇒ legacy grey fallback.
         */
        Styles(XSSFWorkbook wb, byte[] primaryRgb) {
            Font bold = wb.createFont();
            bold.setBold(true);
            bold.setFontHeightInPoints((short) 12);

            title = wb.createCellStyle();
            title.setFont(bold);
            title.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            if (primaryRgb != null) {
                headerStyle.setFillForegroundColor(new XSSFColor(primaryRgb, null));
                headerFont.setColor(IndexedColors.WHITE.getIndex());
            } else {
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            }
            headerStyle.setFont(headerFont);
            header = headerStyle;

            input = wb.createCellStyle();
            Font inputFont = wb.createFont();
            inputFont.setColor(IndexedColors.BLUE.getIndex());
            input.setFont(inputFont);
            input.setAlignment(HorizontalAlignment.CENTER);

            formula = wb.createCellStyle();
            Font formulaFont = wb.createFont();
            formulaFont.setColor(IndexedColors.BLACK.getIndex());
            formula.setFont(formulaFont);
            formula.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle percentStyle = wb.createCellStyle();
            Font percentFont = wb.createFont();
            percentFont.setColor(IndexedColors.GREEN.getIndex());
            percentStyle.setFont(percentFont);
            percentStyle.setDataFormat(wb.createDataFormat().getFormat("0.00%"));
            percentStyle.setAlignment(HorizontalAlignment.CENTER);
            percent = percentStyle;
        }
    }
}
