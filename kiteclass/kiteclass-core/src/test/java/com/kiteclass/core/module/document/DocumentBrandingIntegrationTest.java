package com.kiteclass.core.module.document;

import com.kiteclass.core.module.document.branding.DocumentBrandingAssembler;
import com.kiteclass.core.module.document.docx.DocxGenerator;
import com.kiteclass.core.module.document.pdf.PdfGenerator;
import com.kiteclass.core.module.document.xlsx.XlsxGenerator;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sub-PR 5.5 cross-format consistency test.
 *
 * <p>One {@link BrandingResponse} flows through the {@link DocumentBrandingAssembler} into all
 * three Wave 5 generators. Each format must surface the tenant primary colour in a
 * format-appropriate location:
 * <ul>
 *   <li>PDF — branded header block contains the tenant {@code displayName}.</li>
 *   <li>XLSX — header row fill matches the primary-colour RGB exactly.</li>
 *   <li>DOCX — title paragraph run carries the primary-colour hex (no leading {@code #}).</li>
 * </ul>
 *
 * <p>Pure JUnit (no Spring context) so the test is fast and independent of MVC/security wiring —
 * the controller path is covered separately by {@code DocumentGenerationControllerTest}.
 */
class DocumentBrandingIntegrationTest {

    private static final String PRIMARY_HEX = "#2563EB";
    private static final String DISPLAY_NAME = "Trung tâm Kite Education";

    private final DocumentBrandingAssembler assembler = new DocumentBrandingAssembler(
            new com.kiteclass.core.module.marketing.config.LandingPageSafetyProperties());
    private final DocumentGenerationService service = new DocumentGenerationService(
            List.of(new PdfGenerator(), new XlsxGenerator(), new DocxGenerator()));

    private static BrandingResponse fixedBranding() {
        return BrandingResponse.builder()
                .primaryColor(PRIMARY_HEX)
                .secondaryColor("#8B5CF6")
                .accentColor("#10B981")
                .logoUrl("https://cdn.kitehub.me/logo.png")
                .displayName(DISPLAY_NAME)
                .build();
    }

    private DocumentResponse generate(DocumentFormat format, String templateId, Map<String, Object> data) {
        DocumentRequest base = DocumentRequest.builder()
                .format(format)
                .templateId(templateId)
                .tenantId("tenant-cross-format")
                .data(data)
                .build();
        DocumentRequest enriched = assembler.enrich(base, fixedBranding());
        return service.generate(enriched);
    }

    @Test
    void pdf_invoice_renders_displayName_from_branding() throws Exception {
        Map<String, Object> data = Map.of(
                "invoiceNumber", "INV-2026-0001",
                "issueDate", "2026-04-25",
                "buyerName", "Nguyễn Văn Đức",
                "buyerTaxCode", "0123456789",
                "buyerAddress", "12 Đường Lê Lợi, Quận 1, TP. Hồ Chí Minh",
                "items", List.of(Map.of(
                        "description", "Học phí",
                        "qty", 1,
                        "unitPrice", new BigDecimal("2500000"),
                        "lineTotal", new BigDecimal("2500000"))),
                "subtotal", new BigDecimal("2500000"),
                "vatRate", new BigDecimal("0.08"),
                "vatAmount", new BigDecimal("200000"),
                "total", new BigDecimal("2700000"));

        DocumentResponse resp = generate(DocumentFormat.PDF, "invoice", data);

        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(resp.bytes()))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            assertThat(text).contains(DISPLAY_NAME);
        }
    }

    @Test
    void xlsx_attendance_paints_header_row_with_primaryColor() throws Exception {
        Map<String, Object> data = Map.of(
                "weekStart", "2026-04-20",
                "className", "10A1",
                "students", List.of(Map.of("id", "S001", "name", "Nguyễn Văn Đức")),
                "attendance", Map.of("S001", Map.of("Thứ 2", "P")));

        DocumentResponse resp = generate(DocumentFormat.XLSX, "attendance", data);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(resp.bytes()))) {
            XSSFCell headerCell = wb.getSheetAt(0).getRow(2).getCell(0);
            XSSFColor fill = headerCell.getCellStyle().getFillForegroundColorColor();
            byte[] rgb = fill.getRGB();
            assertThat(rgb[0] & 0xFF).isEqualTo(0x25);
            assertThat(rgb[1] & 0xFF).isEqualTo(0x63);
            assertThat(rgb[2] & 0xFF).isEqualTo(0xEB);
        }
    }

    @Test
    void docx_contract_title_carries_primaryColor_hex() throws Exception {
        Map<String, Object> data = Map.of(
                "teacherName", "Nguyễn Văn Đức",
                "teacherIdNumber", "012345678",
                "tenantName", DISPLAY_NAME,
                "tenantAddress", "12 Đường Lê Lợi",
                "startDate", "2026-05-01",
                "endDate", "2027-04-30",
                "salaryVnd", new BigDecimal("15000000"),
                "subjects", "Toán");

        DocumentResponse resp = generate(DocumentFormat.DOCX, "teacher-contract", data);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(resp.bytes()))) {
            // 3rd paragraph = "HỢP ĐỒNG GIẢNG DẠY" title (per TeacherContractBuilder.build()).
            XWPFParagraph title = doc.getParagraphs().get(2);
            assertThat(title.getText()).contains("HỢP ĐỒNG GIẢNG DẠY");
            assertThat(title.getRuns().get(0).getColor()).isEqualToIgnoringCase("2563EB");
        }
    }
}
