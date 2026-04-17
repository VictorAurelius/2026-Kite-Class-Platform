package com.kiteclass.core.module.assignment.repository;

import com.kiteclass.core.common.constant.SubmissionStatus;
import com.kiteclass.core.module.assignment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Submission entity.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /**
     * Find submission by ID (not deleted).
     *
     * @param id submission ID
     * @return submission if found
     */
    Optional<Submission> findByIdAndDeletedFalse(Long id);

    /**
     * Find submission by assignment and student (not deleted).
     *
     * @param assignmentId assignment ID
     * @param studentId student ID
     * @return submission if found
     */
    Optional<Submission> findByAssignmentIdAndStudentIdAndDeletedFalse(Long assignmentId, Long studentId);

    /**
     * Find all submissions for an assignment (not deleted).
     *
     * @param assignmentId assignment ID
     * @return list of submissions
     */
    List<Submission> findByAssignmentIdAndDeletedFalseOrderBySubmissionDateDesc(Long assignmentId);

    /**
     * Find all submissions by student (not deleted).
     *
     * @param studentId student ID
     * @return list of submissions
     */
    List<Submission> findByStudentIdAndDeletedFalseOrderBySubmissionDateDesc(Long studentId);

    /**
     * Find pending grading submissions for an assignment (not deleted).
     *
     * @param assignmentId assignment ID
     * @return list of pending submissions
     */
    List<Submission> findByAssignmentIdAndStatusAndDeletedFalseOrderBySubmissionDateAsc(
            Long assignmentId, SubmissionStatus status);

    /**
     * Find pending grading submissions for a class (for teacher dashboard).
     *
     * @param classId class ID
     * @return list of pending submissions
     */
    @Query("SELECT s FROM Submission s JOIN Assignment a ON s.assignmentId = a.id " +
           "WHERE a.classId = :classId AND s.status = 'PENDING' AND s.deleted = false " +
           "ORDER BY s.submissionDate ASC")
    List<Submission> findPendingGradingByClass(@Param("classId") Long classId);

    /**
     * Count submissions by assignment (not deleted).
     *
     * @param assignmentId assignment ID
     * @return count
     */
    long countByAssignmentIdAndDeletedFalse(Long assignmentId);

    /**
     * Count graded submissions by assignment (not deleted).
     *
     * @param assignmentId assignment ID
     * @return count
     */
    long countByAssignmentIdAndStatusAndDeletedFalse(Long assignmentId, SubmissionStatus status);

    /**
     * Check if student has submitted for assignment.
     *
     * @param assignmentId assignment ID
     * @param studentId student ID
     * @return true if submission exists
     */
    boolean existsByAssignmentIdAndStudentIdAndDeletedFalse(Long assignmentId, Long studentId);
}
