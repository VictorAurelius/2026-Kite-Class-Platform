package com.kitehub.subscription.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StubCertProvisioningService} (GAP-1024 Phase 1 stub).
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@DisplayName("StubCertProvisioningService Unit Tests")
class StubCertProvisioningServiceTest {

    private final StubCertProvisioningService service = new StubCertProvisioningService();

    @Test
    @DisplayName("Stub auto-issues a certificate synchronously (Phase 1 — no real cert authority)")
    void requestCertificate_autoIssues() {
        CertProvisioningResult result = service.requestCertificate("lop.skyedu.vn");

        assertThat(result.isIssued()).isTrue();
        assertThat(result.isFailed()).isFalse();
        assertThat(result.status()).isEqualTo(CertProvisioningResult.CertStatus.ISSUED);
        assertThat(result.detail()).isNotBlank();
    }
}
