package com.kiteclass.core.module.academicyear;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.Holiday;
import com.kiteclass.core.module.academicyear.service.VnHolidayProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for VnHolidayProvider.
 *
 * @since 3.15.0 (GAP-053)
 */
class VnHolidayProviderTest {

    private final VnHolidayProvider provider = new VnHolidayProvider();

    @Test
    void generates_vn_holidays_for_standard_academic_year() {
        AcademicYear year = AcademicYear.builder()
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 6, 15))
                .build();

        List<Holiday> holidays = provider.generateForAcademicYear(year);

        assertThat(holidays).isNotEmpty();
        // Year spans Sep 5, 2026 → Jun 15, 2027
        // Expected holidays in range: 1/1/2027 (Tết DL), Tết NĐ 2027, Giỗ tổ 2027, 30/4/2027, 1/5/2027
        // NOT in range: 2/9/2026 Quốc khánh (before Sep 5 start), 2/9/2027 (after Jun 15 end)
        assertThat(holidays).extracting(Holiday::getName)
                .contains("Tết Dương lịch", "Tết Nguyên đán", "Ngày Thống nhất", "Quốc tế Lao động");
    }

    @Test
    void excludes_holidays_outside_academic_year() {
        AcademicYear shortYear = AcademicYear.builder()
                .name("summer-2026")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .build();

        List<Holiday> holidays = provider.generateForAcademicYear(shortYear);

        // Only 2/9 is within range — but wait, 2/9 is after 8/31, so nothing
        // Actually July-August has no major VN national holidays
        assertThat(holidays).isEmpty();
    }

    @Test
    void tet_2026_is_in_february() {
        AcademicYear year = AcademicYear.builder()
                .name("2025-2026")
                .startDate(LocalDate.of(2025, 9, 5))
                .endDate(LocalDate.of(2026, 6, 15))
                .build();

        List<Holiday> holidays = provider.generateForAcademicYear(year);

        Holiday tet = holidays.stream()
                .filter(h -> h.getName().equals("Tết Nguyên đán"))
                .findFirst()
                .orElseThrow();

        assertThat(tet.getStartDate().getYear()).isEqualTo(2026);
        assertThat(tet.getStartDate().getMonthValue()).isEqualTo(2);
    }

    @Test
    void all_holidays_within_academic_year_range() {
        AcademicYear year = AcademicYear.builder()
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 6, 15))
                .build();

        List<Holiday> holidays = provider.generateForAcademicYear(year);

        for (Holiday h : holidays) {
            assertThat(h.getStartDate())
                    .as("Holiday %s start date", h.getName())
                    .isBetween(year.getStartDate(), year.getEndDate());
        }
    }
}
