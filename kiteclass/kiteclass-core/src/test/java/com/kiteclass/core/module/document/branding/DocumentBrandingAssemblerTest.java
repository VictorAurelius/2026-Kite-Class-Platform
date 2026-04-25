package com.kiteclass.core.module.document.branding;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentBrandingAssemblerTest {

    private static final String TENANT = "tenant-test-001";

    private final DocumentBrandingAssembler assembler = new DocumentBrandingAssembler();

    @Test
    void enrich_null_branding_returns_request_unchanged() {
        DocumentRequest req = request(Map.of("invoice.number", "INV-001"));

        DocumentRequest out = assembler.enrich(req, null);

        assertThat(out).isSameAs(req);
    }

    @Test
    void enrich_full_branding_injects_all_five_keys() {
        DocumentRequest req = request(Map.of("invoice.number", "INV-001"));
        BrandingResponse branding = BrandingResponse.builder()
                .primaryColor("#2563EB")
                .secondaryColor("#8B5CF6")
                .accentColor("#10B981")
                .logoUrl("https://cdn.example.com/logo.png")
                .displayName("Kite Education Center")
                .build();

        DocumentRequest out = assembler.enrich(req, branding);

        assertThat(out.data())
                .containsEntry("branding.primaryColor", "#2563EB")
                .containsEntry("branding.secondaryColor", "#8B5CF6")
                .containsEntry("branding.accentColor", "#10B981")
                .containsEntry("branding.logoUrl", "https://cdn.example.com/logo.png")
                .containsEntry("branding.displayName", "Kite Education Center")
                .containsEntry("invoice.number", "INV-001");
    }

    @Test
    void enrich_preserves_other_fields() {
        DocumentRequest req = request(Map.of());
        BrandingResponse branding = BrandingResponse.builder().primaryColor("#111111").build();

        DocumentRequest out = assembler.enrich(req, branding);

        assertThat(out.format()).isEqualTo(DocumentFormat.PDF);
        assertThat(out.templateId()).isEqualTo("invoice");
        assertThat(out.tenantId()).isEqualTo(TENANT);
    }

    @Test
    void caller_provided_key_wins_over_branding() {
        Map<String, Object> callerData = new HashMap<>();
        callerData.put("branding.primaryColor", "#FF0000");
        DocumentRequest req = request(callerData);
        BrandingResponse branding = BrandingResponse.builder()
                .primaryColor("#2563EB")
                .accentColor("#10B981")
                .build();

        DocumentRequest out = assembler.enrich(req, branding);

        assertThat(out.data())
                .containsEntry("branding.primaryColor", "#FF0000")
                .containsEntry("branding.accentColor", "#10B981");
    }

    @Test
    void null_or_blank_branding_fields_are_skipped() {
        DocumentRequest req = request(Map.of());
        BrandingResponse branding = BrandingResponse.builder()
                .primaryColor("#2563EB")
                .secondaryColor(null)
                .accentColor("  ")
                .logoUrl("")
                .displayName("Kite")
                .build();

        DocumentRequest out = assembler.enrich(req, branding);

        assertThat(out.data())
                .containsEntry("branding.primaryColor", "#2563EB")
                .containsEntry("branding.displayName", "Kite")
                .doesNotContainKey("branding.secondaryColor")
                .doesNotContainKey("branding.accentColor")
                .doesNotContainKey("branding.logoUrl");
    }

    @Test
    void empty_branding_returns_request_unchanged() {
        DocumentRequest req = request(Map.of("invoice.number", "INV-001"));
        BrandingResponse branding = new BrandingResponse();

        DocumentRequest out = assembler.enrich(req, branding);

        assertThat(out.data()).containsOnly(Map.entry("invoice.number", "INV-001"));
    }

    @Test
    void output_data_map_is_unmodifiable() {
        DocumentRequest req = request(Map.of());
        BrandingResponse branding = BrandingResponse.builder().primaryColor("#111").build();

        DocumentRequest out = assembler.enrich(req, branding);

        Map<String, Object> data = out.data();
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> data.put("foo", "bar"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private DocumentRequest request(Map<String, Object> data) {
        return DocumentRequest.builder()
                .format(DocumentFormat.PDF)
                .templateId("invoice")
                .tenantId(TENANT)
                .data(data)
                .build();
    }
}
