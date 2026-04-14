package com.kiteclass.core.module.academicyear.service;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.Holiday;
import com.kiteclass.core.module.academicyear.entity.HolidayType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides VN national holidays for a given academic year.
 *
 * <p>Handles both solar (fixed date) and lunar (calculated) holidays.
 *
 * <p>Strategy Pattern (per design-patterns.md): swap provider for international tenants.
 *
 * @since 3.15.0 (GAP-053)
 */
@Component
public class VnHolidayProvider {

    /**
     * Generate list of VN national holidays falling within the academic year period.
     *
     * <p>Note: Tết Nguyên đán (Lunar New Year) dates vary yearly.
     * For production, integrate với lunar calendar library.
     * For MVP, uses approximate dates (TODO: lunar calc).
     */
    public List<Holiday> generateForAcademicYear(AcademicYear academicYear) {
        List<Holiday> holidays = new ArrayList<>();
        LocalDate start = academicYear.getStartDate();
        LocalDate end = academicYear.getEndDate();

        // Iterate through each year that academic year spans (typically 2 years: Sep-Jun)
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            addIfInRange(holidays, academicYear, year, Month.JANUARY, 1, 1, "Tết Dương lịch");

            // Tết Nguyên đán — approximate (late Jan / early Feb, 7 days)
            // TODO: Use lunar calendar library for accuracy
            LocalDate tetStart = approximateTetStart(year);
            addHolidayIfInRange(holidays, academicYear, tetStart, tetStart.plusDays(6),
                    HolidayType.NATIONAL, "Tết Nguyên đán", "Lunar New Year holiday");

            // Giỗ tổ Hùng Vương — 10/3 Lunar (approx April)
            addIfInRange(holidays, academicYear, year, Month.APRIL, 18, 18, "Giỗ tổ Hùng Vương");

            addIfInRange(holidays, academicYear, year, Month.APRIL, 30, 30, "Ngày Thống nhất");
            addIfInRange(holidays, academicYear, year, Month.MAY, 1, 1, "Quốc tế Lao động");
            addIfInRange(holidays, academicYear, year, Month.SEPTEMBER, 2, 2, "Quốc khánh");
        }

        return holidays;
    }

    private void addIfInRange(List<Holiday> holidays, AcademicYear ay, int year,
                              Month month, int startDay, int endDay, String name) {
        LocalDate start = LocalDate.of(year, month, startDay);
        LocalDate end = LocalDate.of(year, month, endDay);
        addHolidayIfInRange(holidays, ay, start, end, HolidayType.NATIONAL, name, null);
    }

    private void addHolidayIfInRange(List<Holiday> holidays, AcademicYear ay,
                                      LocalDate start, LocalDate end,
                                      HolidayType type, String name, String description) {
        if (end.isBefore(ay.getStartDate()) || start.isAfter(ay.getEndDate())) {
            return;
        }

        holidays.add(Holiday.builder()
                .academicYear(ay)
                .name(name)
                .startDate(start)
                .endDate(end)
                .type(type)
                .description(description)
                .build());
    }

    /**
     * Approximate Tết Nguyên đán start date.
     * TODO: Replace với proper lunar calendar lookup.
     * Reference table for 2026-2030:
     */
    private LocalDate approximateTetStart(int year) {
        return switch (year) {
            case 2026 -> LocalDate.of(2026, 2, 17);
            case 2027 -> LocalDate.of(2027, 2, 6);
            case 2028 -> LocalDate.of(2028, 1, 26);
            case 2029 -> LocalDate.of(2029, 2, 13);
            case 2030 -> LocalDate.of(2030, 2, 3);
            // Fallback: late January (not perfect but in range)
            default -> LocalDate.of(year, Month.FEBRUARY, 1);
        };
    }
}
