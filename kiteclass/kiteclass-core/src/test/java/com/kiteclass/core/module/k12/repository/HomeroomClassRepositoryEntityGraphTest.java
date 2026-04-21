package com.kiteclass.core.module.k12.repository;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.AcademicYearStatus;
import com.kiteclass.core.module.academicyear.repository.AcademicYearRepository;
import com.kiteclass.core.module.k12.entity.HomeroomClass;
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
 * GAP-134 (Wave 9.5) — Verifies {@link HomeroomClassRepository#findByIdWithAcademicYear}
 * prefetches the lazy {@code @ManyToOne academicYear} in a single SELECT.
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
class HomeroomClassRepositoryEntityGraphTest extends IntegrationTestBase {

    @Autowired
    private HomeroomClassRepository repository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findByIdWithAcademicYear_runsSingleSelect_whenAcademicYearAccessed() {
        AcademicYear year = academicYearRepository.save(AcademicYear.builder()
                .name("GAP134-HRC-YEAR")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2027, 6, 30))
                .status(AcademicYearStatus.CURRENT)
                .build());
        HomeroomClass hrc = HomeroomClass.builder()
                .academicYear(year)
                .grade("10")
                .section("A1")
                .capacity(40)
                .currentEnrolled(0)
                .build();
        hrc.setDeleted(false);
        HomeroomClass saved = repository.save(hrc);
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Optional<HomeroomClass> loaded = repository.findByIdWithAcademicYear(saved.getId());
        assertThat(loaded).isPresent();
        assertThat(Hibernate.isInitialized(loaded.get().getAcademicYear()))
                .as("academicYear must be initialised via EntityGraph")
                .isTrue();
        assertThat(loaded.get().getAcademicYear().getName()).isEqualTo("GAP134-HRC-YEAR");

        long selectCount = stats.getPrepareStatementCount();
        assertThat(selectCount)
                .as("findByIdWithAcademicYear must emit a single SELECT — got %d",
                        selectCount)
                .isEqualTo(1L);
    }
}
