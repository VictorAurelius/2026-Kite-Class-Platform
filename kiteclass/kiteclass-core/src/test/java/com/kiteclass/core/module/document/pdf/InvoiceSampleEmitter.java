package com.kiteclass.core.module.document.pdf;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Not a test — utility to (re)generate {@code document-samples/invoice-sample.pdf} when the
 * template changes. Run with {@code -Dtest=InvoiceSampleEmitter#emit} after removing the
 * {@code @Disabled}. Keep disabled by default so the regular test run does not churn the
 * committed sample (byte output varies across JDK/font versions).
 */
@Disabled("Utility; remove annotation locally when regenerating invoice-sample.pdf")
class InvoiceSampleEmitter {

    @Test
    void emit() throws Exception {
        DocumentRequest req = DocumentRequest.builder()
                .format(DocumentFormat.PDF)
                .templateId("invoice")
                .tenantId("tenant-sample")
                .data(Map.of(
                        "invoiceNumber", "INV-2026-SAMPLE",
                        "issueDate", "2026-04-24",
                        "buyerName", "Nguyễn Văn Đức",
                        "buyerTaxCode", "0123456789",
                        "buyerAddress", "12 Đường Lê Lợi, Quận 1, TP. Hồ Chí Minh",
                        "items", List.of(
                                Map.of("description", "Học phí tháng 04/2026", "qty", 1,
                                        "unitPrice", new BigDecimal("2500000"),
                                        "lineTotal", new BigDecimal("2500000"))),
                        "subtotal", new BigDecimal("2500000"),
                        "vatRate", new BigDecimal("0.08"),
                        "vatAmount", new BigDecimal("200000"),
                        "total", new BigDecimal("2700000")))
                .build();
        DocumentResponse resp = new PdfGenerator().generate(req);
        Path out = Path.of("src/test/resources/document-samples/invoice-sample.pdf");
        Files.write(out, resp.bytes());
    }
}
