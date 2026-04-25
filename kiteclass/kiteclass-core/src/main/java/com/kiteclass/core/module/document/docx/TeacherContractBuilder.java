package com.kiteclass.core.module.document.docx;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import com.kiteclass.core.module.document.branding.HexColorUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;

/**
 * Assembles the teacher contract DOCX with Vietnamese typography conventions (A4, 2.54 cm
 * margins, Times New Roman 12pt body, bold title).
 *
 * <p>Contract terms are placeholders — actual legal wording is deferred to a legal-review wave.
 */
final class TeacherContractBuilder {

    private static final Locale VI_VN = Locale.forLanguageTag("vi-VN");
    private static final DecimalFormat VND_FORMAT;
    private static final String FONT = "Times New Roman";
    private static final int TITLE_FONT_PT = 14;
    private static final int BODY_FONT_PT = 12;
    private static final int A4_WIDTH_TWIPS = 11906;
    private static final int A4_HEIGHT_TWIPS = 16838;
    private static final int MARGIN_TWIPS = 1440; // 2.54 cm
    private static final int MIN_PARAGRAPH_COUNT = 8;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(VI_VN);
        symbols.setGroupingSeparator('.');
        VND_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    DocumentResponse build(DocumentRequest request) {
        Map<String, Object> data = request.data();
        String teacherName = required(data, "teacherName");
        String teacherIdNumber = required(data, "teacherIdNumber");
        String tenantName = required(data, "tenantName");
        String tenantAddress = required(data, "tenantAddress");
        String startDate = required(data, "startDate");
        String endDate = required(data, "endDate");
        BigDecimal salary = asBigDecimal(data.get("salaryVnd"));
        String subjects = asString(data.get("subjects"), "—");
        String titleColorHex = HexColorUtil.stripHash(asString(data.get("branding.primaryColor"), null));

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            applyA4Page(doc);

            addHeading(doc, "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", false, 12, null);
            addHeading(doc, "Độc lập - Tự do - Hạnh phúc", true, 12, null);
            addHeading(doc, "HỢP ĐỒNG GIẢNG DẠY", true, TITLE_FONT_PT, titleColorHex);

            addBody(doc, "Căn cứ nhu cầu và thỏa thuận giữa các bên, hợp đồng giảng dạy này được ký kết "
                    + "giữa:");

            addBody(doc, "BÊN A (Bên thuê): " + tenantName);
            addBody(doc, "Địa chỉ: " + tenantAddress);

            addBody(doc, "BÊN B (Giáo viên): " + teacherName);
            addBody(doc, "Số CMND/CCCD: " + teacherIdNumber);

            addBody(doc, "Điều 1 — Phạm vi công việc: Bên B đồng ý giảng dạy các môn "
                    + subjects + " theo lịch của Bên A.");
            addBody(doc, "Điều 2 — Thời hạn hợp đồng: từ ngày " + startDate + " đến ngày " + endDate + ".");
            addBody(doc, "Điều 3 — Mức lương: " + formatVnd(salary)
                    + " đồng/tháng, thanh toán vào ngày 5 hàng tháng.");
            addBody(doc, "Điều 4 — Nội dung chi tiết, quyền và nghĩa vụ của các bên "
                    + "[sẽ được pháp lý duyệt ở wave sau — placeholder theo wave-05 plan §3].");

            addSignatureBlock(doc, tenantName, teacherName);

            doc.write(out);
            return DocumentResponse.of(out.toByteArray(), DocumentFormat.DOCX,
                    buildFilename(teacherName, startDate));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build teacher contract docx", ex);
        }
    }

    private static void applyA4Page(XWPFDocument doc) {
        CTSectPr sectPr = doc.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = sectPr.addNewPgSz();
        pageSize.setW(java.math.BigInteger.valueOf(A4_WIDTH_TWIPS));
        pageSize.setH(java.math.BigInteger.valueOf(A4_HEIGHT_TWIPS));
        pageSize.setOrient(STPageOrientation.PORTRAIT);
        CTPageMar margin = sectPr.addNewPgMar();
        margin.setTop(java.math.BigInteger.valueOf(MARGIN_TWIPS));
        margin.setRight(java.math.BigInteger.valueOf(MARGIN_TWIPS));
        margin.setBottom(java.math.BigInteger.valueOf(MARGIN_TWIPS));
        margin.setLeft(java.math.BigInteger.valueOf(MARGIN_TWIPS));
    }

    private static void addHeading(XWPFDocument doc, String text, boolean bold, int sizePt,
                                   String colorHexNoHash) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setFontFamily(FONT);
        run.setFontSize(sizePt);
        run.setBold(bold);
        if (colorHexNoHash != null) {
            run.setColor(colorHexNoHash);
        }
        run.setText(text);
    }

    private static void addBody(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun run = p.createRun();
        run.setFontFamily(FONT);
        run.setFontSize(BODY_FONT_PT);
        run.setText(text);
    }

    private static void addSignatureBlock(XWPFDocument doc, String tenantName, String teacherName) {
        // Two-column signature layout rendered via tabbed paragraph — POI XWPF tables would be
        // cleaner but add complexity; tab alignment is adequate for a placeholder contract.
        XWPFParagraph spacer = doc.createParagraph();
        spacer.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun spacerRun = spacer.createRun();
        spacerRun.setText("");
        spacerRun.addBreak();
        spacerRun.addBreak();

        XWPFParagraph labels = doc.createParagraph();
        labels.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun lRun = labels.createRun();
        lRun.setFontFamily(FONT);
        lRun.setFontSize(BODY_FONT_PT);
        lRun.setBold(true);
        lRun.setText("ĐẠI DIỆN BÊN A\t\t\t\tĐẠI DIỆN BÊN B");

        XWPFParagraph names = doc.createParagraph();
        names.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun nRun = names.createRun();
        nRun.setFontFamily(FONT);
        nRun.setFontSize(BODY_FONT_PT);
        nRun.setText("(" + tenantName + ")\t\t\t(" + teacherName + ")");
    }

    private static String formatVnd(BigDecimal value) {
        return VND_FORMAT.format(value);
    }

    private static String buildFilename(String teacherName, String startDate) {
        String slug = teacherName.replaceAll("\\s+", "-").toLowerCase(VI_VN);
        return "teacher-contract-" + slug + "-" + startDate + ".docx";
    }

    private static String required(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "Missing required data key '" + key + "' for teacher-contract template");
        }
        return value.toString();
    }

    private static String asString(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return (value instanceof BigDecimal bd) ? bd : new BigDecimal(value.toString());
    }

    static int minParagraphCount() {
        return MIN_PARAGRAPH_COUNT;
    }
}
