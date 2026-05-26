package com.kiteclass.core.module.document.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentGenerationTestBase;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/**
 * TDD tests for {@link PdfGenerator}.
 *
 * <p>These tests drive Wave 5 Sub-PR 5.1 — they cover the Vietnamese tax invoice template
 * ({@code templateId = "invoice"}), validate the PDF byte stream via Apache PDFBox 2.0
 * {@link PDFTextStripper}, and guard the Vietnamese diacritic rendering contract
 * (see {@code rules.md} BR-DOC-PDF-002 and gotcha §3 in {@code .claude/skills/document-generation/pdf/SKILL.md}).
 */
class PdfGeneratorTest extends DocumentGenerationTestBase {

    private final PdfGenerator generator = new PdfGenerator();

    private static Map<String, Object> sampleInvoiceData() {
        return Map.of(
                "invoiceNumber", "INV-2026-0001",
                "issueDate", "2026-04-24",
                "buyerName", "Nguyễn Văn Đức",
                "buyerTaxCode", "0123456789",
                "buyerAddress", "12 Đường Lê Lợi, Quận 1, TP. Hồ Chí Minh",
                "items", List.of(
                        Map.of(
                                "description", "Học phí tháng 04/2026",
                                "qty", 1,
                                "unitPrice", new BigDecimal("2500000"),
                                "lineTotal", new BigDecimal("2500000"))),
                "subtotal", new BigDecimal("2500000"),
                "vatRate", new BigDecimal("0.08"),
                "vatAmount", new BigDecimal("200000"),
                "total", new BigDecimal("2700000"));
    }

    @Test
    void format_returns_pdf() {
        assertThat(generator.format()).isEqualTo(DocumentFormat.PDF);
    }

    @Test
    void generate_invoice_returns_non_empty_pdf_bytes() {
        DocumentRequest req = sampleRequest(DocumentFormat.PDF, "invoice", sampleInvoiceData());

        DocumentResponse resp = generator.generate(req);

        assertThat(resp.bytes()).isNotEmpty();
        assertThat(resp.mimeType()).isEqualTo("application/pdf");
        assertThat(resp.filename()).endsWith(".pdf");
        // PDF magic number: %PDF-
        assertThat(resp.bytes()[0]).isEqualTo((byte) '%');
        assertThat(resp.bytes()[1]).isEqualTo((byte) 'P');
        assertThat(resp.bytes()[2]).isEqualTo((byte) 'D');
        assertThat(resp.bytes()[3]).isEqualTo((byte) 'F');
    }

    @Test
    void generated_invoice_contains_invoice_number_and_buyer_name_and_vnd_total() throws Exception {
        DocumentRequest req = sampleRequest(DocumentFormat.PDF, "invoice", sampleInvoiceData());

        DocumentResponse resp = generator.generate(req);
        String text = extractText(resp.bytes());

        assertThat(text).contains("INV-2026-0001");
        assertThat(text).contains("Nguyễn Văn Đức");
        // VND total formatted with thousand separators — Vietnamese locale uses '.' separator.
        assertThat(text).contains("2.700.000");
    }

    @Test
    void vietnamese_diacritics_round_trip_through_pdf_text_layer() throws Exception {
        DocumentRequest req = sampleRequest(DocumentFormat.PDF, "invoice", sampleInvoiceData());

        DocumentResponse resp = generator.generate(req);
        String text = extractText(resp.bytes());

        // Full round-trip of Đ, ễ, ă (the three hardest Vietnamese glyphs in most fonts)
        assertThat(text).contains("Đ");
        assertThat(text).contains("ễ");
        assertThat(text).contains("ă");
        // Full name + key diacritic-heavy words
        assertThat(text).contains("Nguyễn Văn Đức");
        assertThat(text).contains("Học phí");
    }

