package com.kiteclass.core.module.enrollment.bulkimport.service;

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

/**
 * Generates the downloadable bulk-enroll template xlsx.
 *
 * <p>Two sheets:
 * <ul>
 *   <li><b>GhiDanh</b> — canonical header row + 2 example rows showing the
 *       email-resolve and phone-resolve variants.</li>
 *   <li><b>HuongDan</b> — Vietnamese instructions explaining each column +
 *       required/optional rules.</li>
 * </ul>
 *
 * <p>Headers MUST match {@link EnrollmentXlsxParser} canonical names so a
 * round-trip (download template → fill → upload) parses cleanly.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Slf4j
@Component
public class EnrollmentTemplateGenerator {

    /** Data-entry sheet name. */
    public static final String DATA_SHEET = "GhiDanh";

    /** Instructions sheet name. */
    public static final String GUIDE_SHEET = "HuongDan";

    private static final String[] HEADERS = {
            EnrollmentXlsxParser.COL_STUDENT_EMAIL,
            EnrollmentXlsxParser.COL_STUDENT_PHONE,
            EnrollmentXlsxParser.COL_CLASS_CODE,
            EnrollmentXlsxParser.COL_TUITION_AMOUNT,
            EnrollmentXlsxParser.COL_DISCOUNT_PERCENT,
            EnrollmentXlsxParser.COL_NOTE,
    };

    /** Two example rows: row 1 resolves by email, row 2 resolves by phone. */
    private static final String[][] EXAMPLE_ROWS = {
            {"an.nguyen@example.com", "", "TOAN9A", "1500000", "0", ""},
            {"", "0912345678", "LY10B", "2000000", "10", "Học sinh chuyển lớp"},
    };

    private static final String[] GUIDE_LINES = {
            "HƯỚNG DẪN NHẬP GHI DANH HÀNG LOẠT",
            "",
            "1. Nhập dữ liệu vào sheet \"" + DATA_SHEET + "\", mỗi dòng là một lượt ghi danh.",
            "2. class_code (BẮT BUỘC): mã lớp cần ghi danh, ví dụ TOAN9A. Phải khớp đúng mã lớp đã tạo trong trung tâm.",
            "3. Cần email HOẶC phone để xác định học sinh: điền student_email (ưu tiên) hoặc student_phone. Có thể điền cả hai.",
            "4. tuition_amount (BẮT BUỘC): số tiền học phí, nhập dạng số (ví dụ 1500000), không thêm dấu phân cách.",
            "5. discount_percent (TÙY CHỌN): phần trăm giảm giá từ 0 đến 100, mặc định 0 nếu để trống.",
            "6. note (TÙY CHỌN): ghi chú thêm cho lượt ghi danh.",
            "7. Tối đa 1000 dòng mỗi lần tải lên. Hệ thống sẽ báo lỗi từng dòng nếu có vấn đề.",
            "8. Học sinh đã được ghi danh trong lớp sẽ bị bỏ qua và báo lỗi (không ghi danh trùng).",
    };

    /**
     * Builds the template workbook as xlsx bytes.
     *
     * @return xlsx bytes ready to stream back as an attachment
     */
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            buildDataSheet(workbook);
            buildGuideSheet(workbook);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate enrollment bulk-import template xlsx", e);
            throw new EnrollmentBulkImportParseException(
                    "Không tạo được file mẫu: " + e.getMessage(), e);
        }
    }

    private void buildDataSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet(DATA_SHEET);

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

        for (int r = 0; r < EXAMPLE_ROWS.length; r++) {
            Row row = sheet.createRow(r + 1);
            String[] cols = EXAMPLE_ROWS[r];
            for (int c = 0; c < cols.length; c++) {
                row.createCell(c).setCellValue(cols[c]);
            }
        }

        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void buildGuideSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet(GUIDE_SHEET);

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleStyle.setFont(titleFont);

        for (int i = 0; i < GUIDE_LINES.length; i++) {
            Row row = sheet.createRow(i);
            Cell cell = row.createCell(0);
            cell.setCellValue(GUIDE_LINES[i]);
            if (i == 0) {
                cell.setCellStyle(titleStyle);
            }
        }
        sheet.autoSizeColumn(0);
    }
}
