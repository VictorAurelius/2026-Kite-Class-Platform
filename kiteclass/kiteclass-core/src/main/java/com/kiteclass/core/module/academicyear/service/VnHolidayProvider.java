package com.kiteclass.core.module.academicyear.service;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.Holiday;
import com.kiteclass.core.module.academicyear.entity.HolidayType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides VN national holidays for a given academic year.
 *
 * <p>Solar (fixed): Tết Dương lịch, 30/4, 1/5, 2/9.
 * Lunar: Tết Nguyên đán, Giỗ tổ Hùng Vương, Tết Trung Thu — solar dates loaded from
 * {@code vn-lunar-holidays.csv} (11-year window 2025-2035). For years outside the window,
 * falls back to approximate dates (accuracy ±7-14 days).
 *
 * <p>Strategy Pattern (per design-patterns.md): swap provider for international tenants.
 *
 * @since 3.15.0 (GAP-053)
 * @see "GAP-100 — Lunar Calendar for VN Holidays"
 */
@Component
public class VnHolidayProvider {

    private static final Logger LOG = LoggerFactory.getLogger(VnHolidayProvider.class);
    private static final String CSV_RESOURCE = "data/vn-lunar-holidays.csv";

    private final Map<Integer, LunarHolidayRow> lunarByYear = new HashMap<>();

    public VnHolidayProvider() {
        loadLunarTable();
    }

    @PostConstruct
    public void loadLunarTable() {
        lunarByYear.clear();
        try (var stream = new ClassPathResource(CSV_RESOURCE).getInputStream();
             var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("year,")) {
                    continue;
                }
                String[] cols = line.split(",");
                if (cols.length != 4) {
                    LOG.warn("Skipping malformed CSV row: {}", line);
                    continue;
                }
                int year = Integer.parseInt(cols[0].trim());
                lunarByYear.put(year, new LunarHolidayRow(
                        LocalDate.parse(cols[1].trim()),
                        LocalDate.parse(cols[2].trim()),
                        LocalDate.parse(cols[3].trim())));
            }
            LOG.info("Loaded {} lunar holiday years from {}", lunarByYear.size(), CSV_RESOURCE);
        } catch (Exception e) {
            LOG.error("Failed to load lunar holiday CSV — falling back to approximations", e);
        }
    }

    public List<Holiday> generateForAcademicYear(AcademicYear academicYear) {
        List<Holiday> holidays = new ArrayList<>();
        LocalDate start = academicYear.getStartDate();
        LocalDate end = academicYear.getEndDate();

        for (int year = start.getYear(); year <= end.getYear(); year++) {
            addIfInRange(holidays, academicYear, year, Month.JANUARY, 1, 1, "Tết Dương lịch");

            LocalDate tetStart = lookupTetStart(year);
            addHolidayIfInRange(holidays, academicYear, tetStart, tetStart.plusDays(6),
                    HolidayType.NATIONAL, "Tết Nguyên đán", "Lunar New Year holiday");

            LocalDate giotoonggia = lookupGiotoonggia(year);
            addHolidayIfInRange(holidays, academicYear, giotoonggia, giotoonggia,
                    HolidayType.NATIONAL, "Giỗ tổ Hùng Vương", "10/3 âm lịch");

            addIfInRange(holidays, academicYear, year, Month.APRIL, 30, 30, "Ngày Thống nhất");
            addIfInRange(holidays, academicYear, year, Month.MAY, 1, 1, "Quốc tế Lao động");

            LocalDate trungThu = lookupTrungThu(year);
            addHolidayIfInRange(holidays, academicYear, trungThu, trungThu,
                    HolidayType.NATIONAL, "Tết Trung Thu", "15/8 âm lịch");

            addIfInRange(holidays, academicYear, year, Month.SEPTEMBER, 2, 2, "Quốc khánh");
        }

        return holidays;
    }

    public LocalDate lookupTetStart(int year) {
        LunarHolidayRow row = lunarByYear.get(year);
        if (row != null) {
            return row.tetStart();
        }
        LOG.warn("Year {} outside lunar table range — using approximate Tết date (Feb 1)", year);
        return LocalDate.of(year, Month.FEBRUARY, 1);
    }

    public LocalDate lookupGiotoonggia(int year) {
        LunarHolidayRow row = lunarByYear.get(year);
        if (row != null) {
            return row.giotoonggia();
        }
        LOG.warn("Year {} outside lunar table range — using approximate Giỗ tổ (Apr 15)", year);
        return LocalDate.of(year, Month.APRIL, 15);
    }

    public LocalDate lookupTrungThu(int year) {
        LunarHolidayRow row = lunarByYear.get(year);
        if (row != null) {
            return row.trungThu();
        }
        LOG.warn("Year {} outside lunar table range — using approximate Trung Thu (Sep 20)", year);
        return LocalDate.of(year, Month.SEPTEMBER, 20);
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

    private record LunarHolidayRow(LocalDate tetStart, LocalDate giotoonggia, LocalDate trungThu) {}
}
