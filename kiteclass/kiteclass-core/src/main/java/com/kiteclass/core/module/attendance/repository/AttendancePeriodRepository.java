package com.kiteclass.core.module.attendance.repository;

import com.kiteclass.core.module.attendance.entity.AttendancePeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link AttendancePeriod} (Phase 1A read-only).
 *
 * <p>Write operations and idempotent recording deferred to GAP-323b.
 *
 * @since GAP-323 Phase 1A (Wave 18b1)
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
}
