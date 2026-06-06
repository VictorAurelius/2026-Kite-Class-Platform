package com.kiteclass.core.module.tenantsettings.util;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Computes the current Vietnamese K-12 academic year (Năm học).
 *
 * <p>VN convention: the school year runs September → May/June. The label is the
 * span of two calendar years, e.g. {@code "2026-2027"}.
 *
 * <p>Boundary rule (per Wave provisioning-1 Bucket F / GAP-947):
 * <ul>
 *   <li>If current month &ge; 9 (Sep-Dec) → {@code "<year>-<year+1>"}</li>
 *   <li>Else (Jan-Aug)                    → {@code "<year-1>-<year>"}</li>
 * </ul>
 *
 * <p>Used to auto-fill {@code TenantSettings.academicYear} at tenant provision so a
 * freshly provisioned trường học already has the correct Năm học without manual setup
 * (benchmark B1 MISA QLTH + recommendation C2 — Năm học required field at provision).
 *
 * @since Wave provisioning-1 (GAP-947)
 */
public final class AcademicYearCalculator {

    /** IANA timezone for Vietnam — default tenant timezone. */
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** First month (inclusive) of a VN academic year — September. */
    private static final int ACADEMIC_YEAR_START_MONTH = 9;

    private AcademicYearCalculator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Computes the current academic year label using today's date in {@code Asia/Ho_Chi_Minh}.
     *
     * @return academic year label, e.g. {@code "2026-2027"}
     */
    public static String currentAcademicYear() {
        return currentAcademicYear(LocalDate.now(VN_ZONE));
    }

    /**
     * Computes the academic year label that the supplied date falls within.
     *
     * @param date the reference date (must not be null)
     * @return academic year label, e.g. {@code "2026-2027"}
     * @throws IllegalArgumentException if date is null
     */
    public static String currentAcademicYear(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Reference date cannot be null");
        }
        int year = date.getYear();
        if (date.getMonthValue() >= ACADEMIC_YEAR_START_MONTH) {
            return year + "-" + (year + 1);
        }
        return (year - 1) + "-" + year;
    }
}
