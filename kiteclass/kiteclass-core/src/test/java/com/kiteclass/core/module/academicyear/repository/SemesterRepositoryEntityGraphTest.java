package com.kiteclass.core.module.academicyear.repository;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.AcademicYearStatus;
import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.academicyear.entity.SemesterType;
import com.kiteclass.core.testutil.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-134 (Wave 9.5) — Verifies {@link SemesterRepository#findByIdWithAcademicYear}
 * prefetches the lazy {@code @ManyToOne academicYear} in a single SELECT.
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
class SemesterRepositoryEntityGraphTest extends IntegrationTestBase {

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findByIdWithAcademicYear_runsSingleSelect_whenAcademicYearAccessed() {
        AcademicYear year = academicYearRepository.save(AcademicYear.builder()
                .name("GAP134-SEM-YEAR")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2027, 6, 30))
                .status(AcademicYearStatus.CURRENT)
                .build());

        Semester semester = Semester.builder()
                .academicYear(year)
                .type(SemesterType.HK1)
                .name("HK1 2026-2027")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2027, 1, 15))
                .build();
        semester.setDeleted(false);
        Semester saved = semesterRepository.save(semester);
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Optional<Semester> loaded = semesterRepository.findByIdWithAcademicYear(saved.getId());
        assertThat(loaded).isPresent();
        assertThat(Hibernate.isInitialized(loaded.get().getAcademicYear()))
                .as("academicYear must be initialised via EntityGraph")
                .isTrue();

        long selectCount = stats.getPrepareStatementCount();
        assertThat(selectCount)
                .as("findByIdWithAcademicYear must emit a single SELECT — got %d",
                        selectCount)
                .isEqualTo(1L);
    }
}
