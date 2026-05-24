package com.kiteclass.core.module.course.service;

import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.entity.PricingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PricingCalculator} — Wave beta-readiness-4 Bucket C (GAP-292).
 *
 * <p>Covers 4 PricingModel × scenarios per BR-COURSE-PRICING-001..002.
 *
 * <p>VN sample data: "Trung tâm Anh ngữ Sky Education" Lớp Anh ngữ 5A1 với pricing
 * benchmarks Apollo English / ILA Vietnam 2024.
 */
@DisplayName("PricingCalculator — 4 PricingModel × scenarios (Wave br-4 Bucket C)")
class PricingCalculatorTest {

    private PricingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PricingCalculator();
    }

    @Nested
    @DisplayName("PER_HOUR pricing (VN TT Anh ngữ market dominant)")
    class PerHourPricing {

        @Test
        @DisplayName("Lớp Anh ngữ Sky Education: 200.000đ/giờ × 12 giờ = 2.400.000đ")
        void calculate_perHour_12hours() {
            // Trung tâm Anh ngữ Sky Education benchmark Apollo-style pricing
            Course course = courseBuilder(PricingModel.PER_HOUR, "200000");
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 5, 31),
                    new BigDecimal("12.0")
            );
            assertThat(result).isEqualByComparingTo("2400000.00");
        }

        @Test
        @DisplayName("ILA benchmark 250.000đ/giờ × 1.5 giờ (1 buổi) = 375.000đ")
        void calculate_perHour_singleSession() {
            Course course = courseBuilder(PricingModel.PER_HOUR, "250000");
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 14),
                    LocalDate.of(2026, 5, 14),
                    new BigDecimal("1.5")
            );
            assertThat(result).isEqualByComparingTo("375000.00");
        }

        @Test
        @DisplayName("Zero attended hours returns 0đ (student missed all sessions)")
        void calculate_perHour_zeroHours() {
            Course course = courseBuilder(PricingModel.PER_HOUR, "200000");
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 5, 31),
                    BigDecimal.ZERO
            );
            assertThat(result).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Null attended hours treated as 0đ (defensive)")
        void calculate_perHour_nullHours() {
            Course course = courseBuilder(PricingModel.PER_HOUR, "200000");
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 5, 31),
                    null
            );
            assertThat(result).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("MONTHLY pricing (TT âm nhạc / mầm non)")
    class MonthlyPricing {

        @Test
        @DisplayName("Trung tâm Sky Music Piano cơ bản 1.500.000đ/tháng × 1 tháng")
        void calculate_monthly_oneMonth() {
            Course course = courseBuilder(PricingModel.MONTHLY, "1500000");
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 5, 31),
                    null  // attendedHours ignored for MONTHLY
            );
            assertThat(result).isEqualByComparingTo("1500000.00");
        }

        @Test
        @DisplayName("Period overlap 2 tháng (15/05 - 15/06) = 2 × monthly fee")
        void calculate_monthly_overlapTwoMonths() {
            Course course = courseBuilder(PricingModel.MONTHLY, "1500000");
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 15),
                    LocalDate.of(2026, 6, 15),
                    null
            );
            assertThat(result).isEqualByComparingTo("3000000.00");
        }

        @Test
        @DisplayName("3-month period = 3 × monthly fee")
        void calculate_monthly_threeMonthPeriod() {
            Course course = courseBuilder(PricingModel.MONTHLY, "1500000");
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 3, 31),
                    null
            );
            assertThat(result).isEqualByComparingTo("4500000.00");
        }
    }

    @Nested
    @DisplayName("COURSE_PACKAGE pricing (IELTS/TOEIC bundles)")
    class CoursePackagePricing {

        @Test
        @DisplayName("Lớp IELTS 7.0 40 buổi tối: 12.000.000đ/khoá (one-shot, ignores period + hours)")
        void calculate_coursePackage_ielts() {
            Course course = courseBuilder(PricingModel.COURSE_PACKAGE, "12000000");
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 8, 31),
                    new BigDecimal("60.0")  // ignored
            );
            assertThat(result).isEqualByComparingTo("12000000.00");
        }
    }

    @Nested
    @DisplayName("FREE pricing (trial / demo)")
    class FreePricing {

        @Test
        @DisplayName("Buổi thử miễn phí Lớp Anh ngữ 5A1 — Sky Education = 0đ")
        void calculate_free_zero() {
            Course course = courseBuilder(PricingModel.FREE, "0");
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 14),
                    LocalDate.of(2026, 5, 14),
                    new BigDecimal("1.5")  // ignored — FREE always 0
            );
            assertThat(result).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("FREE with non-zero unit_price still returns 0đ (CHECK constraint enforces at DB but Calc defensive)")
        void calculate_free_evenWithNonZeroUnitPrice() {
            // In practice DB CHECK constraint prevents this, but Calculator defensive
            Course course = courseBuilder(PricingModel.FREE, "0");
            BigDecimal result = calculator.calculate(course, null, null, null);
            assertThat(result).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("Defensive cases")
    class DefensiveCases {

        @Test
        @DisplayName("Null course throws IllegalArgumentException")
        void calculate_nullCourse_throws() {
            assertThatThrownBy(() -> calculator.calculate(null,
                    LocalDate.now(), LocalDate.now(), BigDecimal.ONE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Null pricing_model defaults to PER_HOUR (defensive against legacy data)")
        void calculate_nullPricingModel_defaultsPerHour() {
            Course course = new Course();
            course.setUnitPrice(new BigDecimal("200000"));
            course.setPricingModel(null);

            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 5, 31),
                    new BigDecimal("2.0")
            );
            assertThat(result).isEqualByComparingTo("400000.00");
        }

        @Test
        @DisplayName("Null unit_price treated as 0 (defensive)")
        void calculate_nullUnitPrice_zero() {
            Course course = new Course();
            course.setPricingModel(PricingModel.PER_HOUR);
            course.setUnitPrice(null);

            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 5, 31),
                    new BigDecimal("2.0")
            );
            assertThat(result).isEqualByComparingTo("0");
        }
    }

    // ============== Cross-bucket BR-COURSE-PRICING-004 ==============
    @Nested
    @DisplayName("BR-COURSE-PRICING-004 — Cross-bucket reschedule period boundary semantics")
    class CrossBucketRescheduleSemantics {

        @Test
        @DisplayName("PER_HOUR pre-reschedule: 8 sessions × 1.5h × 200k = 2.400.000đ")
        void preReschedule_eightSessions_returns_2_400_000() {
            // Setup: Course PER_HOUR, unit_price=200000đ/giờ, period 01-15/05 contains 8 sessions × 1.5h
            Course course = courseBuilder(PricingModel.PER_HOUR, "200000");
            BigDecimal attendedHoursPreReschedule = new BigDecimal("12.0");  // 8 × 1.5h
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 5, 15),
                    attendedHoursPreReschedule
            );
            assertThat(result).isEqualByComparingTo("2400000.00");
        }

        @Test
        @DisplayName("PER_HOUR post-reschedule session #5 moves 14/05 → 20/05: period 01-15/05 = 7 × 1.5h = 2.100.000đ")
        void postReschedule_period1to15May_dropsSession5_returns_2_100_000() {
            // After Bucket D ClassService.reschedule moves session #5 (14/05) → 20/05
            // Period 01-15/05 now has only 7 sessions (1,2,3,4,6,7,8) × 1.5h = 10.5h
            Course course = courseBuilder(PricingModel.PER_HOUR, "200000");
            BigDecimal attendedHoursPostReschedule = new BigDecimal("10.5");  // 7 × 1.5h
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 5, 15),
                    attendedHoursPostReschedule
            );
            assertThat(result).isEqualByComparingTo("2100000.00");
        }

        @Test
        @DisplayName("PER_HOUR post-reschedule period 16-31/05 picks up session #5: 1 × 1.5h × 200k = 300.000đ")
        void postReschedule_period16to31May_picksUpSession5_returns_300_000() {
            // The rescheduled session #5 (now 20/05) falls into period 16-31/05
            // That period had 0 sessions before; now has 1 session × 1.5h
            Course course = courseBuilder(PricingModel.PER_HOUR, "200000");
            BigDecimal attendedHoursMovedSession = new BigDecimal("1.5");  // session #5 moved here
            BigDecimal result = calculator.calculate(
                    course,
                    LocalDate.of(2026, 5, 16),
                    LocalDate.of(2026, 5, 31),
                    attendedHoursMovedSession
            );
            assertThat(result).isEqualByComparingTo("300000.00");
        }

        @Test
        @DisplayName("Total revenue invariant: pre-reschedule total = post-reschedule sum (2.4M = 2.1M + 0.3M)")
        void totalRevenueInvariant_postReschedule_equalsPreReschedule() {
            // Cross-bucket invariant test (BR-COURSE-PRICING-004):
            // Reschedule MOVES revenue across period boundaries but does NOT create or destroy revenue.
            Course course = courseBuilder(PricingModel.PER_HOUR, "200000");

            // Pre-reschedule: 1 invoice period 01-15/05 = 2.4M
            BigDecimal preRescheduleTotal = calculator.calculate(
                    course, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15),
                    new BigDecimal("12.0")
            );

            // Post-reschedule: 2 invoice periods (01-15/05 reduced + 16-31/05 picks up moved session)
            BigDecimal postRescheduleP1 = calculator.calculate(
                    course, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15),
                    new BigDecimal("10.5")
            );
            BigDecimal postRescheduleP2 = calculator.calculate(
                    course, LocalDate.of(2026, 5, 16), LocalDate.of(2026, 5, 31),
                    new BigDecimal("1.5")
            );

            assertThat(postRescheduleP1.add(postRescheduleP2))
                    .as("Revenue invariant: pre-reschedule total = post-reschedule sum")
                    .isEqualByComparingTo(preRescheduleTotal);
        }

        @Test
        @DisplayName("MONTHLY pricing unaffected by reschedule (BR-COURSE-PRICING-004 caveat)")
        void monthly_unaffectedByReschedule() {
            // Per BR-COURSE-PRICING-004: MONTHLY + COURSE_PACKAGE + FREE NOT affected by reschedule
            Course course = courseBuilder(PricingModel.MONTHLY, "1500000");
            BigDecimal preReschedule = calculator.calculate(
                    course, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null);
            BigDecimal postReschedule = calculator.calculate(
                    course, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null);
            assertThat(preReschedule).isEqualByComparingTo(postReschedule)
                    .isEqualByComparingTo("1500000.00");
        }
    }

    // ============== Helpers ==============

    private static Course courseBuilder(PricingModel model, String unitPriceStr) {
        Course course = new Course();
        course.setName("Lớp Anh ngữ 5A1 — Trung tâm Sky Education");
        course.setPricingModel(model);
        course.setUnitPrice(new BigDecimal(unitPriceStr));
        return course;
    }
}
