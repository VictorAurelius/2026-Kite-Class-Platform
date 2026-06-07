package com.kiteclass.core.module.document.branding;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.marketing.config.LandingPageSafetyProperties;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentBrandingAssemblerTest {

    private static final String TENANT = "tenant-test-001";

    // Default allowlist: localhost, minio, kite-minio, cdn.kitehub.me, assets.kitehub.me
    private final DocumentBrandingAssembler assembler =
            new DocumentBrandingAssembler(new LandingPageSafetyProperties());

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
                .logoUrl("https://cdn.kitehub.me/logo.png")
                .displayName("Kite Education Center")
                .build();

        DocumentRequest out = assembler.enrich(req, branding);

        assertThat(out.data())
                .containsEntry("branding.primaryColor", "#2563EB")
                .containsEntry("branding.secondaryColor", "#8B5CF6")
                .containsEntry("branding.accentColor", "#10B981")
                .containsEntry("branding.logoUrl", "https://cdn.kitehub.me/logo.png")
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
    void caller_provided_non_url_key_wins_over_branding() {
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

    // ---- GAP-1040 SSRF guards -------------------------------------------------------------

    @Test
    void caller_supplied_logoUrl_is_stripped_no_egress() {
        // SSRF attempt: caller injects cloud metadata endpoint via the plain logoUrl key.
        Map<String, Object> callerData = new HashMap<>();
        callerData.put("logoUrl", "http://169.254.169.254/latest/meta-data/");
        callerData.put("invoice.number", "INV-001");
        DocumentRequest req = request(callerData);

        DocumentRequest out = assembler.enrich(req, null);

        assertThat(out.data())
                .doesNotContainKey("logoUrl")
                .doesNotContainKey("branding.logoUrl")
                .containsEntry("invoice.number", "INV-001");
    }

    @Test
    void caller_supplied_branding_logoUrl_is_stripped_server_wins() {
        // SSRF attempt: caller overrides the branding logo with an internal host.
        Map<String, Object> callerData = new HashMap<>();
        callerData.put("branding.logoUrl", "http://kite-mailhog:8025/");
        DocumentRequest req = request(callerData);
        BrandingResponse branding = BrandingResponse.builder()
                .logoUrl("https://cdn.kitehub.me/logo.png")
                .build();

        DocumentRequest out = assembler.enrich(req, branding);

        // Server value wins; caller's internal-host URL never reaches the render pipeline.
        assertThat(out.data())
                .containsEntry("branding.logoUrl", "https://cdn.kitehub.me/logo.png");
    }

    @Test
    void caller_supplied_branding_logoUrl_stripped_when_no_server_branding() {
        Map<String, Object> callerData = new HashMap<>();
        callerData.put("branding.logoUrl", "http://kite-mailhog:8025/");
        DocumentRequest req = request(callerData);

        DocumentRequest out = assembler.enrich(req, null);

        assertThat(out.data()).doesNotContainKey("branding.logoUrl");
    }

    @Test
    void caller_supplied_arbitrary_url_key_is_stripped() {
        Map<String, Object> callerData = new HashMap<>();
        callerData.put("faviconUrl", "http://169.254.169.254/");
        callerData.put("backgroundImageUrl", "http://internal-host/secret");
        DocumentRequest req = request(callerData);

        DocumentRequest out = assembler.enrich(req, null);

        assertThat(out.data())
                .doesNotContainKey("faviconUrl")
                .doesNotContainKey("backgroundImageUrl");
    }

    @Test
    void server_logoUrl_outside_allowlist_is_skipped_no_egress() {
        DocumentRequest req = request(Map.of());
        BrandingResponse branding = BrandingResponse.builder()
                .displayName("Kite")
                .logoUrl("https://evil.attacker.example.com/logo.png")
                .build();

        DocumentRequest out = assembler.enrich(req, branding);

        assertThat(out.data())
                .containsEntry("branding.displayName", "Kite")
                .doesNotContainKey("branding.logoUrl");
    }

    @Test
    void server_logoUrl_metadata_ip_is_skipped() {
        DocumentRequest req = request(Map.of());
        BrandingResponse branding = BrandingResponse.builder()
                .logoUrl("http://169.254.169.254/latest/meta-data/")
                .build();

        DocumentRequest out = assembler.enrich(req, branding);

        assertThat(out.data()).doesNotContainKey("branding.logoUrl");
    }

    @Test
    void server_logoUrl_in_allowlist_is_kept() {
        DocumentRequest req = request(Map.of());
        BrandingResponse branding = BrandingResponse.builder()
                .logoUrl("https://assets.kitehub.me/tenant/logo.png")
                .build();

        DocumentRequest out = assembler.enrich(req, branding);

        assertThat(out.data())
                .containsEntry("branding.logoUrl", "https://assets.kitehub.me/tenant/logo.png");
    }

    @Test
    void server_logoUrl_dev_minio_http_is_kept() {
        DocumentRequest req = request(Map.of());
        BrandingResponse branding = BrandingResponse.builder()
                .logoUrl("http://kite-minio:9000/branding/logo.png")
                .build();

        DocumentRequest out = assembler.enrich(req, branding);

        assertThat(out.data())
                .containsEntry("branding.logoUrl", "http://kite-minio:9000/branding/logo.png");
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
