package com.kiteclass.core.module.payroll.dto;

import com.kiteclass.core.module.payroll.entity.PayrollPeriod;
import com.kiteclass.core.module.payroll.enums.PayrollStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-only view of {@link PayrollPeriod} for admin list / detail endpoints (Phase 1).
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
public record PayrollPeriodResponse(
        Long id,
        Long teacherId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal hoursWorked,
        BigDecimal grossAmount,
        BigDecimal deductions,
        BigDecimal netAmount,
        PayrollStatus status
) {
    public static PayrollPeriodResponse from(PayrollPeriod p) {
        return new PayrollPeriodResponse(
                p.getId(),
                p.getTeacherId(),
                p.getStartDate(),
                p.getEndDate(),
                p.getHoursWorked(),
                p.getGrossAmount(),
                p.getDeductions(),
                p.getNetAmount(),
                p.getStatus()
        );
    }
}
