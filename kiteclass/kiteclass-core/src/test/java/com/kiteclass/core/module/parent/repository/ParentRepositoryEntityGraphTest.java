package com.kiteclass.core.module.parent.repository;

import com.kiteclass.core.common.constant.ParentRelationship;
import com.kiteclass.core.common.constant.ParentStatus;
import com.kiteclass.core.module.parent.entity.Parent;
import com.kiteclass.core.testutil.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-134 (Wave 9.5) — Verifies that {@link ParentRepository#findByIdWithStudentLinks}
 * prefetches the {@code studentLinks} lazy collection in a single SELECT.
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
class ParentRepositoryEntityGraphTest extends IntegrationTestBase {

    @Autowired
    private ParentRepository parentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Parent buildParent(String email) {
        Parent p = Parent.builder()
                .email(email)
                .fullName("Nguyen Phu Huynh")
                .phoneNumber("0901234567")
                .relationship(ParentRelationship.GUARDIAN)
                .status(ParentStatus.PENDING)
                .build();
        p.setDeleted(false);
        return p;
    }

    @Test
    void findByIdWithStudentLinks_runsSingleSelect_whenStudentLinksAccessed() {
        Parent saved = parentRepository.save(buildParent("gap134-parent@example.com"));
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Optional<Parent> loaded = parentRepository.findByIdWithStudentLinks(saved.getId());
        assertThat(loaded).isPresent();
        int links = loaded.get().getStudentLinks().size();
        assertThat(links).isGreaterThanOrEqualTo(0);

        long selectCount = stats.getPrepareStatementCount();
        assertThat(selectCount)
                .as("findByIdWithStudentLinks must emit a single SELECT — got %d",
                        selectCount)
                .isEqualTo(1L);
    }

    @Test
    void findByIdAndDeletedFalse_triggersExtraSelect_whenStudentLinksAccessed_demonstratingBaseline() {
        Parent saved = parentRepository.save(buildParent("gap134-parent-baseline@example.com"));
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Optional<Parent> loaded = parentRepository.findByIdAndDeletedFalse(saved.getId());
        assertThat(loaded).isPresent();
        loaded.get().getStudentLinks().size();

        long selectCount = stats.getPrepareStatementCount();
        assertThat(selectCount)
                .as("baseline: legacy method emits ≥2 statements when touching " +
                        "the lazy studentLinks; got %d", selectCount)
                .isGreaterThanOrEqualTo(2L);
    }
}
