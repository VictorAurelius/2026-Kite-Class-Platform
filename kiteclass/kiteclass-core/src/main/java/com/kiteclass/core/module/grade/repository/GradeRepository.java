package com.kiteclass.core.module.grade.repository;

import com.kiteclass.core.common.constant.GradeStatus;
import com.kiteclass.core.module.grade.entity.Grade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Grade entity.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    /**
     * Find grade by ID (not deleted).
     *
     * @param id grade ID
     * @return grade if found
     */
    Optional<Grade> findByIdAndDeletedFalse(Long id);

    /**
     * Find grade by ID with {@code components} collection prefetched in a single
     * round-trip — GAP-134 anti-N+1 path for callers that need the breakdown
     * (transcript generation, final-score recompute, grade review UI).
     *
     * <p>Without this, iterating {@code grade.getComponents()} inside
     * {@code GradeServiceImpl.calculateFinalScore(...)} triggers one SELECT per
     * grade row — the classic N+1 pattern called out in the performance audit.
     *
     * @param id grade ID
     * @return grade with components prefetched, if found
     * @since 2.7.3 (GAP-134)
     */
    @EntityGraph(attributePaths = {"components"})
    @Query("SELECT g FROM Grade g WHERE g.id = :id AND g.deleted = false")
    Optional<Grade> findByIdWithComponents(@Param("id") Long id);

    /**
     * Find grade by student ID and class ID (not deleted).
     * Unique constraint enforced by database.
     *
     * @param studentId student ID
     * @param classId class ID
     * @return grade if found
     */
    Optional<Grade> findByStudentIdAndClassIdAndDeletedFalse(Long studentId, Long classId);

    /**
     * Find all grades by student ID (not deleted).
     * Used for transcript generation.
     *
     * @param studentId student ID
     * @return list of grades
     */
    List<Grade> findByStudentIdAndDeletedFalseOrderByCalculatedAtDesc(Long studentId);

    /**
     * Find all grades by class ID (not deleted).
     * Used for class grade report.
     *
     * @param classId class ID
     * @return list of grades
     */
    List<Grade> findByClassIdAndDeletedFalseOrderByFinalScoreDesc(Long classId);

    /**
     * Find all finalized grades by student ID (not deleted).
     * Used for GPA calculations.
     *
     * @param studentId student ID
     * @return list of finalized grades
     */
    @Query("SELECT g FROM Grade g WHERE g.studentId = :studentId " +
           "AND g.status IN ('PASSED', 'FAILED') AND g.deleted = false " +
           "ORDER BY g.finalizedAt DESC")
    List<Grade> findFinalizedGradesByStudentId(@Param("studentId") Long studentId);

    /**
     * Find all grades by status (not deleted).
     * Used for filtering grades by status.
     *
     * @param status grade status
     * @return list of grades
     */
    List<Grade> findByStatusAndDeletedFalseOrderByCalculatedAtDesc(GradeStatus status);

    /**
     * Find all grades by class ID and status (not deleted).
     * Used for class statistics.
     *
     * @param classId class ID
     * @param status grade status
     * @return list of grades
     */
    List<Grade> findByClassIdAndStatusAndDeletedFalse(Long classId, GradeStatus status);

    /**
     * Count grades by class ID (not deleted).
     *
     * @param classId class ID
     * @return count
     */
    long countByClassIdAndDeletedFalse(Long classId);

    /**
     * Count finalized grades by class ID (not deleted).
     *
     * @param classId class ID
     * @return count
     */
    @Query("SELECT COUNT(g) FROM Grade g WHERE g.classId = :classId " +
           "AND g.status IN ('PASSED', 'FAILED') AND g.deleted = false")
    long countFinalizedGradesByClassId(@Param("classId") Long classId);

    /**
     * Calculate average final score by class ID (not deleted).
     *
     * @param classId class ID
     * @return average final score, or null if no grades
     */
    @Query("SELECT AVG(g.finalScore) FROM Grade g WHERE g.classId = :classId " +
           "AND g.finalScore IS NOT NULL AND g.deleted = false")
    Double calculateAverageFinalScoreByClassId(@Param("classId") Long classId);

    /**
     * Check if student has grade in class (not deleted).
     *
     * @param studentId student ID
     * @param classId class ID
     * @return true if exists
     */
    boolean existsByStudentIdAndClassIdAndDeletedFalse(Long studentId, Long classId);
}