    @Test
    void unknown_template_id_throws_illegal_argument() {
        DocumentRequest req = sampleRequest(DocumentFormat.PDF, "does-not-exist", sampleInvoiceData());

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
                .format(DocumentFormat.XLSX)
                .templateId("invoice")
                .tenantId(SAMPLE_TENANT_ID)
                .data(sampleInvoiceData())
                .build();

        assertThatThrownBy(() -> generator.generate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("format");
    }

    @Test
    void invoice_template_renders_all_line_items() throws Exception {
        Map<String, Object> data = Map.of(
                "invoiceNumber", "INV-2026-0042",
                "issueDate", "2026-04-24",
                "buyerName", "Trần Thị Ánh",
                "buyerTaxCode", "9876543210",
                "buyerAddress", "99 Nguyễn Trãi, Hà Nội",
                "items", List.of(
                        Map.of("description", "Khóa học Toán", "qty", 2,
                                "unitPrice", new BigDecimal("1000000"), "lineTotal", new BigDecimal("2000000")),
                        Map.of("description", "Khóa học Văn", "qty", 1,
                                "unitPrice", new BigDecimal("800000"), "lineTotal", new BigDecimal("800000"))),
                "subtotal", new BigDecimal("2800000"),
                "vatRate", new BigDecimal("0.08"),
                "vatAmount", new BigDecimal("224000"),
                "total", new BigDecimal("3024000"));

        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.PDF, "invoice", data));
        String text = extractText(resp.bytes());

        assertThat(text).contains("Khóa học Toán");
        assertThat(text).contains("Khóa học Văn");
        assertThat(text).contains("INV-2026-0042");
        assertThat(text).contains("Trần Thị Ánh");
    }

    @Test
    void branding_displayName_renders_in_header_when_provided() throws Exception {
        java.util.HashMap<String, Object> data = new java.util.HashMap<>(sampleInvoiceData());
        data.put("branding.primaryColor", "#2563EB");
        data.put("branding.logoUrl", "https://cdn.example.com/logo.png");
        data.put("branding.displayName", "Trung tâm Kite");

        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.PDF, "invoice", data));
        String text = extractText(resp.bytes());

        assertThat(text).contains("Trung tâm Kite");
    }

    @Test
    void branding_absent_does_not_emit_header_block_or_break_render() throws Exception {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.PDF, "invoice", sampleInvoiceData()));
        String text = extractText(resp.bytes());

        // Original title still present, no leftover placeholder text from the branded header.
        assertThat(text).contains("HÓA ĐƠN GIÁ TRỊ GIA TĂNG");
        assertThat(text).doesNotContain("Tenant Name");
    }

    @Test
    void invoice_render_under_soft_cap_for_regression_canary() {
        // GAP-216 — soft-cap regression canary, not the SLO.
        // SLO per BR-DOC-PDF-007 is p95 <2s on production hardware. CI runners + first render
        // (font load + Thymeleaf template parse) are slower; observed 4.8s on a constrained
        // WSL2 runner 2026-05-26 → 6s ceiling adopted (3× SLO) with headroom for cold-JVM +
        // shared CI executor variance. "If a render takes longer than this, something is
        // genuinely wrong" — fail the test, not pass-through. True p95 measurement requires
        // JMH (deferred to follow-up GAP-750 per GAP-216 §Acceptance).
        DocumentRequest req = sampleRequest(DocumentFormat.PDF, "invoice", sampleInvoiceData());

        long startNs = System.nanoTime();
        DocumentResponse resp = generator.generate(req);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        assertThat(resp.bytes()).isNotEmpty();
        assertThat(elapsedMs)
                .as("PDF first render took %d ms — should stay under 6000 ms soft cap (BR-DOC-PDF-007)", elapsedMs)
                .isLessThan(6000);
    }

    @Test
    void filename_incorporates_invoice_number_when_available() {
        DocumentResponse resp = generator.generate(
                sampleRequest(DocumentFormat.PDF, "invoice", sampleInvoiceData()));

        assertThat(resp.filename()).contains("INV-2026-0001");
    }

    private static String extractText(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }
}
