package com.kiteclass.core.module.payroll.service;

import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.payroll.dto.PayrollConfigResponse;
import com.kiteclass.core.module.payroll.dto.PayrollPeriodResponse;
import com.kiteclass.core.module.payroll.entity.PayrollPeriod;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * PayrollService — Phase 1 (GAP-057 Phase 1, Wave 18a Bucket C).
 *
 * <p><b>Phase 1 scope (HOURLY only):</b>
 * <ul>
 *   <li>{@link #calculate(Long, LocalDate, LocalDate)} — given a teacher + period,
 *       sum hours from {@code ClassSession} (read-only across the {@code clazz}
 *       module) and persist a {@link PayrollPeriod} with status DRAFT and
 *       deductions = 0.</li>
 *   <li>{@link #getPeriodById(Long)} / {@link #listPeriods} / {@link #listConfigs}
 *       — admin read-only views.</li>
 * </ul>
 *
 * <p><b>Phase 2 (GAP-057b) deferred items:</b> SALARY/COMMISSION/HYBRID
 * calculation, VN tax (TNCN) progressive deductions, BHXH/BHYT mandatory
 * percentages, payslip PDF (depends on GAP-047), bank export, and admin
 * run/approve UI workflow.
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
public interface PayrollService {

    /**
     * Calculate a payroll period for a teacher.
     *
     * <p>Phase 1: only PayrollType.HOURLY is supported. Other types throw
     * {@link UnsupportedOperationException} naming GAP-057b.
     *
     * <p>Algorithm (HOURLY):
     * <ol>
     *   <li>Validate {@code endDate &gt;= startDate}.</li>
     *   <li>Look up {@code PayrollConfig} for teacher (404 if missing).</li>
     *   <li>Validate type=HOURLY + hourlyRate &gt; 0.</li>
     *   <li>Find all {@code TeacherClass} assignments for teacher.</li>
     *   <li>For each class, sum {@code ClassSession} duration where
     *       {@code sessionDate ∈ [startDate, endDate]} (read-only).</li>
     *   <li>{@code grossAmount = hoursWorked * hourlyRate} HALF_EVEN scale=2.</li>
     *   <li>Persist {@link PayrollPeriod} status=DRAFT, deductions=0,
     *       netAmount=grossAmount.</li>
     * </ol>
     *
     * @param teacherId teacher FK; must have an active {@code PayrollConfig}
     * @param startDate inclusive start (e.g. first day of month)
     * @param endDate   inclusive end (e.g. last day of month); &gt;= startDate
     * @return persisted {@link PayrollPeriod}
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException
     *         if PayrollConfig missing for teacher
     * @throws com.kiteclass.core.common.exception.ValidationException
     *         if endDate &lt; startDate, or HOURLY config has null/non-positive rate
     * @throws UnsupportedOperationException if config type is not HOURLY
     *         (Phase 2 GAP-057b)
     */
    PayrollPeriod calculate(Long teacherId, LocalDate startDate, LocalDate endDate);

    /**
     * Lookup a single period by id (admin view).
     *
     * @param id period PK
     * @return entity (caller maps to DTO)
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if not found
     */
    PayrollPeriod getPeriodById(Long id);

    /**
     * Page of payroll configs (admin list view, read-only).
     *
     * @param pageable pagination + sorting
     * @return page of config DTOs
     */
    PageResponse<PayrollConfigResponse> listConfigs(Pageable pageable);

    /**
     * Page of payroll periods filterable by teacher / date range (admin view).
     *
     * @param teacherId optional teacher FK filter
     * @param startDate optional inclusive start date filter
     * @param endDate   optional inclusive end date filter
     * @param pageable  pagination + sorting
     * @return page of period DTOs
     */
    PageResponse<PayrollPeriodResponse> listPeriods(
            Long teacherId, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
