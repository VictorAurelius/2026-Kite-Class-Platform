package com.kiteclass.core.module.parent.repository;

import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;
import com.kiteclass.core.module.childprotection.enums.IncidentStatus;
import com.kiteclass.core.module.childprotection.enums.IncidentVisibilityScope;
import com.kiteclass.core.module.childprotection.repository.IncidentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-321b-1-conduct (Wave 19 Bucket D) — verifies that
 * {@link IncidentRepository#findVisibleForParentList} loads visible
 * incidents for a parent caller with ≤3 prepared statements per
 * facet call, and crucially that {@code STAFF_ONLY} rows in the
 * fixture NEVER appear in the result.
 *
 * <p>Why ≤3? The {@code Incident} entity has no JPA collection
 * associations; the encrypted {@code description} + {@code evidencePaths}
 * fields are decrypted via {@code @Convert} attribute converters during
 * read regardless of fetch mode. So the JPQL list query plan is a single
 * SELECT — one row per visible incident. A conservative ceiling of 3
 * leaves headroom for any Hibernate internal book-keeping (e.g. enum
 * resolution, BaseEntity metadata) without licensing N+1.
 *
 * <p>Mirrors {@code ParentFeesFacetEntityGraphIT} verbatim for the
 * fixture-and-assertion shape; specialized for the visibility filter so
 * the regression contract on STAFF_ONLY exclusion is end-to-end (not just
 * unit-mocked).
 *
 * <p>Guarded by {@code ENABLE_INTEGRATION_TESTS=true} per the existing
 * project convention.
 *
 * @since 2.19.0 (Wave 19 Bucket D — GAP-321b-1-conduct)
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("ParentConductFacet — N+1 protection + STAFF_ONLY exclusion (BR-CHILD-PROTECT-005)")
class ParentConductFacetEntityGraphIT {

    @Autowired private IncidentRepository incidentRepository;
    @PersistenceContext private EntityManager entityManager;

    private static final Long CHILD_ID = 999_002L;

    @Test
    @DisplayName("findVisibleForParentList runs ≤3 prepared statements AND filters STAFF_ONLY rows")
    void visibleQueryDoesNotTriggerN1AndExcludesStaffOnly() {
        UUID tenant = UUID.randomUUID();

        // Given: 3 incidents on the same child:
        //   - 1 PARENT_VISIBLE (LOW)   → must appear
        //   - 1 PUBLIC          (MEDIUM) → must appear
        //   - 1 STAFF_ONLY      (HIGH)   → must NEVER appear
        save(tenant, CHILD_ID, IncidentSeverity.LOW,
                IncidentVisibilityScope.PARENT_VISIBLE, "Tham gia tốt hoạt động lớp");
        save(tenant, CHILD_ID, IncidentSeverity.MEDIUM,
                IncidentVisibilityScope.PUBLIC, "Đề xuất khen ngợi cuối tuần");
        save(tenant, CHILD_ID, IncidentSeverity.HIGH,
                IncidentVisibilityScope.STAFF_ONLY,
                "STAFF-CONFIDENTIAL — must NEVER reach parent surface");

        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // When: query parent-facet method with the parent-allowed scope set.
        List<Incident> visible = incidentRepository.findVisibleForParentList(
                CHILD_ID,
                EnumSet.of(IncidentVisibilityScope.PARENT_VISIBLE,
                        IncidentVisibilityScope.PUBLIC));

        long selectCount = stats.getPrepareStatementCount();

        // Then: STAFF_ONLY excluded; both PARENT_VISIBLE + PUBLIC present.
        assertThat(visible).hasSize(2);
        assertThat(visible)
                .as("STAFF_ONLY incidents MUST be excluded from parent-portal visible set "
                        + "(BR-CHILD-PROTECT-005)")
                .extracting(Incident::getVisibilityScope)
                .containsExactlyInAnyOrder(
                        IncidentVisibilityScope.PARENT_VISIBLE,
                        IncidentVisibilityScope.PUBLIC);
        assertThat(visible)
                .extracting(Incident::getTitle)
                .doesNotContain("STAFF-CONFIDENTIAL — must NEVER reach parent surface");

        // And: no N+1. ≤3 prepared statements per facet call (1 SELECT +
        // ≤2 Hibernate internal coalesce / metadata).
        assertThat(selectCount)
                .as("findVisibleForParentList must not trigger N+1 — "
                        + "expected ≤3 prepared statements, got %d", selectCount)
                .isLessThanOrEqualTo(3L);
    }

    private void save(UUID tenant, Long studentId, IncidentSeverity severity,
                      IncidentVisibilityScope scope, String title) {
        Incident i = Incident.builder()
                .title(title)
                .description("Encrypted narrative — decryption transparent at read time")
                .severity(severity)
                .category(IncidentCategory.BULLYING)
                .status(IncidentStatus.RESOLVED)
                .reporterUserId(1L)
                .subjectStudentId(studentId)
                .visibilityScope(scope)
                .build();
        i.setInstanceId(tenant);
        incidentRepository.save(i);
    }
}
