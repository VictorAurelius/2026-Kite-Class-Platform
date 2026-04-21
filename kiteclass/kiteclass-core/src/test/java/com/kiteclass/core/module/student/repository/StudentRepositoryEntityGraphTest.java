package com.kiteclass.core.module.student.repository;

import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.testutil.IntegrationTestBase;
import com.kiteclass.core.testutil.StudentTestDataBuilder;
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
 * GAP-134 (Wave 9.5) — Verifies that {@link StudentRepository#findByIdWithParentLinks}
 * prefetches the {@code parentLinks} lazy collection in a single SELECT.
 *
 * <p>Guarded by {@code ENABLE_INTEGRATION_TESTS=true} to avoid requiring Docker
 * for every local build (Testcontainers + PostgreSQL).
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
class StudentRepositoryEntityGraphTest extends IntegrationTestBase {

    @Autowired
    private StudentRepository studentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findByIdWithParentLinks_runsSingleSelect_whenParentLinksAccessed() {
        // Given — save a student (no parentLinks needed: fetch-join targets the
        // collection; its size is 0 after persist/clear but the SELECT must still
        // be a single statement thanks to the @EntityGraph).
        Student student = StudentTestDataBuilder.createDefaultStudent();
        student.setId(null);
        student.setEmail("gap134-student@example.com");
        Student saved = studentRepository.save(student);
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // When — fetch with EntityGraph and touch the collection
        Optional<Student> loaded = studentRepository.findByIdWithParentLinks(saved.getId());
        assertThat(loaded).isPresent();
        int links = loaded.get().getParentLinks().size();

        long selectCount = stats.getPrepareStatementCount();

        // Then — exactly 1 prepared statement for parent + prefetched collection
        assertThat(links).isGreaterThanOrEqualTo(0);
        assertThat(selectCount)
                .as("findByIdWithParentLinks must emit a single SELECT — got %d",
                        selectCount)
                .isEqualTo(1L);
    }

    @Test
    void findByIdAndDeletedFalse_triggersExtraSelect_whenParentLinksAccessed_demonstratingBaseline() {
        // Baseline documents the N+1 problem this gap solves; not a regression test.
        Student student = StudentTestDataBuilder.createDefaultStudent();
        student.setId(null);
        student.setEmail("gap134-student-baseline@example.com");
        Student saved = studentRepository.save(student);
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Optional<Student> loaded = studentRepository.findByIdAndDeletedFalse(saved.getId());
        assertThat(loaded).isPresent();
        // Trigger lazy init
        loaded.get().getParentLinks().size();

        long selectCount = stats.getPrepareStatementCount();
        assertThat(selectCount)
                .as("baseline: legacy method emits ≥2 statements when touching " +
                        "the lazy parentLinks; got %d", selectCount)
                .isGreaterThanOrEqualTo(2L);
    }
}
