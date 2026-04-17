package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.module.lms.dto.response.CourseProgressResponse;
import com.kiteclass.core.module.lms.dto.response.LessonProgressResponse;

/**
 * Service interface for lesson progress tracking operations.
 * Handles student progress through lessons and course-level progress calculations.
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
public interface LessonProgressService {

    /**
     * Mark a lesson as completed for a student (idempotent - BR-LMS-010).
     * Creates progress record if not exists, updates if already exists.
     * Publishes LessonCompletedEvent for downstream processing.
     *
     * @param lessonId the lesson ID
     * @param userId the student user ID
     * @return updated progress response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lesson not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if student not enrolled (for paid lessons)
     */
    LessonProgressResponse completeLesson(Long lessonId, Long userId);

    /**
     * Get course progress for a student (BR-LMS-004).
     * Calculates: progressPercent = (completedLessons / totalLessons) * 100
     *
     * @param courseId the course ID
     * @param userId the student user ID
     * @return course progress response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if student not enrolled
     */
    CourseProgressResponse getCourseProgress(Long courseId, Long userId);

    /**
     * Get lesson progress for a student.
     *
     * @param lessonId the lesson ID
     * @param userId the student user ID
     * @return progress response, or null if no progress record exists
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lesson not found
     */
    LessonProgressResponse getLessonProgress(Long lessonId, Long userId);
}
