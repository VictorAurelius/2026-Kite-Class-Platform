package com.kiteclass.core.module.clazz.repository;

import com.kiteclass.core.module.clazz.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ClassSession entity.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    /**
     * Finds a session by ID that is not deleted.
     *
     * @param id session ID
     * @return Optional containing session if found and not deleted
     */
    Optional<ClassSession> findByIdAndDeletedFalse(Long id);

    /**
     * Finds all sessions for a class, ordered by session number.
     *
     * @param classId class ID
     * @return list of sessions in order
     */
    List<ClassSession> findByClassIdAndDeletedFalseOrderBySessionNumberAsc(Long classId);

    /**
     * Counts sessions for a class (not deleted).
     *
     * @param classId class ID
     * @return count of sessions
     */
    long countByClassIdAndDeletedFalse(Long classId);

    /**
     * Checks if a session number already exists in a class.
     *
     * @param classId       class ID
     * @param sessionNumber session number
     * @return true if session number exists
     */
    boolean existsByClassIdAndSessionNumberAndDeletedFalse(Long classId, Integer sessionNumber);

    /**
     * Finds the maximum session number for a class.
     * Used when appending new sessions.
     *
     * @param classId class ID
     * @return maximum session number or 0 if no sessions
     */
    @Query("SELECT COALESCE(MAX(s.sessionNumber), 0) FROM ClassSession s WHERE s.classId = :classId AND s.deleted = false")
    int findMaxSessionNumberByClassId(@Param("classId") Long classId);

    /**
     * Soft deletes all sessions for a class (used when class is deleted).
     * Sets deleted = true for all sessions belonging to classId.
     *
     * @param classId class ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE ClassSession s SET s.deleted = true WHERE s.classId = :classId")
    void softDeleteByClassId(@Param("classId") Long classId);
}
