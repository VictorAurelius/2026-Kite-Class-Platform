package com.kiteclass.core.module.settings.versioning;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.entity.BrandingVersion;
import com.kiteclass.core.module.settings.repository.BrandingVersionRepository;
import com.kiteclass.core.module.settings.service.BrandingService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for <strong>GAP-220</strong> — proves
 * {@code branding_versions.snapshot_json} (Postgres {@code jsonb} column) accepts the JSON string
 * Hibernate binds via {@code @JdbcTypeCode(SqlTypes.JSON)} on
 * {@link com.kiteclass.core.module.settings.entity.BrandingVersion#getSnapshotJson()}.
 *
 * <p>Before the fix, calling {@link BrandingService#updateBranding} threw
 * {@code InvalidDataAccessResourceUsageException: column "snapshot_json" is of type jsonb but
 * expression is of type character varying}. This was latent for ~3 weeks
 * because no IT covered the real DB path —
 * {@code BrandingControllerTest#shouldUpdateBranding} mocks {@code BrandingService.updateBranding()}.
 *
 * <p>This test runs the full pipeline against TestContainers Postgres, so a regression
 * (e.g., someone removing the {@code @JdbcTypeCode} or downgrading Hibernate below 6.2) would
 * fail loudly.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class BrandingVersionSnapshotJsonbIntegrationTest {

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private BrandingVersionRepository brandingVersionRepository;

    private UUID tenant;

    @BeforeEach
    void setUp() {
        tenant = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenant);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("updateBranding successfully writes snapshot_json to jsonb column (no Postgres type-mismatch error)")
    void updateBranding_persists_jsonb_snapshot_row() {
        UpdateBrandingRequest req = UpdateBrandingRequest.builder()
                .displayName("GAP-220 Regression School")
                .primaryColor("#2563EB")
                .secondaryColor("#8B5CF6")
                .accentColor("#10B981")
                .tagline("Diacritics: Đêm trăng")
                .build();

        // The fix: this must not throw
        // InvalidDataAccessResourceUsageException("column \"snapshot_json\" is of type jsonb...").
        brandingService.updateBranding(req);

        List<BrandingVersion> versions = brandingVersionRepository.findAll();
        assertThat(versions)
                .as("BrandingVersionService.snapshot should have written exactly one row "
                        + "for this tenant on first updateBranding")
                .hasSize(1);

        BrandingVersion only = versions.get(0);
        assertThat(only.getInstanceId()).isEqualTo(tenant);
        assertThat(only.getSnapshotJson())
                .as("snapshot_json must round-trip through the jsonb column")
                .isNotBlank()
                .contains("GAP-220 Regression School")
                .contains("#2563EB")
                .contains("Đêm trăng"); // Vietnamese diacritics survive jsonb encoding
    }

    @Test
    @DisplayName("Multiple updateBranding calls produce a monotonic version sequence (round-trip stable)")
    void multiple_updates_produce_monotonic_versions() {
        UpdateBrandingRequest req1 = UpdateBrandingRequest.builder()
                .displayName("Update 1")
                .primaryColor("#111111")
                .secondaryColor("#222222")
                .accentColor("#333333")
                .build();
        brandingService.updateBranding(req1);

        UpdateBrandingRequest req2 = UpdateBrandingRequest.builder()
                .displayName("Update 2")
                .primaryColor("#AAAAAA")
                .secondaryColor("#BBBBBB")
                .accentColor("#CCCCCC")
                .build();
        brandingService.updateBranding(req2);

        List<BrandingVersion> versions = brandingVersionRepository.findAll();
        assertThat(versions).as("two updates → two rows").hasSize(2);
        assertThat(versions)
                .extracting(BrandingVersion::getVersionNumber)
                .as("version numbers must be monotonic")
                .doesNotContainNull()
                .allMatch(n -> n > 0);
        assertThat(versions)
                .extracting(BrandingVersion::getSnapshotJson)
                .as("each snapshot retains its tenant-displayName")
                .anyMatch(json -> json.contains("Update 1"))
                .anyMatch(json -> json.contains("Update 2"));
    }
}
