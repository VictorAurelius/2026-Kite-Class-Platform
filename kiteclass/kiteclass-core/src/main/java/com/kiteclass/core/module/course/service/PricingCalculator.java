package com.kiteclass.core.module.course.service;

import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.entity.PricingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calculates invoice amounts based on Course's pricing_model + unit_price (Wave beta-readiness-4 Bucket C / GAP-292).
 *
 * <p>Decouples pricing math from {@code InvoiceService} so that pricing model changes
 * (future PER_SESSION, hybrid models) localize to this Calculator without touching
 * invoice generation orchestration.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-COURSE-PRICING-002: unit_price interpretation depends on pricing_model</li>
 *   <li>BR-COURSE-PRICING-004 (cross-bucket): period boundary changes (via Bucket D reschedule)
 *       trigger recalc — caller is responsible for re-invoking this method with updated periodStart/End</li>
 * </ul>
 *
 * <p>Examples (VN TT Anh ngữ market reference):
 * <pre>
 *   PER_HOUR, unit_price=200000, attendedHours=12.0  → 2.400.000đ
 *   MONTHLY, unit_price=1500000, periodStart..End spans 2 calendar months → 3.000.000đ
 *   COURSE_PACKAGE, unit_price=8000000  → 8.000.000đ (1-shot per enrollment, periodStart..End ignored)
 *   FREE, unit_price=0  → 0đ
 * </pre>
 *
 * @see Course#getPricingModel()
 * @see PricingModel
 */
@Slf4j
@Component
public class PricingCalculator {

    /**
     * Calculates the invoice amount for the given course + period + attended hours.
     *
     * @param course the course (provides pricingModel + unitPrice)
     * @param periodStart inclusive start date of invoice period
     * @param periodEnd inclusive end date of invoice period
     * @param attendedHours total attended session hours within period (used for PER_HOUR only)
     * @return the invoice amount in VND
     */
    public BigDecimal calculate(
            Course course,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal attendedHours
    ) {
        if (course == null) {
            throw new IllegalArgumentException("Course must not be null");
        }
        PricingModel model = course.getPricingModel();
        if (model == null) {
            log.warn("Course {} has null pricing_model; defaulting to PER_HOUR per migration default",
                    course.getId());
            model = PricingModel.PER_HOUR;
        }
        BigDecimal unitPrice = course.getUnitPrice() != null ? course.getUnitPrice() : BigDecimal.ZERO;

        return switch (model) {
            case FREE -> BigDecimal.ZERO;

            case PER_HOUR -> {
                BigDecimal hours = attendedHours != null ? attendedHours : BigDecimal.ZERO;
                yield unitPrice.multiply(hours).setScale(2, RoundingMode.HALF_UP);
            }

            case MONTHLY -> {
                long monthsInPeriod = monthsBetweenInclusive(periodStart, periodEnd);
                yield unitPrice.multiply(BigDecimal.valueOf(monthsInPeriod))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            case COURSE_PACKAGE -> unitPrice.setScale(2, RoundingMode.HALF_UP);
        };
    }

    /**
     * Counts the number of distinct calendar months that overlap [periodStart, periodEnd] inclusive.
     *
     * <p>Examples:
     * <ul>
     *   <li>01/05/2026 - 31/05/2026 → 1 month</li>
     *   <li>15/05/2026 - 15/06/2026 → 2 months (overlaps May + June)</li>
     *   <li>01/01/2026 - 31/03/2026 → 3 months</li>
     * </ul>
     *
     * @return number of months overlapped (>=1 if start<=end)
     */
    private long monthsBetweenInclusive(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        LocalDate startMonth = start.withDayOfMonth(1);
        LocalDate endMonth = end.withDayOfMonth(1);
        return ChronoUnit.MONTHS.between(startMonth, endMonth) + 1;
    }
}
