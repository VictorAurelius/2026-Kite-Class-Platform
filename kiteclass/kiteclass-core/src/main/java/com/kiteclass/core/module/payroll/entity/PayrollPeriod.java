package com.kiteclass.core.module.payroll.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.payroll.enums.PayrollStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PayrollPeriod — one row per teacher per pay period (typically monthly).
 *
 * <p>Created by {@code PayrollService.calculate(...)} in Phase 1 with
 * {@link PayrollStatus#DRAFT} status and zero deductions. Phase 2 (GAP-057b)
 * adds APPROVED / PAID transitions, VN tax (TNCN) progressive deductions, BHXH
 * + BHYT mandatory deductions, payslip PDF generation (depends on GAP-047),
 * and bank export.
 *
 * <p>Business rules:
 * <ul>
 *   <li>BR-PAYROLL-004: {@code endDate &gt;= startDate} (DB CHECK constraint).</li>
 *   <li>BR-PAYROLL-005: For HOURLY: {@code grossAmount = hoursWorked * hourlyRate}
 *       with HALF_EVEN rounding to scale 2 (BR-PAYROLL-007).</li>
 *   <li>BR-PAYROLL-006: Phase 1 sets deductions = 0 → netAmount = grossAmount.
 *       Phase 2 ships TNCN + BHXH/BHYT.</li>
 *   <li>BR-PAYROLL-008: Multi-tenant isolation via instance_id.</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
@Entity
@Table(
        name = "payroll_periods",
        indexes = {
                @Index(name = "idx_payroll_periods_teacher_id", columnList = "teacher_id"),
                @Index(name = "idx_payroll_periods_instance_id", columnList = "instance_id"),
                @Index(name = "idx_payroll_periods_dates", columnList = "start_date, end_date"),
                @Index(name = "idx_payroll_periods_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPeriod extends BaseEntity {

    /**
     * Foreign key to {@code teachers.id}. Required.
     */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /**
     * Inclusive start date of the pay period. Required.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Inclusive end date of the pay period. Required.
     * Must be &gt;= startDate (BR-PAYROLL-004; enforced by DB CHECK constraint).
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Total hours worked in the period. Required for HOURLY type.
     * Stored as DECIMAL(7,2) — supports fractional hours (e.g. 1.5h sessions).
     *
     * <p>Phase 1: derived by summing {@code ClassSession} duration in [startDate,
     * endDate] for sessions where the teacher is assigned (read from clazz module).
     * Phase 2 ships the persisted "hours_worked" attribution refinement.
     */
    @Column(name = "hours_worked", precision = 7, scale = 2)
    private BigDecimal hoursWorked;

    /**
     * Gross compensation in VND before deductions.
     * Phase 1 HOURLY: {@code hoursWorked * hourlyRate} HALF_EVEN scale=2.
     */
    @Column(name = "gross_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal grossAmount;

    /**
     * Total deductions in VND (TNCN + BHXH + BHYT in Phase 2).
     * Phase 1: always 0 per BR-PAYROLL-006.
     */
    @Column(name = "deductions", precision = 15, scale = 2, nullable = false)
    private BigDecimal deductions;

    /**
     * Net compensation in VND. {@code netAmount = grossAmount - deductions}.
     * Phase 1: equals grossAmount (deductions=0).
     */
    @Column(name = "net_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal netAmount;

    /**
     * Lifecycle status. Phase 1 always creates {@link PayrollStatus#DRAFT}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PayrollStatus status = PayrollStatus.DRAFT;
}
