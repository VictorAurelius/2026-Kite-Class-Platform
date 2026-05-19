package com.kiteclass.core.module.document.docx;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
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
 * Assembles the VN CS thesis report DOCX with HUST/UIT/UET academic norms (A4, asymmetric
 * 3-2-2-3 cm margins for binding gutter, Times New Roman 13pt body / 14pt heading).
 *
 * <p>Wave 100.7 Phase 3b shipped via Create pipeline (per {@code docx-pipeline-scoping.md}
 * §3 Decision). LibreOffice/Word offline template authoring unavailable on this stack;
 * Create pipeline programmatically builds the entire skeleton via POI XWPF — same approach
 * as {@link TeacherContractBuilder} (Wave 5).
 *
 * <p>Skeleton structure (V1):
 * <ol>
 *   <li>Cover page: school header + title + student info + supervisor + year</li>
 *   <li>TOC stub ({@code [Mục lục]} placeholder — Word auto-TOC field code deferred to V2)</li>
 *   <li>7 chapter H1 shells with optional body text from {@code chapter.N.body} data keys</li>
 *   <li>Bibliography section header with optional {@code bibliography.entries} body</li>
 *   <li>Appendix section header</li>
 * </ol>
 *
 * <p>VN academic norm references:
 * <ul>
 *   <li>A4: 11906 × 16838 twips (210 × 297 mm)</li>
 *   <li>Margin top/bottom/right: 2 cm = 1134 twips; left: 3 cm = 1701 twips (binding gutter)</li>
 *   <li>Times New Roman handles Unicode VN diacritics natively (no font substitution)</li>
 * </ul>
 *
 * <p>Required data keys (caller populates {@link DocumentRequest#data()}):
 * <ul>
 *   <li>{@code title} — thesis title (VN, sentence case or title case)</li>
 *   <li>{@code studentName} — full student name</li>
 *   <li>{@code studentId} — MSSV</li>
 *   <li>{@code supervisor} — GVHD full name with title (e.g., "TS. Nguyễn Văn A")</li>
 *   <li>{@code year} — academic year (e.g., "2026")</li>
 *   <li>{@code school} — school/department (e.g., "Đại học Bách Khoa Hà Nội")</li>
 * </ul>
 *
 * <p>Optional data keys (when caller assembles content):
 * <ul>
 *   <li>{@code chapter.N.title} — chapter N display title (default to numbered heading)</li>
 *   <li>{@code chapter.N.body} — chapter N body text block (V1 plain text; V2 MD parse)</li>
 *   <li>{@code bibliography.entries} — bibliography text block (V1 plain text)</li>
 * </ul>
 *
 * <p>Defaults for chapter titles match the Wave 100.7 thesis structure
 * (see {@code documents/08-thesis/chapter-mapping.md} mapping table).
 */
final class ThesisReportBuilder {

    private static final Locale VI_VN = Locale.forLanguageTag("vi-VN");
    private static final String FONT = "Times New Roman";
    private static final int BODY_FONT_PT = 13;
    private static final int HEADING_FONT_PT = 14;
    private static final int COVER_TITLE_FONT_PT = 18;
    private static final int A4_WIDTH_TWIPS = 11906;
    private static final int A4_HEIGHT_TWIPS = 16838;
    private static final int MARGIN_2CM_TWIPS = 1134;
    private static final int MARGIN_3CM_TWIPS = 1701;
    private static final int CHAPTER_COUNT = 7;
    private static final int MIN_PARAGRAPH_COUNT = 12;

    /**
     * Default chapter titles aligned with Wave 100.7 thesis chapter structure. Override per
     * chapter via {@code chapter.N.title} data key.
     */
    private static final List<String> DEFAULT_CHAPTER_TITLES = List.of(
            "Chương 1: Tổng quan",
            "Chương 2: Kiến trúc hệ thống",
            "Chương 3: Triển khai",
            "Chương 4: Triển khai vận hành và kết quả",
            "Chương 5: Lộ trình triển khai",
            "Chương 6: Kiểm thử và đánh giá",
            "Chương 7: Kết luận");

    DocumentResponse build(DocumentRequest request) {
        Map<String, Object> data = request.data();
        String title = required(data, "title");
        String studentName = required(data, "studentName");
        String studentId = required(data, "studentId");
        String supervisor = required(data, "supervisor");
        String year = required(data, "year");
        String school = required(data, "school");

        try (XWPFDocument doc = new XWPFDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            applyA4PageWithBindingGutter(doc);

            renderCoverPage(doc, title, studentName, studentId, supervisor, year, school);
            renderTocStub(doc);
            renderChapterShells(doc, data);
            renderBibliographySection(doc, data);
            renderAppendixSection(doc);

            doc.write(out);
            return DocumentResponse.of(
                    out.toByteArray(), DocumentFormat.DOCX, buildFilename(studentName, year));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build thesis report docx", ex);
        }
    }

    private static void applyA4PageWithBindingGutter(XWPFDocument doc) {
        CTSectPr sectPr = doc.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = sectPr.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(A4_WIDTH_TWIPS));
        pageSize.setH(BigInteger.valueOf(A4_HEIGHT_TWIPS));
        pageSize.setOrient(STPageOrientation.PORTRAIT);
        CTPageMar margin = sectPr.addNewPgMar();
        margin.setTop(BigInteger.valueOf(MARGIN_2CM_TWIPS));
        margin.setRight(BigInteger.valueOf(MARGIN_2CM_TWIPS));
        margin.setBottom(BigInteger.valueOf(MARGIN_2CM_TWIPS));
        margin.setLeft(BigInteger.valueOf(MARGIN_3CM_TWIPS));
    }

    private static void renderCoverPage(
            XWPFDocument doc,
            String title,
            String studentName,
            String studentId,
            String supervisor,
            String year,
            String school) {
        addCentered(doc, school.toUpperCase(VI_VN), true, HEADING_FONT_PT);
        addCentered(doc, "KHOÁ LUẬN TỐT NGHIỆP", true, HEADING_FONT_PT);
        addBlankLine(doc);
        addBlankLine(doc);
        addCentered(doc, title, true, COVER_TITLE_FONT_PT);
        addBlankLine(doc);
        addBlankLine(doc);
        addCentered(doc, "Sinh viên thực hiện: " + studentName, false, BODY_FONT_PT);
        addCentered(doc, "Mã số sinh viên: " + studentId, false, BODY_FONT_PT);
        addCentered(doc, "Giáo viên hướng dẫn: " + supervisor, false, BODY_FONT_PT);
        addBlankLine(doc);
        addCentered(doc, "Năm " + year, true, BODY_FONT_PT);
        addPageBreak(doc);
    }

    private static void renderTocStub(XWPFDocument doc) {
        addCentered(doc, "MỤC LỤC", true, HEADING_FONT_PT);
        addBlankLine(doc);
        addBody(
                doc,
                "[Mục lục tự động sẽ được Word render khi mở file — cập nhật bằng Ctrl+A → F9. "
                        + "V1 placeholder; V2 sẽ inject TOC field code.]");
        addPageBreak(doc);
    }

    private static void renderChapterShells(XWPFDocument doc, Map<String, Object> data) {
        for (int n = 1; n <= CHAPTER_COUNT; n++) {
            String key = "chapter." + n;
            String defaultTitle = DEFAULT_CHAPTER_TITLES.get(n - 1);
            String chapterTitle = asString(data.get(key + ".title"), defaultTitle);
            String chapterBody = asString(data.get(key + ".body"), "");

            addHeadingLeft(doc, chapterTitle, true, HEADING_FONT_PT);
            if (chapterBody.isBlank()) {
                addBody(doc, "[Nội dung Chương " + n + " — placeholder. Inject qua data key '"
                        + key + ".body' khi assemble.]");
            } else {
                addBody(doc, chapterBody);
            }
            addPageBreak(doc);
        }
    }

    private static void renderBibliographySection(XWPFDocument doc, Map<String, Object> data) {
        addHeadingLeft(doc, "TÀI LIỆU THAM KHẢO", true, HEADING_FONT_PT);
        String entries = asString(data.get("bibliography.entries"), "");
        if (entries.isBlank()) {
            addBody(
                    doc,
                    "[Bibliography entries — placeholder. Inject qua data key 'bibliography.entries' "
                            + "khi assemble. IEEE format per documents/08-thesis/references/bibliography.md.]");
        } else {
            addBody(doc, entries);
        }
        addPageBreak(doc);
    }

    private static void renderAppendixSection(XWPFDocument doc) {
        addHeadingLeft(doc, "PHỤ LỤC", true, HEADING_FONT_PT);
        addBody(
                doc,
                "[Phụ lục — placeholder. Audit reports, benchmark data, beta reviews. V2 inject.]");
    }

    private static void addCentered(XWPFDocument doc, String text, boolean bold, int sizePt) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setFontFamily(FONT);
        run.setFontSize(sizePt);
        run.setBold(bold);
        run.setText(text);
    }

    private static void addHeadingLeft(XWPFDocument doc, String text, boolean bold, int sizePt) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun run = p.createRun();
        run.setFontFamily(FONT);
        run.setFontSize(sizePt);
        run.setBold(bold);
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

    private static void addBlankLine(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setFontFamily(FONT);
        run.setFontSize(BODY_FONT_PT);
        run.setText("");
    }

    private static void addPageBreak(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.addBreak(org.apache.poi.xwpf.usermodel.BreakType.PAGE);
    }

    private static String required(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "Missing required data key '" + key + "' for thesis-report template");
        }
        return value.toString();
    }

    private static String asString(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private static String buildFilename(String studentName, String year) {
        String slug = studentName.replaceAll("\\s+", "-").toLowerCase(VI_VN);
        return "thesis-report-" + slug + "-" + year + ".docx";
    }

    static int minParagraphCount() {
        return MIN_PARAGRAPH_COUNT;
    }

    static int marginLeftTwips() {
        return MARGIN_3CM_TWIPS;
    }

    static int marginRightTwips() {
        return MARGIN_2CM_TWIPS;
    }

    static int bodyFontPt() {
        return BODY_FONT_PT;
    }

    static int chapterCount() {
        return CHAPTER_COUNT;
    }
}
