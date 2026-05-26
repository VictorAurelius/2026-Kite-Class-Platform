package com.kiteclass.core.module.document.docx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentGenerationTestBase;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class DocxGeneratorTest extends DocumentGenerationTestBase {

    private final DocxGenerator generator = new DocxGenerator();

    private static Map<String, Object> sampleContractData() {
        return Map.of(
                "teacherName", "Nguyễn Văn Đức",
                "teacherIdNumber", "012345678",
                "tenantName", "Trường THPT Lê Quý Đôn",
                "tenantAddress", "12 Đường Lê Lợi, Quận 1, TP. Hồ Chí Minh",
                "startDate", "2026-05-01",
                "endDate", "2027-04-30",
                "salaryVnd", new BigDecimal("15000000"),
                "subjects", "Toán, Vật lý");
    }

    @Test
    void format_returns_docx() {
        assertThat(generator.format()).isEqualTo(DocumentFormat.DOCX);
    }

    @Test
    void generate_returns_non_empty_docx_bytes() {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.DOCX, "teacher-contract", sampleContractData()));

        assertThat(resp.bytes()).isNotEmpty();
        assertThat(resp.mimeType()).isEqualTo(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(resp.filename()).endsWith(".docx");
        // docx is ZIP — magic number PK
        assertThat(resp.bytes()[0]).isEqualTo((byte) 'P');
        assertThat(resp.bytes()[1]).isEqualTo((byte) 'K');
    }

    @Test
    void generated_docx_contains_title_and_parties() throws Exception {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.DOCX, "teacher-contract", sampleContractData()));

        String text = extractAllText(resp.bytes());
        assertThat(text).contains("HỢP ĐỒNG GIẢNG DẠY");
        assertThat(text).contains("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM");
        assertThat(text).contains("Nguyễn Văn Đức");
        assertThat(text).contains("Trường THPT Lê Quý Đôn");
    }

    @Test
    void vietnamese_diacritics_preserved_in_contract_text() throws Exception {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.DOCX, "teacher-contract", sampleContractData()));

        String text = extractAllText(resp.bytes());
        assertThat(text).contains("Đ");
        assertThat(text).contains("ễ");
        assertThat(text).contains("Toán, Vật lý");
    }

    @Test
    void salary_formatted_with_vn_thousand_separator() throws Exception {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.DOCX, "teacher-contract", sampleContractData()));

        String text = extractAllText(resp.bytes());
        assertThat(text).contains("15.000.000");
    }

    @Test
    void dates_appear_in_contract() throws Exception {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.DOCX, "teacher-contract", sampleContractData()));

        String text = extractAllText(resp.bytes());
        assertThat(text).contains("2026-05-01");
        assertThat(text).contains("2027-04-30");
    }

    @Test
    void document_has_at_least_minimum_paragraph_count() throws Exception {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.DOCX, "teacher-contract", sampleContractData()));

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(resp.bytes()))) {
            assertThat(doc.getParagraphs().size())
                    .isGreaterThanOrEqualTo(TeacherContractBuilder.minParagraphCount());
        }
    }

    @Test
    void filename_slugs_teacher_name() {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.DOCX, "teacher-contract", sampleContractData()));

        assertThat(resp.filename()).contains("nguyễn-văn-đức");
        assertThat(resp.filename()).contains("2026-05-01");
    }

    @Test
    void unknown_template_id_throws() {
        DocumentRequest req = sampleRequest(DocumentFormat.DOCX, "does-not-exist", sampleContractData());

        assertThatThrownBy(() -> generator.generate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does-not-exist");
    }

    @Test
    void generate_rejects_null_request() {
        assertThatThrownBy(() -> generator.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request");
    }

    @Test
    void generate_rejects_request_with_mismatched_format() {
        DocumentRequest req = DocumentRequest.builder()
                .format(DocumentFormat.PDF)
                .templateId("teacher-contract")
                .tenantId(SAMPLE_TENANT_ID)
                .data(sampleContractData())
                .build();

        assertThatThrownBy(() -> generator.generate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("format");
    }

    @Test
    void branding_primary_color_applied_to_contract_title() throws Exception {
        Map<String, Object> data = new HashMap<>(sampleContractData());
        data.put("branding.primaryColor", "#2563EB");

        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.DOCX, "teacher-contract", data));

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(resp.bytes()))) {
            // Third heading = "HỢP ĐỒNG GIẢNG DẠY" per TeacherContractBuilder.build().
            org.apache.poi.xwpf.usermodel.XWPFParagraph title = doc.getParagraphs().get(2);
            assertThat(title.getText()).contains("HỢP ĐỒNG GIẢNG DẠY");
            assertThat(title.getRuns().get(0).getColor()).isEqualToIgnoringCase("2563EB");
        }
    }

    @Test
    void contract_render_under_soft_cap_for_regression_canary() {
        // GAP-216 — soft-cap regression canary, NOT the SLO. DOCX is faster than PDF
        // (no font load, no template parse); 2s ceiling sufficient. True p95 measurement
        // requires JMH suite (deferred to follow-up GAP-750).
        DocumentRequest req = sampleRequest(DocumentFormat.DOCX, "teacher-contract", sampleContractData());

        long startNs = System.nanoTime();
        DocumentResponse resp = generator.generate(req);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        assertThat(resp.bytes()).isNotEmpty();
        assertThat(elapsedMs)
                .as("DOCX render took %d ms — should stay under 2000 ms soft cap", elapsedMs)
                .isLessThan(2000);
    }

    @Test
    void missing_required_key_throws() {
        Map<String, Object> incomplete = new HashMap<>(sampleContractData());
        incomplete.remove("teacherName");
        DocumentRequest req = sampleRequest(DocumentFormat.DOCX, "teacher-contract", incomplete);

        assertThatThrownBy(() -> generator.generate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teacherName");
    }

    private static String extractAllText(byte[] bytes) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append('\n'));
            return sb.toString();
        }
    }
}
