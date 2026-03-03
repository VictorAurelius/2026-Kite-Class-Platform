package com.kiteclass.core.module.lms.repository;

import com.kiteclass.core.module.lms.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Lesson entity.
 * Supports finding lessons by module, filtering by trial status, and cross-module queries.
 *
 * @since 2.9.0
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /**
     * Find all lessons for a module, ordered by order_number.
     * Only returns non-deleted lessons.
     *
     * @param moduleId the module ID
     * @return list of lessons ordered by order_number
     */
    List<Lesson> findByModuleIdAndDeletedFalseOrderByOrderNumber(Long moduleId);

    /**
     * Find trial lessons for a module, ordered by order_number.
     * Used for guest access (BR-LMS-001).
     *
     * @param moduleId the module ID
     * @return list of trial lessons ordered by order_number
     */
    List<Lesson> findByModuleIdAndIsTrialTrueAndDeletedFalseOrderByOrderNumber(Long moduleId);

    /**
     * Find a lesson by ID (non-deleted only).
     *
     * @param id the lesson ID
     * @return Optional containing the lesson if found and not deleted
     */
    Optional<Lesson> findByIdAndDeletedFalse(Long id);

    /**
     * Check if a lesson with the given order number already exists for a module.
     * Used to enforce BR-LMS-008: order number must be unique within module.
     *
     * @param moduleId the module ID
     * @param orderNumber the order number to check
     * @return true if a lesson with this order number exists (non-deleted)
     */
    boolean existsByModuleIdAndOrderNumberAndDeletedFalse(Long moduleId, Integer orderNumber);

    /**
     * Check if a lesson with the given order number exists for a module,
     * excluding a specific lesson ID (used for updates).
     *
     * @param moduleId the module ID
     * @param orderNumber the order number to check
     * @param excludeId the lesson ID to exclude from the check
     * @return true if a lesson with this order number exists (excluding the specified ID)
     */
    boolean existsByModuleIdAndOrderNumberAndIdNotAndDeletedFalse(
        Long moduleId, Integer orderNumber, Long excludeId
    );

    /**
     * Find all lessons for a course (across all modules).
     * Used for progress calculation (BR-LMS-004).
     *
     * @param courseId the course ID
     * @return list of all lessons in the course
     */
    @Query("SELECT l FROM Lesson l " +
           "JOIN CourseModule m ON l.moduleId = m.id " +
           "WHERE m.courseId = :courseId " +
           "AND l.deleted = false " +
           "AND m.deleted = false " +
           "ORDER BY m.orderNumber, l.orderNumber")
    List<Lesson> findAllLessonsByCourseId(@Param("courseId") Long courseId);

    /**
     * Count all lessons for a course (across all modules).
     * Used for progress calculation (BR-LMS-004).
     *
     * @param courseId the course ID
     * @return count of lessons in the course
     */
    @Query("SELECT COUNT(l) FROM Lesson l " +
           "JOIN CourseModule m ON l.moduleId = m.id " +
           "WHERE m.courseId = :courseId " +
           "AND l.deleted = false " +
           "AND m.deleted = false")
    long countLessonsByCourseId(@Param("courseId") Long courseId);

    /**
     * Count non-deleted lessons for a module.
     *
     * @param moduleId the module ID
     * @return count of lessons
     */
    long countByModuleIdAndDeletedFalse(Long moduleId);
}
