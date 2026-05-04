package com.kiteclass.core.module.payroll.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.payroll.enums.PayrollType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * PayrollConfig — per-teacher payroll configuration.
 *
 * <p>One row per Teacher per tenant. Drives {@code PayrollService.calculate(...)}.
 *
 * <p><b>Phase 1 (GAP-057 Phase 1):</b> Only {@link PayrollType#HOURLY} is wired
 * to a calculation engine. Fields {@code baseSalary}, {@code commissionPercent},
 * {@code gvcnAllowance}, {@code bonuses} are persisted (so admin UI can carry the
 * config forward when Phase 2 lands) but unused by the Phase 1 calc engine.
 *
 * <p>VN tax / BHXH / BHYT progressive computation deferred to Phase 2 (GAP-057b)
 * per `documents/01-business/kiteclass/payroll/rules.md` BR-PAYROLL-006.
 *
 * <p>Business rules:
 * <ul>
 *   <li>BR-PAYROLL-001: One config per teacher per tenant (uk_payroll_configs_teacher).</li>
 *   <li>BR-PAYROLL-002: When type=HOURLY, hourlyRate must be &gt; 0.</li>
 *   <li>BR-PAYROLL-003: Multi-tenant isolation via instance_id.</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
@Entity
@Table(
        name = "payroll_configs",
        indexes = {
                @Index(name = "idx_payroll_configs_teacher_id", columnList = "teacher_id"),
                @Index(name = "idx_payroll_configs_instance_id", columnList = "instance_id"),
                @Index(name = "idx_payroll_configs_type", columnList = "type")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollConfig extends BaseEntity {

    /**
     * Foreign key to {@code teachers.id}. Required.
     *
     * <p>One PayrollConfig per teacher (BR-PAYROLL-001 — uniqueness enforced
     * via DB unique constraint, not at entity level since soft-delete may keep
     * stale rows).
     */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /**
     * Payroll calculation type. Required.
     * Phase 1: only HOURLY is calculable.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PayrollType type;

    /**
     * Hourly rate in VND. Required when {@link #type} is {@link PayrollType#HOURLY}.
     * Stored as DECIMAL(15,2) for VND precision (HALF_EVEN rounding per
     * BR-PAYROLL-007).
     *
     * <p>Phase 1: this is the only rate field consulted by the calc engine.
     */
    @Column(name = "hourly_rate", precision = 15, scale = 2)
    private BigDecimal hourlyRate;

    /**
     * Base monthly salary in VND.
     *
     * <p><b>Phase 2 GAP-057b:</b> consumed by SALARY / HYBRID calc.
     * Persisted in Phase 1 so admins can prefill config but unused.
     */
    @Column(name = "base_salary", precision = 15, scale = 2)
    private BigDecimal baseSalary;

    /**
     * Commission percent (0-100) of tuition collected for teacher's classes.
     *
     * <p><b>Phase 2 GAP-057b:</b> consumed by COMMISSION / HYBRID calc.
     */
    @Column(name = "commission_percent", precision = 5, scale = 2)
    private BigDecimal commissionPercent;

    /**
     * GVCN (giáo viên chủ nhiệm — homeroom teacher) allowance per pay period in VND.
     *
     * <p><b>Phase 2 GAP-057b:</b> consumed by HYBRID calc + K-12 personas.
     */
    @Column(name = "gvcn_allowance", precision = 15, scale = 2)
    private BigDecimal gvcnAllowance;

    /**
     * Free-form bonuses serialized as JSONB map (key → VND amount).
     *
     * <p><b>Phase 2 GAP-057b:</b> consumed by HYBRID calc. Phase 1 stores as
     * plain TEXT (JSON string) — schema column is JSONB, but the entity does
     * not yet pair {@code @JdbcTypeCode(SqlTypes.JSON)} because Phase 1 does
     * not read/write structured Map. When Phase 2 wires Map<String,BigDecimal>,
     * pair {@code @JdbcTypeCode(SqlTypes.JSON)} per memory
     * {@code feedback_jpa_jsonb_jdbctypecode.md}.
     */
    @Column(name = "bonuses", columnDefinition = "TEXT")
    private String bonusesJson;
}
