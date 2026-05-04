package com.kiteclass.core.module.attendance.repository;

import com.kiteclass.core.module.attendance.entity.AttendancePeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link AttendancePeriod}.
 *
 * <p>Phase 1A (read-only) shipped Wave 18b1. Phase 1B adds the upsert-lookup
 * by the V50 unique-index tuple plus the daily roll-up aggregation query.
 *
 * @since GAP-323 Phase 1A (Wave 18b1); Phase 1B GAP-323b (Wave 18b2)
 */
@Repository
public interface AttendancePeriodRepository extends JpaRepository<AttendancePeriod, Long> {

    Optional<AttendancePeriod> findByIdAndDeletedFalse(Long id);

    Page<AttendancePeriod> findByStudentIdAndDateBetweenAndDeletedFalse(
            Long studentId,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    List<AttendancePeriod> findByClassIdAndDateAndDeletedFalse(Long classId, LocalDate date);

    Page<AttendancePeriod> findBySubjectSectionIdAndDateBetweenAndDeletedFalse(
            Long subjectSectionId,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    /**
     * Idempotency lookup matching the unique index
     * {@code uk_att_period_student_section_date_period} from V50. The tenant
     * filter is applied by Hibernate, so no {@code instance_id} clause is
     * needed at the JPQL level.
     */
    Optional<AttendancePeriod>
            findByStudentIdAndSubjectSectionIdAndDateAndPeriodNoAndDeletedFalse(
                    Long studentId,
                    Long subjectSectionId,
                    LocalDate date,
                    Integer periodNo);

    /**
     * Per-(student, date) roll-up across a date range for a single class.
     *
     * <p>Returns rows shaped as
     * {@code [studentId, classId, date, periodCount, presentCount, absentCount,
     * lateCount, excusedCount, makeupCount]}.
     */
    @Query("""
            SELECT ap.studentId,
                   ap.classId,
                   ap.date,
                   COUNT(ap),
                   SUM(CASE WHEN ap.status = com.kiteclass.core.common.constant.AttendanceStatus.PRESENT THEN 1L ELSE 0L END),
                   SUM(CASE WHEN ap.status = com.kiteclass.core.common.constant.AttendanceStatus.ABSENT THEN 1L ELSE 0L END),
                   SUM(CASE WHEN ap.status = com.kiteclass.core.common.constant.AttendanceStatus.LATE THEN 1L ELSE 0L END),
                   SUM(CASE WHEN ap.status = com.kiteclass.core.common.constant.AttendanceStatus.EXCUSED THEN 1L ELSE 0L END),
                   SUM(CASE WHEN ap.status = com.kiteclass.core.common.constant.AttendanceStatus.MAKEUP THEN 1L ELSE 0L END)
              FROM AttendancePeriod ap
             WHERE ap.classId = :classId
               AND ap.date BETWEEN :from AND :to
               AND ap.deleted = false
             GROUP BY ap.studentId, ap.classId, ap.date
             ORDER BY ap.date ASC, ap.studentId ASC
            """)
    List<Object[]> aggregateDailyRollupForClass(
            @Param("classId") Long classId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
