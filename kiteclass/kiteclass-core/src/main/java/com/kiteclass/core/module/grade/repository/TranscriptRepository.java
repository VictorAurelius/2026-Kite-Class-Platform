package com.kiteclass.core.module.grade.repository;

import com.kiteclass.core.module.grade.entity.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Transcript entity.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Repository
public interface TranscriptRepository extends JpaRepository<Transcript, Long> {

    /**
     * Find transcript by ID (not deleted).
     *
     * @param id transcript ID
     * @return transcript if found
     */
    Optional<Transcript> findByIdAndDeletedFalse(Long id);

    /**
     * Find transcript by student ID and semester (not deleted).
     * Unique constraint enforced by database.
     *
     * @param studentId student ID
     * @param semester semester (e.g., "Spring 2024", "Fall 2023")
     * @return transcript if found
     */
    Optional<Transcript> findByStudentIdAndSemesterAndDeletedFalse(Long studentId, String semester);

    /**
     * Find all transcripts by student ID (not deleted).
     * Used for student's complete academic record.
     *
     * @param studentId student ID
     * @return list of transcripts
     */
    List<Transcript> findByStudentIdAndDeletedFalseOrderBySemesterDesc(Long studentId);

    /**
     * Find all transcripts by semester (not deleted).
     * Used for semester-wide statistics.
     *
     * @param semester semester
     * @return list of transcripts
     */
    List<Transcript> findBySemesterAndDeletedFalseOrderByStudentIdAsc(String semester);

    /**
     * Find latest transcript by student ID (not deleted).
     * Used for displaying current semester GPA.
     *
     * @param studentId student ID
     * @return latest transcript if found
     */
    @Query("SELECT t FROM Transcript t WHERE t.studentId = :studentId " +
           "AND t.deleted = false ORDER BY t.semester DESC LIMIT 1")
    Optional<Transcript> findLatestByStudentId(@Param("studentId") Long studentId);

    /**
     * Calculate cumulative GPA for student (not deleted).
     * Average of all semester GPAs.
     *
     * @param studentId student ID
     * @return cumulative GPA, or null if no transcripts
     */
    @Query("SELECT AVG(t.semesterGpa) FROM Transcript t WHERE t.studentId = :studentId " +
           "AND t.deleted = false")
    Double calculateCumulativeGpaByStudentId(@Param("studentId") Long studentId);

    /**
     * Calculate total credits earned by student (not deleted).
     *
     * @param studentId student ID
     * @return total credits
     */
    @Query("SELECT COALESCE(SUM(t.totalCredits), 0) FROM Transcript t " +
           "WHERE t.studentId = :studentId AND t.deleted = false")
    Integer calculateTotalCreditsByStudentId(@Param("studentId") Long studentId);

    /**
     * Count transcripts by student ID (not deleted).
     *
     * @param studentId student ID
     * @return count
     */
    long countByStudentIdAndDeletedFalse(Long studentId);

    /**
     * Check if student has transcript for semester (not deleted).
     *
     * @param studentId student ID
     * @param semester semester
     * @return true if exists
     */
    boolean existsByStudentIdAndSemesterAndDeletedFalse(Long studentId, String semester);
}
