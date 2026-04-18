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

        assertThat(holidays).isEmpty();
    }

    @Test
    void tet_2026_loaded_from_csv_is_feb_17() {
        LocalDate tet = provider.lookupTetStart(2026);
        assertThat(tet).isEqualTo(LocalDate.of(2026, 2, 17));
    }

    @Test
    void tet_2027_loaded_from_csv_is_feb_6() {
        LocalDate tet = provider.lookupTetStart(2027);
        assertThat(tet).isEqualTo(LocalDate.of(2027, 2, 6));
    }

    @Test
    void giotoonggia_2026_loaded_from_csv_is_apr_26() {
        LocalDate giotoonggia = provider.lookupGiotoonggia(2026);
        assertThat(giotoonggia).isEqualTo(LocalDate.of(2026, 4, 26));
    }

    @Test
    void trung_thu_2026_loaded_from_csv_is_sep_25() {
        LocalDate trungThu = provider.lookupTrungThu(2026);
        assertThat(trungThu).isEqualTo(LocalDate.of(2026, 9, 25));
    }

    @Test
    void out_of_range_year_uses_approximate_fallback() {
        LocalDate tet2050 = provider.lookupTetStart(2050);
        assertThat(tet2050).isEqualTo(LocalDate.of(2050, 2, 1));

        LocalDate giotoonggia2050 = provider.lookupGiotoonggia(2050);
        assertThat(giotoonggia2050).isEqualTo(LocalDate.of(2050, 4, 15));

        LocalDate trungThu2050 = provider.lookupTrungThu(2050);
        assertThat(trungThu2050).isEqualTo(LocalDate.of(2050, 9, 20));
    }

    @Test
    void trung_thu_included_when_falls_in_academic_year() {
        AcademicYear year = AcademicYear.builder()
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 6, 15))
                .build();

        List<Holiday> holidays = provider.generateForAcademicYear(year);

        Holiday trungThu = holidays.stream()
                .filter(h -> h.getName().equals("Tết Trung Thu"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Trung Thu missing from holidays"));

        assertThat(trungThu.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 25));
    }

    @Test
    void giotoonggia_included_and_uses_csv_date_not_hardcoded_apr_18() {
        AcademicYear year = AcademicYear.builder()
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 6, 15))
                .build();

        List<Holiday> holidays = provider.generateForAcademicYear(year);

        Holiday giotoonggia = holidays.stream()
                .filter(h -> h.getName().equals("Giỗ tổ Hùng Vương"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Giỗ tổ missing"));

        assertThat(giotoonggia.getStartDate()).isEqualTo(LocalDate.of(2027, 4, 16));
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
