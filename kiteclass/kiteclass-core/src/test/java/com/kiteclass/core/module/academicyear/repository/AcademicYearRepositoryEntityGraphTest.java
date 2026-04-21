package com.kiteclass.core.module.academicyear.repository;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.AcademicYearStatus;
import com.kiteclass.core.module.academicyear.entity.Holiday;
import com.kiteclass.core.module.academicyear.entity.HolidayType;
import com.kiteclass.core.testutil.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-134 (Wave 9.5) — Verifies {@link AcademicYearRepository#findFirstByStatusWithHolidays}
 * prefetches the {@code holidays} lazy collection in a single SELECT, closing the
 * N+1 exposed by {@code AcademicYearService#isHoliday}.
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
class AcademicYearRepositoryEntityGraphTest extends IntegrationTestBase {

    @Autowired
    private AcademicYearRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private AcademicYear buildYear(String name) {
        AcademicYear ay = AcademicYear.builder()
                .name(name)
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2027, 6, 30))
                .status(AcademicYearStatus.CURRENT)
                .build();
        ay.setDeleted(false);
        return ay;
    }

    private Holiday buildHoliday(AcademicYear year, String name, LocalDate start) {
        Holiday h = Holiday.builder()
                .academicYear(year)
                .name(name)
                .startDate(start)
                .endDate(start)
                .type(HolidayType.NATIONAL)
                .build();
        h.setDeleted(false);
        return h;
    }

    @Test
    void findFirstByStatusWithHolidays_runsSingleSelect_whenHolidaysAccessed() {
        AcademicYear year = buildYear("GAP134-AY-2026-2027");
        Holiday h1 = buildHoliday(year, "Tet", LocalDate.of(2027, 2, 17));
        Holiday h2 = buildHoliday(year, "Quoc Khanh", LocalDate.of(2026, 9, 2));
        year.getHolidays().add(h1);
        year.getHolidays().add(h2);
        repository.save(year);
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Optional<AcademicYear> loaded = repository.findFirstByStatusWithHolidays(
                AcademicYearStatus.CURRENT);
        assertThat(loaded).isPresent();
        int holidayCount = loaded.get().getHolidays().size();
        assertThat(holidayCount).isGreaterThanOrEqualTo(2);

        long selectCount = stats.getPrepareStatementCount();
        assertThat(selectCount)
                .as("findFirstByStatusWithHolidays must emit a single SELECT — got %d",
                        selectCount)
                .isEqualTo(1L);
    }

    @Test
    void findFirstByStatusAndDeletedFalse_triggersExtraSelect_demonstratingBaseline() {
        AcademicYear year = buildYear("GAP134-AY-BASELINE");
        year.getHolidays().add(buildHoliday(year, "Tet", LocalDate.of(2027, 2, 17)));
        repository.save(year);
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Optional<AcademicYear> loaded = repository.findFirstByStatusAndDeletedFalse(
                AcademicYearStatus.CURRENT);
        assertThat(loaded).isPresent();
        loaded.get().getHolidays().size();

        long selectCount = stats.getPrepareStatementCount();
        assertThat(selectCount)
                .as("baseline: legacy method emits ≥2 statements when touching " +
                        "the lazy holidays; got %d", selectCount)
                .isGreaterThanOrEqualTo(2L);
    }
}
