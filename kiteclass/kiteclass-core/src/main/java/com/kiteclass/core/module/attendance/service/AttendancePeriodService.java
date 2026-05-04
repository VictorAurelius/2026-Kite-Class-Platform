package com.kiteclass.core.module.attendance.service;

import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for {@link com.kiteclass.core.module.attendance.entity.AttendancePeriod}
 * (Phase 1A read-only).
 *
 * <p>Write API + idempotent recording deferred to GAP-323b. Daily
 * aggregation view + GradeFormulaService deferred to GAP-323c.
 *
 * @since GAP-323 Phase 1A (Wave 18b1)
 */
public interface AttendancePeriodService {

    /**
     * Find a single period-attendance record by its primary key.
     *
     * @param id primary key
     * @return populated response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException
     *         if no row matches
     */
    AttendancePeriodResponse findById(Long id);

    /**
     * Page through period-attendance for a single student in a date range.
     *
     * <p>Used by parent portal feeds and student attendance history views.
     */
    Page<AttendancePeriodResponse> findByStudent(
            Long studentId,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    /**
     * Return the daily roster (all periods + all students) for a class on a
     * given date. Phase 1A returns the raw list; daily roll-up
     * (vắng cả ngày = vắng ≥7 tiết) is deferred to GAP-323b.
     */
    List<AttendancePeriodResponse> findByClassAndDate(Long classId, LocalDate date);

    /**
     * Page through period-attendance for a single SubjectSection across a
     * date range. Used by bộ môn (subject teacher) review UI.
     */
    Page<AttendancePeriodResponse> findBySubjectSection(
            Long subjectSectionId,
            LocalDate from,
            LocalDate to,
            Pageable pageable);
}
