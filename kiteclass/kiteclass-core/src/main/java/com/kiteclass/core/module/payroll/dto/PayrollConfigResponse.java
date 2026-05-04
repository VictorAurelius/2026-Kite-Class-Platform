package com.kiteclass.core.module.payroll.dto;

import com.kiteclass.core.module.payroll.entity.PayrollConfig;
import com.kiteclass.core.module.payroll.enums.PayrollType;

import java.math.BigDecimal;

/**
 * Read-only view of {@link PayrollConfig} for admin list endpoints (Phase 1).
 *
 * <p>Phase 2 (GAP-057b) extends with {@code baseSalary}, {@code commissionPercent},
 * {@code gvcnAllowance}, {@code bonuses} once the calc engine consumes them.
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
public record PayrollConfigResponse(
        Long id,
        Long teacherId,
        PayrollType type,
        BigDecimal hourlyRate,
        BigDecimal baseSalary,
        BigDecimal commissionPercent,
        BigDecimal gvcnAllowance
) {
    public static PayrollConfigResponse from(PayrollConfig c) {
        return new PayrollConfigResponse(
                c.getId(),
                c.getTeacherId(),
                c.getType(),
                c.getHourlyRate(),
                c.getBaseSalary(),
                c.getCommissionPercent(),
                c.getGvcnAllowance()
        );
    }
}
