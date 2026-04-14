package com.kiteclass.core.module.academicyear;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.AcademicYearStatus;
import com.kiteclass.core.module.academicyear.entity.Holiday;
import com.kiteclass.core.module.academicyear.entity.HolidayType;
import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.academicyear.entity.SemesterType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for domain entity methods.
 */
class AcademicYearEntityTest {

    @Test
    void academic_year_contains_date_within_range() {
        AcademicYear year = AcademicYear.builder()
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 6, 15))
                .build();

        assertThat(year.contains(LocalDate.of(2026, 9, 5))).isTrue();  // boundary start
        assertThat(year.contains(LocalDate.of(2027, 6, 15))).isTrue(); // boundary end
        assertThat(year.contains(LocalDate.of(2027, 1, 1))).isTrue();  // middle
        assertThat(year.contains(LocalDate.of(2026, 9, 4))).isFalse(); // before
        assertThat(year.contains(LocalDate.of(2027, 6, 16))).isFalse();// after
    }

    @Test
    void academic_year_isCurrent_reflects_status() {
        AcademicYear year = AcademicYear.builder().status(AcademicYearStatus.CURRENT).build();
        assertThat(year.isCurrent()).isTrue();

        year.setStatus(AcademicYearStatus.UPCOMING);
        assertThat(year.isCurrent()).isFalse();

        year.setStatus(AcademicYearStatus.COMPLETED);
        assertThat(year.isCurrent()).isFalse();
    }

    @Test
    void semester_contains_date_within_range() {
        Semester semester = Semester.builder()
                .type(SemesterType.HK1)
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 1, 15))
                .build();

        assertThat(semester.contains(LocalDate.of(2026, 11, 1))).isTrue();
        assertThat(semester.contains(LocalDate.of(2026, 9, 5))).isTrue();
        assertThat(semester.contains(LocalDate.of(2027, 1, 15))).isTrue();
        assertThat(semester.contains(LocalDate.of(2027, 2, 1))).isFalse();
        assertThat(semester.contains(LocalDate.of(2026, 8, 1))).isFalse();
    }

    @Test
    void holiday_contains_date_within_range() {
        Holiday holiday = Holiday.builder()
                .name("Tết Nguyên đán")
                .type(HolidayType.NATIONAL)
                .startDate(LocalDate.of(2027, 2, 6))
                .endDate(LocalDate.of(2027, 2, 12))
                .build();

        assertThat(holiday.contains(LocalDate.of(2027, 2, 8))).isTrue();
        assertThat(holiday.contains(LocalDate.of(2027, 2, 6))).isTrue();
        assertThat(holiday.contains(LocalDate.of(2027, 2, 12))).isTrue();
        assertThat(holiday.contains(LocalDate.of(2027, 2, 5))).isFalse();
        assertThat(holiday.contains(LocalDate.of(2027, 2, 13))).isFalse();
    }

    @Test
    void single_day_holiday_contains_only_that_day() {
        Holiday holiday = Holiday.builder()
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2027, 1, 1))
                .type(HolidayType.NATIONAL)
                .build();

        assertThat(holiday.contains(LocalDate.of(2027, 1, 1))).isTrue();
        assertThat(holiday.contains(LocalDate.of(2026, 12, 31))).isFalse();
        assertThat(holiday.contains(LocalDate.of(2027, 1, 2))).isFalse();
    }
}
