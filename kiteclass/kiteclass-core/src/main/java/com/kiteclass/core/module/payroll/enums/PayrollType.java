package com.kiteclass.core.module.payroll.enums;

import lombok.Getter;

/**
 * Payroll calculation type for a Teacher.
 *
 * <p>Defines how a teacher's compensation is calculated:
 * <ul>
 *   <li>{@link #SALARY}: Fixed monthly salary (typical for school full-time teachers).
 *   <li>{@link #HOURLY}: Pay per teaching hour (typical for centers / part-time).
 *   <li>{@link #COMMISSION}: Percent of tuition collected for teacher's classes.
 *   <li>{@link #HYBRID}: Base salary + bonuses (commission, GVCN allowance, etc.).
 * </ul>
 *
 * <p><b>Phase 1 scope (GAP-057 Phase 1):</b> Only {@link #HOURLY} is supported by
 * {@code PayrollService.calculate(...)}. The other types are reserved as enum
 * values so {@code PayrollConfig} can be persisted with the intended type, but
 * the calculation engine will throw {@link UnsupportedOperationException} until
 * Phase 2 (GAP-057b) ships SALARY / COMMISSION / HYBRID with VN tax (TNCN) +
 * BHXH / BHYT / payslip PDF / bank export.
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
@Getter
public enum PayrollType {

    /**
     * Fixed monthly salary. Phase 2 (GAP-057b).
     */
    SALARY("Lương cố định", "Fixed monthly salary"),

    /**
     * Pay per teaching hour. Phase 1 supported.
     */
    HOURLY("Trả theo giờ", "Pay per teaching hour"),

    /**
     * Commission as % of tuition collected. Phase 2 (GAP-057b).
     */
    COMMISSION("Hoa hồng theo học phí", "Commission percent of tuition"),

    /**
     * Base + bonuses (GVCN allowance, perfect attendance, etc.). Phase 2 (GAP-057b).
     */
    HYBRID("Lương + thưởng", "Base salary plus bonuses");

    private final String displayNameVi;
    private final String description;

    PayrollType(String displayNameVi, String description) {
        this.displayNameVi = displayNameVi;
        this.description = description;
    }

    /**
     * Whether the Phase 1 PayrollService supports calculating this type.
     *
     * @return true if Phase 1 calculate() implements this type
     */
    public boolean isPhase1Supported() {
        return this == HOURLY;
    }
}
