package com.kiteclass.core.module.attendance.service;

import com.kiteclass.core.module.attendance.dto.AttendancePeriodBatchCreateRequest;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodUpdateRequest;
import com.kiteclass.core.module.attendance.dto.ClassBatchAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.DailyAttendanceRollupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for {@link com.kiteclass.core.module.attendance.entity.AttendancePeriod}.
 *
 * <p>Phase 1A (read-only) shipped Wave 18b1 (GAP-323). Phase 1B (write API +
 * upsert idempotency + daily roll-up) ships Wave 18b2 (GAP-323b). Mobile UI,
 * offline queue, and GradeFormulaService remain deferred per the gap.
 *
 * @since GAP-323 Phase 1A (Wave 18b1); write API GAP-323b (Wave 18b2)
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

    /**
     * Idempotent upsert of a batch of period-attendance records.
     *
     * <p>For each entry, looks up the existing row by the (student, subject
     * section, date, period_no) tuple — the unique index from V50 — and either
     * updates it (status / notes / recorded_by / recorded_at) or inserts a new
     * one. Resubmitting the same batch therefore yields the same final state
     * without producing duplicates.
     *
     * @param request batch (≥1, ≤60 entries)
     * @param recordedBy teacher / GVCN reference id, derived by the controller from the
     *        authenticated principal ({@code X-User-Reference-Id}), NOT a client header (GAP-1300)
     * @return the upserted rows, in the same order as {@code request.entries}
     */
    List<AttendancePeriodResponse> upsertBatch(
            AttendancePeriodBatchCreateRequest request, Long recordedBy);

    /**
     * Class-overview convenience wrapper for {@link #upsertBatch}.
     *
     * <p>Folds {@code classId} and {@code date} from the request envelope into
     * each per-cell entry (per {@link ClassBatchAttendanceRequest}) and forwards
     * to the existing idempotent upsert path. The teacher-overview save UI
     * (route {@code (teacher)/teacher/attendance/[classId]}, GAP-268a) calls
     * this in a single round-trip across all 1-10 periods.
     *
     * @param classId target class (URL path)
     * @param date lesson date (URL query)
     * @param request body containing per-cell entries
     * @param recordedBy teacher / GVCN reference id, derived from the authenticated principal
     *        ({@code X-User-Reference-Id}), NOT a client header (GAP-1300)
     * @return upserted rows in entry order
     * @since GAP-268a (Wave 51 Bucket B)
     */
    List<AttendancePeriodResponse> upsertClassBatch(
            Long classId,
            LocalDate date,
            ClassBatchAttendanceRequest request,
            Long recordedBy);

    /**
     * Update status / notes on a single period-attendance row.
     *
     * <p>Optimistic locking via {@code @Version}: the request must carry the
     * version the client read; a stale value triggers
     * {@link org.springframework.dao.OptimisticLockingFailureException} which
     * the global exception handler maps to HTTP 409.
     *
     * @param id row primary key
     * @param request status (required), notes (optional), version (required)
     * @param recordedBy teacher / GVCN user ID rewriting the row
     */
    AttendancePeriodResponse update(
            Long id, AttendancePeriodUpdateRequest request, Long recordedBy);

    /**
     * Daily roll-up across one class for a date range — per-day counts
     * (period_count / present / absent / late / excused) plus the boolean
     * {@code allDayAbsent} (TT 22/2021 threshold: absent ≥ 7 tiết).
     *
     * <p>Phase 1B v1 implements this via on-demand aggregation. A
     * materialized-view + debounced refresh trigger is documented in
     * GAP-323b §1B.4 but deferred to a follow-up PR; the on-demand version
     * is correctness-equivalent and unblocks the GVCN dashboard surface.
     */
    List<DailyAttendanceRollupResponse> dailyRollupForClass(
            Long classId, LocalDate from, LocalDate to);
}
