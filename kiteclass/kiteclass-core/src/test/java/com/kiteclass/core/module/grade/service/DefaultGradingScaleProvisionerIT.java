package com.kiteclass.core.module.grade.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.grade.entity.GradingScale;
import com.kiteclass.core.module.grade.repository.GradingScaleRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DefaultGradingScaleProvisioner} (GAP-1002).
 *
 * <p>Closes the GAP-1002 residual: verify new-tenant provisioning seeds 8 default grading
 * bands idempotently, and confirm the legacy {@code instance_id IS NULL} default fallback is
 * unreachable by design (so the per-tenant seed is the working path).</p>
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("DefaultGradingScaleProvisioner Integration Tests (GAP-1002)")
class DefaultGradingScaleProvisionerIT {

    @Autowired
    private DefaultGradingScaleProvisioner provisioner;

    @Autowired
    private GradingScaleRepository gradingScaleRepository;

    private final UUID newTenant = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Mirror saga consumer (GAP-1047): TenantContext set to the new tenant so the
        // Hibernate tenantFilter + (prod) RLS admit the seed inserts.
        TenantContext.setCurrentTenant(newTenant);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("New-tenant provisioning seeds 8 default grading bands (A+..F)")
    void seedDefaults_newTenant_seeds8Bands() {
        provisioner.seedDefaults(newTenant);

        List<GradingScale> scales = gradingScaleRepository
                .findByInstanceIdAndDeletedFalseOrderByMinScoreDesc(newTenant);

        assertThat(scales).hasSize(8);
        assertThat(scales).extracting(GradingScale::getLetterGrade)
                .containsExactlyInAnyOrder("A+", "A", "B+", "B", "C+", "C", "D", "F");
        assertThat(scales).allMatch(s -> Boolean.TRUE.equals(s.getIsDefault()));
        assertThat(scales).allMatch(s -> newTenant.equals(s.getInstanceId()));
    }

    @Test
    @DisplayName("Re-running seed is idempotent — no duplicate bands (re-published saga / admin retry)")
    void seedDefaults_rerun_idempotentNoDuplicates() {
        provisioner.seedDefaults(newTenant);
        provisioner.seedDefaults(newTenant); // re-run: must skip (tenant already has scales)

        assertThat(gradingScaleRepository.countByInstanceIdAndDeletedFalse(newTenant)).isEqualTo(8L);
    }

    @Test
    @DisplayName("Legacy NULL-default fallback is unreachable by design — findDefaultGradingScales() empty")
    void findDefaultGradingScales_deadCodeByDesign_returnsEmpty() {
        // GAP-1002 root finding: GradingScale.instance_id is NOT NULL + tenantFilter + RLS,
        // so no `instance_id IS NULL` rows can ever exist → the legacy default fallback returns
        // nothing. The per-tenant provisioner (seedDefaults) is the working path; this asserts
        // the NULL fallback stays dead (Phase 1.5 cleanup tracked in GAP-1002).
        provisioner.seedDefaults(newTenant);

        assertThat(gradingScaleRepository.findDefaultGradingScales()).isEmpty();
    }
}
