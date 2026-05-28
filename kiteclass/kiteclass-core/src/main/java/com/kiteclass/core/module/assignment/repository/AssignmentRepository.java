package com.kiteclass.core.module.assignment.repository;

import com.kiteclass.core.common.constant.AssignmentStatus;
import com.kiteclass.core.module.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Assignment entity.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * Find assignment by ID (not deleted).
     *
     * @param id assignment ID
     * @return assignment if found
     */
    Optional<Assignment> findByIdAndDeletedFalse(Long id);

    /**
     * Find all assignments by class ID (not deleted).
     *
     * @param classId class ID
     * @return list of assignments
     */
    List<Assignment> findByClassIdAndDeletedFalseOrderByDueDateDesc(Long classId);

    /**
     * Find published assignments by class ID (not deleted).
     *
     * @param classId class ID
     * @return list of published assignments
     */
    List<Assignment> findByClassIdAndStatusAndDeletedFalseOrderByDueDateDesc(
            Long classId, AssignmentStatus status);

    /**
     * Find assignments created by an actor (not deleted).
     *
     * @param createdBy actor UUID (X-User-Id, matches BaseEntity.createdBy after GAP-795 Long→UUID migration)
     * @return list of assignments
     */
    List<Assignment> findByCreatedByAndDeletedFalseOrderByCreatedAtDesc(UUID createdBy);

    /**
     * Find assignments due between dates (for notifications).
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of assignments
     */
    @Query("SELECT a FROM Assignment a WHERE a.dueDate BETWEEN :startDate AND :endDate " +
           "AND a.status = 'PUBLISHED' AND a.deleted = false")
    List<Assignment> findDueBetween(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find overdue assignments (for late penalty reminders).
     *
     * @return list of overdue assignments
     */
    @Query("SELECT a FROM Assignment a WHERE a.dueDate < :now " +
           "AND a.status = 'PUBLISHED' AND a.allowLateSubmission = true AND a.deleted = false")
    List<Assignment> findOverdueAssignments(@Param("now") LocalDateTime now);

    /**
     * Count assignments by class ID (not deleted).
     *
     * @param classId class ID
     * @return count
     */
    long countByClassIdAndDeletedFalse(Long classId);
}
