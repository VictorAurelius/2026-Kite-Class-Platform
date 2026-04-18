package com.kitehub.email.client;

import com.kitehub.email.dto.TenantBranding;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BrandingClient that exercise the feature-flag and
 * graceful-degradation paths without starting an HTTP server.
 *
 * <p>Mapping of a real HTTP response payload → TenantBranding is exercised by
 * the end-to-end SESEmailServiceTest (via a mocked BrandingClient) and by
 * integration tests when the full service is up.
 */
class BrandingClientTest {

    @Test
    void fetchBranding_returnsDefaults_whenFeatureFlagDisabled() {
        BrandingClient disabled = new BrandingClient("http://unused", 2, false);

        TenantBranding branding = disabled.fetchBranding(42L, "tenant-a");

        assertThat(branding.getDisplayName()).isEqualTo("KiteClass");
        assertThat(branding.getPrimaryColor()).isEqualTo("#667eea");
    }

    @Test
    void fetchBranding_returnsDefaults_whenInstanceIdNull() {
        BrandingClient client = new BrandingClient("http://unused", 2, true);

        TenantBranding branding = client.fetchBranding(null, null);

        assertThat(branding.getDisplayName()).isEqualTo("KiteClass");
    }

    @Test
    void fetchBranding_returnsDefaults_whenBackendUnreachable() {
        // Port 1 is an unprivileged RFC-reserved port — connect always fails fast.
        BrandingClient client = new BrandingClient("http://127.0.0.1:1", 1, true);

        TenantBranding branding = client.fetchBranding(99L, "tenant-offline");

        assertThat(branding.getDisplayName()).isEqualTo("KiteClass");
        assertThat(branding.getPrimaryColor()).isEqualTo("#667eea");
    }

    @Test
    void defaultBranding_hasAllRequiredFields() {
        TenantBranding d = TenantBranding.defaultBranding();

        assertThat(d.getDisplayName()).isNotBlank();
        assertThat(d.getPrimaryColor()).matches("^#[0-9A-Fa-f]{6}$");
        assertThat(d.getSecondaryColor()).matches("^#[0-9A-Fa-f]{6}$");
        assertThat(d.getAccentColor()).matches("^#[0-9A-Fa-f]{6}$");
    }
}
