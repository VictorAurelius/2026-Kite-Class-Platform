package com.kiteclass.core.module.document;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentGenerationFoundationTest {

    @Test
    void documentFormat_has_three_supported_formats_for_wave5() {
        assertThat(DocumentFormat.values())
                .containsExactlyInAnyOrder(
                        DocumentFormat.PDF,
                        DocumentFormat.XLSX,
                        DocumentFormat.DOCX);
    }

    @Test
    void documentFormat_mime_types_match_spec() {
        assertThat(DocumentFormat.PDF.mimeType()).isEqualTo("application/pdf");
        assertThat(DocumentFormat.XLSX.mimeType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(DocumentFormat.DOCX.mimeType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Test
    void documentRequest_rejects_null_format() {
        assertThatThrownBy(() -> DocumentRequest.builder()
                        .format(null)
                        .templateId("invoice")
                        .tenantId("tenant-1")
                        .data(Map.of())
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("format");
    }

    @Test
    void documentRequest_rejects_blank_templateId() {
        assertThatThrownBy(() -> DocumentRequest.builder()
                        .format(DocumentFormat.PDF)
                        .templateId("")
                        .tenantId("tenant-1")
                        .data(Map.of())
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateId");
    }

    @Test
    void documentRequest_rejects_blank_tenantId() {
        assertThatThrownBy(() -> DocumentRequest.builder()
                        .format(DocumentFormat.PDF)
                        .templateId("invoice")
                        .tenantId("   ")
                        .data(Map.of())
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void documentRequest_null_data_becomes_empty_map() {
        DocumentRequest req = DocumentRequest.builder()
                .format(DocumentFormat.PDF)
                .templateId("invoice")
                .tenantId("tenant-1")
                .data(null)
                .build();

        assertThat(req.data()).isEmpty();
    }

    @Test
    void documentResponse_exposes_bytes_mime_and_filename() {
        byte[] payload = new byte[]{1, 2, 3};

        DocumentResponse resp = DocumentResponse.of(payload, DocumentFormat.PDF, "invoice-001.pdf");

        assertThat(resp.bytes()).isEqualTo(payload);
        assertThat(resp.mimeType()).isEqualTo("application/pdf");
        assertThat(resp.filename()).isEqualTo("invoice-001.pdf");
    }

    @Test
    void documentResponse_rejects_null_payload() {
        assertThatThrownBy(() -> DocumentResponse.of(null, DocumentFormat.PDF, "x.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bytes");
    }

    @Test
    void facade_stub_throws_unsupported_for_pdf_until_sub_pr_5_1() {
        DocumentGenerationService service = new DocumentGenerationService(java.util.List.of());
        DocumentRequest req = sampleRequest(DocumentFormat.PDF);

        assertThatThrownBy(() -> service.generate(req))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void facade_stub_throws_unsupported_for_xlsx_until_sub_pr_5_2() {
        DocumentGenerationService service = new DocumentGenerationService(java.util.List.of());
        DocumentRequest req = sampleRequest(DocumentFormat.XLSX);

        assertThatThrownBy(() -> service.generate(req))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("XLSX");
    }

    @Test
    void facade_stub_throws_unsupported_for_docx_until_sub_pr_5_3() {
        DocumentGenerationService service = new DocumentGenerationService(java.util.List.of());
        DocumentRequest req = sampleRequest(DocumentFormat.DOCX);

        assertThatThrownBy(() -> service.generate(req))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("DOCX");
    }

    @Test
    void facade_delegates_to_registered_generator_when_present() {
        byte[] payload = new byte[]{10, 20};
        Generator fakePdf = new Generator() {
            @Override
            public DocumentFormat format() {
                return DocumentFormat.PDF;
            }

            @Override
            public DocumentResponse generate(DocumentRequest request) {
                return DocumentResponse.of(payload, DocumentFormat.PDF, "x.pdf");
            }
        };
        DocumentGenerationService service = new DocumentGenerationService(java.util.List.of(fakePdf));

        DocumentResponse resp = service.generate(sampleRequest(DocumentFormat.PDF));

        assertThat(resp.bytes()).isEqualTo(payload);
    }

    private static DocumentRequest sampleRequest(DocumentFormat format) {
        return DocumentRequest.builder()
                .format(format)
                .templateId("sample")
                .tenantId("tenant-1")
                .data(Map.of("k", "v"))
                .build();
    }
}
