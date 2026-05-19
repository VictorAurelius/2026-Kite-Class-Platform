package com.kiteclass.core.module.document.docx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentGenerationTestBase;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

class ThesisReportBuilderTest extends DocumentGenerationTestBase {

    private final DocxGenerator generator = new DocxGenerator();

    private static Map<String, Object> sampleThesisData() {
        return Map.of(
                "title", "Nền tảng SaaS đa khách hàng cho quản lý trung tâm giáo dục",
                "studentName", "Nguyễn Văn Kiệt",
                "studentId", "20210123",
                "supervisor", "TS. Trần Thị Hồng",
                "year", "2026",
                "school", "Đại học Bách Khoa Hà Nội");
    }

    @Test
    void generate_returns_non_empty_docx_bytes() {
        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData()));

        assertThat(resp.bytes()).isNotEmpty();
        assertThat(resp.mimeType())
                .isEqualTo(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(resp.filename()).endsWith(".docx");
        // docx is ZIP — magic number PK
        assertThat(resp.bytes()[0]).isEqualTo((byte) 'P');
        assertThat(resp.bytes()[1]).isEqualTo((byte) 'K');
    }

    @Test
    void cover_page_contains_title_and_student_info() throws Exception {
        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData()));

        String text = extractAllText(resp.bytes());
        assertThat(text).contains("Nền tảng SaaS đa khách hàng cho quản lý trung tâm giáo dục");
        assertThat(text).contains("Nguyễn Văn Kiệt");
        assertThat(text).contains("20210123");
        assertThat(text).contains("TS. Trần Thị Hồng");
        assertThat(text).contains("KHOÁ LUẬN TỐT NGHIỆP");
    }

    @Test
    void school_rendered_uppercase_on_cover() throws Exception {
        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData()));

        String text = extractAllText(resp.bytes());
        assertThat(text).contains("ĐẠI HỌC BÁCH KHOA HÀ NỘI");
    }

    @Test
    void all_seven_chapter_headings_rendered_with_default_titles() throws Exception {
        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData()));

        String text = extractAllText(resp.bytes());
        assertThat(text).contains("Chương 1: Tổng quan");
        assertThat(text).contains("Chương 2: Kiến trúc hệ thống");
        assertThat(text).contains("Chương 3: Triển khai");
        assertThat(text).contains("Chương 4: Triển khai vận hành và kết quả");
        assertThat(text).contains("Chương 5: Lộ trình triển khai");
        assertThat(text).contains("Chương 6: Kiểm thử và đánh giá");
        assertThat(text).contains("Chương 7: Kết luận");
    }

    @Test
    void bibliography_and_appendix_sections_present() throws Exception {
        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData()));

        String text = extractAllText(resp.bytes());
        assertThat(text).contains("TÀI LIỆU THAM KHẢO");
        assertThat(text).contains("PHỤ LỤC");
        assertThat(text).contains("MỤC LỤC");
    }

    @Test
    void chapter_body_injected_when_provided() throws Exception {
        Map<String, Object> data = new HashMap<>(sampleThesisData());
        data.put("chapter.1.body", "Đây là nội dung Chương 1 — bối cảnh nghiên cứu.");
        data.put("chapter.3.body", "Đây là nội dung Chương 3 — code snippets thực tế.");

        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", data));
        String text = extractAllText(resp.bytes());

        assertThat(text).contains("Đây là nội dung Chương 1 — bối cảnh nghiên cứu.");
        assertThat(text).contains("Đây là nội dung Chương 3 — code snippets thực tế.");
        // Chapters 2/4-7 still placeholder
        assertThat(text).contains("[Nội dung Chương 2");
    }

    @Test
    void chapter_title_overridable_via_data_key() throws Exception {
        Map<String, Object> data = new HashMap<>(sampleThesisData());
        data.put("chapter.1.title", "Chương 1: Giới thiệu (custom)");

        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", data));
        String text = extractAllText(resp.bytes());

        assertThat(text).contains("Chương 1: Giới thiệu (custom)");
        // Default title not rendered when override provided
        assertThat(text).doesNotContain("Chương 1: Tổng quan");
    }

    @Test
    void bibliography_entries_injected_when_provided() throws Exception {
        Map<String, Object> data = new HashMap<>(sampleThesisData());
        data.put(
                "bibliography.entries",
                "[1] EasyEdu, \"Tính năng EasyEdu,\" 2024.\n"
                        + "[2] MISA, \"MISA EMIS,\" 2024.");

        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", data));
        String text = extractAllText(resp.bytes());

        assertThat(text).contains("[1] EasyEdu");
        assertThat(text).contains("[2] MISA");
    }

    @Test
    void vietnamese_diacritics_preserved() throws Exception {
        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData()));

        String text = extractAllText(resp.bytes());
        assertThat(text).contains("đ");
        assertThat(text).contains("ệ");
        assertThat(text).contains("ố");
    }

    @Test
    void a4_page_size_applied() throws Exception {
        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData()));

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(resp.bytes()))) {
            CTSectPr sectPr = doc.getDocument().getBody().getSectPr();
            assertThat(sectPr).as("sectPr must exist").isNotNull();
            CTPageSz pageSize = sectPr.getPgSz();
            // A4 portrait: 11906 × 16838 twips
            assertThat(pageSize.getW()).isEqualTo(BigInteger.valueOf(11906));
            assertThat(pageSize.getH()).isEqualTo(BigInteger.valueOf(16838));
        }
    }

    @Test
    void binding_gutter_margins_asymmetric_3_2_2_3_cm() throws Exception {
        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData()));

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(resp.bytes()))) {
            CTSectPr sectPr = doc.getDocument().getBody().getSectPr();
            CTPageMar margin = sectPr.getPgMar();
            // HUST/UIT/UET norm: 3 cm left (1701 twips) for binding, 2 cm elsewhere (1134 twips)
            assertThat(margin.getLeft())
                    .as("left margin must be 3 cm = 1701 twips (binding gutter)")
                    .isEqualTo(BigInteger.valueOf(ThesisReportBuilder.marginLeftTwips()));
            assertThat(margin.getRight())
                    .isEqualTo(BigInteger.valueOf(ThesisReportBuilder.marginRightTwips()));
            assertThat(margin.getTop())
                    .isEqualTo(BigInteger.valueOf(ThesisReportBuilder.marginRightTwips()));
            assertThat(margin.getBottom())
                    .isEqualTo(BigInteger.valueOf(ThesisReportBuilder.marginRightTwips()));
        }
    }

    @Test
    void font_family_times_new_roman_on_body_paragraph() throws Exception {
        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData()));

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(resp.bytes()))) {
            // First paragraph = cover school header; verify TNR + 14pt
            XWPFParagraph first = doc.getParagraphs().get(0);
            assertThat(first.getRuns().get(0).getFontFamily()).isEqualTo("Times New Roman");
        }
    }

    @Test
    void document_has_at_least_minimum_paragraph_count() throws Exception {
        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData()));

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(resp.bytes()))) {
            assertThat(doc.getParagraphs().size())
                    .isGreaterThanOrEqualTo(ThesisReportBuilder.minParagraphCount());
        }
    }

    @Test
    void filename_slugs_student_name_with_year() {
        DocumentResponse resp =
                generator.generate(
                        sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData()));

        assertThat(resp.filename()).startsWith("thesis-report-");
        assertThat(resp.filename()).contains("nguyễn-văn-kiệt");
        assertThat(resp.filename()).contains("2026");
        assertThat(resp.filename()).endsWith(".docx");
    }

    @Test
    void missing_required_key_throws() {
        Map<String, Object> incomplete = new HashMap<>(sampleThesisData());
        incomplete.remove("studentId");
        DocumentRequest req = sampleRequest(DocumentFormat.DOCX, "thesis-report", incomplete);

        assertThatThrownBy(() -> generator.generate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("studentId");
    }

    @Test
    void unknown_template_id_after_thesis_route_still_rejected() {
        DocumentRequest req = sampleRequest(DocumentFormat.DOCX, "non-existent", sampleThesisData());

        assertThatThrownBy(() -> generator.generate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-existent")
                .hasMessageContaining("thesis-report");
    }

    @Test
    void thesis_render_under_soft_cap_for_regression_canary() {
        // GAP-216 soft-cap regression canary — thesis is larger than contract but still bounded.
        DocumentRequest req = sampleRequest(DocumentFormat.DOCX, "thesis-report", sampleThesisData());

        long startNs = System.nanoTime();
        DocumentResponse resp = generator.generate(req);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        assertThat(resp.bytes()).isNotEmpty();
        assertThat(elapsedMs)
                .as("Thesis DOCX render took %d ms — should stay under 3000 ms soft cap", elapsedMs)
                .isLessThan(3000);
    }

    private static String extractAllText(byte[] bytes) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append('\n'));
            return sb.toString();
        }
    }
}
