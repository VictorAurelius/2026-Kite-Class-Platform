package com.kiteclass.core.module.tenantsettings.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AcademicYearCalculator} — VN K-12 Năm học boundary.
 *
 * @since Wave provisioning-1 (GAP-947)
 */
@DisplayName("AcademicYearCalculator Tests")
class AcademicYearCalculatorTest {

    @Test
    @DisplayName("September → start of new academic year (YYYY-YYYY+1)")
    void september_startsNewYear() {
        assertThat(AcademicYearCalculator.currentAcademicYear(LocalDate.of(2026, 9, 1)))
                .isEqualTo("2026-2027");
    }

    @Test
    @DisplayName("December → same academic year as September (YYYY-YYYY+1)")
    void december_sameAsSeptember() {
        assertThat(AcademicYearCalculator.currentAcademicYear(LocalDate.of(2026, 12, 31)))
                .isEqualTo("2026-2027");
    }

    @Test
    @DisplayName("January → prior-September academic year (YYYY-1-YYYY)")
    void january_priorYearSpan() {
        assertThat(AcademicYearCalculator.currentAcademicYear(LocalDate.of(2027, 1, 15)))
                .isEqualTo("2026-2027");
    }

    @Test
    @DisplayName("May → prior-September academic year (YYYY-1-YYYY)")
    void may_priorYearSpan() {
        assertThat(AcademicYearCalculator.currentAcademicYear(LocalDate.of(2026, 5, 20)))
                .isEqualTo("2025-2026");
    }

    @Test
    @DisplayName("August (boundary just before Sep) → still prior span")
    void august_stillPriorSpan() {
        assertThat(AcademicYearCalculator.currentAcademicYear(LocalDate.of(2026, 8, 31)))
                .isEqualTo("2025-2026");
    }

    @Test
    @DisplayName("Null date → IllegalArgumentException")
    void nullDate_throws() {
        assertThatThrownBy(() -> AcademicYearCalculator.currentAcademicYear(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("No-arg variant returns a well-formed YYYY-YYYY label")
    void noArg_wellFormed() {
        assertThat(AcademicYearCalculator.currentAcademicYear()).matches("\\d{4}-\\d{4}");
    }
}
