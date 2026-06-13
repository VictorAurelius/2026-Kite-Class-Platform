package com.kiteclass.core.module.lms.repository;

import com.kiteclass.core.module.lms.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LessonProgress entity.
 * Supports progress tracking queries for students.
 *
 * @since 2.9.0
 */
@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    /**
     * Find progress record for a user and lesson.
     * Used to check if a lesson has been completed (BR-LMS-009).
     *
     * @param userId the user ID
     * @param lessonId the lesson ID
     * @return Optional containing the progress record if found
     */
    Optional<LessonProgress> findByUserIdAndLessonIdAndDeletedFalse(Long userId, Long lessonId);

    /**
     * Find all progress records for a user across a course.
     * Used for course progress calculation (BR-LMS-004).
     *
     * @param courseId the course ID
     * @param userId the user ID
     * @return list of progress records
     */
    @Query("SELECT lp FROM LessonProgress lp " +
           "JOIN Lesson l ON lp.lessonId = l.id " +
           "JOIN CourseModule m ON l.moduleId = m.id " +
           "WHERE m.courseId = :courseId " +
           "AND lp.userId = :userId " +
           "AND lp.deleted = false " +
           "AND l.deleted = false " +
           "AND m.deleted = false")
    List<LessonProgress> findProgressByCourseIdAndUserId(
        @Param("courseId") Long courseId,
        @Param("userId") Long userId
    );

    /**
     * Count completed lessons for a user in a course.
     * Used for course progress calculation (BR-LMS-004).
     *
     * @param courseId the course ID
     * @param userId the user ID
     * @return count of completed lessons
     */
    @Query("SELECT COUNT(lp) FROM LessonProgress lp " +
           "JOIN Lesson l ON lp.lessonId = l.id " +
           "JOIN CourseModule m ON l.moduleId = m.id " +
           "WHERE m.courseId = :courseId " +
           "AND lp.userId = :userId " +
           "AND lp.completed = true " +
           "AND lp.deleted = false " +
           "AND l.deleted = false " +
           "AND m.deleted = false")
    long countCompletedLessonsByCourseIdAndUserId(
        @Param("courseId") Long courseId,
        @Param("userId") Long userId
    );

    /**
     * Find all COMPLETED progress records across ALL students for a course.
     * Used to build the teacher completion roster (BR-LMS-016..020 aggregate).
     *
     * @param courseId the course ID
     * @return list of completed progress records for every student in the course
     */
    @Query("SELECT lp FROM LessonProgress lp " +
           "JOIN Lesson l ON lp.lessonId = l.id " +
           "JOIN CourseModule m ON l.moduleId = m.id " +
           "WHERE m.courseId = :courseId " +
           "AND lp.completed = true " +
           "AND lp.deleted = false " +
           "AND l.deleted = false " +
           "AND m.deleted = false")
    List<LessonProgress> findCompletedProgressByCourseId(@Param("courseId") Long courseId);

    /**
     * Find all progress records for a user.
     *
     * @param userId the user ID
     * @return list of progress records
     */
    List<LessonProgress> findByUserIdAndDeletedFalse(Long userId);

    /**
     * Find all progress records for a lesson.
     *
     * @param lessonId the lesson ID
     * @return list of progress records
     */
    List<LessonProgress> findByLessonIdAndDeletedFalse(Long lessonId);
}
