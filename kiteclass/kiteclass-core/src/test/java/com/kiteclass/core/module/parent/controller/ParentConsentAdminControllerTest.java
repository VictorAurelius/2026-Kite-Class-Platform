package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.parent.dto.BulkBumpConsentRequest;
import com.kiteclass.core.module.parent.dto.BulkBumpConsentResponse;
import com.kiteclass.core.module.parent.service.ConsentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-level test for {@link ParentConsentAdminController}.
 *
 * <p>Covers the admin bulk-bump endpoint contract: tenant resolution
 * via {@link TenantContext}, delegation to {@link ConsentService}, and
 * response shape (bumpedCount + newVersion + effectiveAt).
 *
 * <p>RBAC enforcement (PRINCIPAL/ADMIN) is verified by Spring Security
 * configuration outside this test scope; this class focuses on the
 * controller's wiring + body assembly.
 *
 * @since 2.24.0 (Wave 24 — GAP-361 Phase 1C v1.5)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParentConsentAdminController")
class ParentConsentAdminControllerTest {

    @Mock private ConsentService consentService;

    private ParentConsentAdminController controller;

    private static final UUID TENANT_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef0123456789");

    @BeforeEach
    void setUp() {
        controller = new ParentConsentAdminController(consentService);
        TenantContext.setCurrentTenant(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("bulkBump: returns count from ConsentService + new version + effectiveAt")
    void bulkBump_returnsCount() {
        when(consentService.bulkBumpVersion(eq(TENANT_ID), eq(2), eq("Privacy policy v2 — added homework facet")))
                .thenReturn(7);

        BulkBumpConsentRequest body = new BulkBumpConsentRequest(
                2, "Privacy policy v2 — added homework facet", Instant.parse("2026-05-06T00:00:00Z"));

        ResponseEntity<ApiResponse<BulkBumpConsentResponse>> resp = controller.bulkBump(body);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse<BulkBumpConsentResponse> wrapper = resp.getBody();
        assertThat(wrapper).isNotNull();
        BulkBumpConsentResponse data = wrapper.getData();
        assertThat(data.bumpedCount()).isEqualTo(7);
        assertThat(data.newVersion()).isEqualTo(2);
        assertThat(data.effectiveAt()).isNotNull();

        verify(consentService).bulkBumpVersion(TENANT_ID, 2, "Privacy policy v2 — added homework facet");
    }

    @Test
    @DisplayName("bulkBump: idempotent — 0 records updated when all already at target version")
    void bulkBump_idempotent_zeroRows() {
        when(consentService.bulkBumpVersion(eq(TENANT_ID), eq(1), eq("no-op")))
                .thenReturn(0);

        BulkBumpConsentRequest body = new BulkBumpConsentRequest(1, "no-op", Instant.now());

        ResponseEntity<ApiResponse<BulkBumpConsentResponse>> resp = controller.bulkBump(body);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        BulkBumpConsentResponse data = resp.getBody().getData();
        assertThat(data.bumpedCount()).isZero();
        assertThat(data.newVersion()).isEqualTo(1);
    }
}
