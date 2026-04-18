package com.kiteclass.core.module.k12.repository;

import com.kiteclass.core.module.k12.entity.ClassScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository cho ClassScheduleSlot (GAP-099 Phase 1).
 *
 * <p>Phase 1 scope: basic CRUD + common lookup methods. Complex queries (conflict detection,
 * weekly aggregation across sections, iCal generation) belong to Phase 2 services.
 *
 * @since GAP-099 Phase 1
 */
@Repository
public interface ClassScheduleSlotRepository extends JpaRepository<ClassScheduleSlot, Long> {

    /**
     * Find all active (non-deleted) slots for a SubjectSection.
     */
    List<ClassScheduleSlot> findBySubjectSectionIdAndDeletedFalse(Long subjectSectionId);

    /**
     * Find slots for a SubjectSection on a specific day of the week.
     */
    List<ClassScheduleSlot> findBySubjectSectionIdAndDayOfWeekAndDeletedFalse(
            Long subjectSectionId, DayOfWeek dayOfWeek);

    /**
     * Find slots effective on a specific date for a SubjectSection.
     * Application-level filter via isActiveOn() still needed nếu cần verify effectiveUntil semantics;
     * DB-level gives pre-filtering by effective_from.
     */
    List<ClassScheduleSlot> findBySubjectSectionIdAndEffectiveFromLessThanEqualAndDeletedFalse(
            Long subjectSectionId, LocalDate date);
}
