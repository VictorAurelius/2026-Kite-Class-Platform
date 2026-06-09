package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.lms.dto.response.CourseProgressResponse;
import com.kiteclass.core.module.lms.dto.response.LessonProgressResponse;
import com.kiteclass.core.module.lms.entity.CourseModule;
import com.kiteclass.core.module.lms.entity.Lesson;
import com.kiteclass.core.module.lms.entity.LessonProgress;
import com.kiteclass.core.module.lms.event.LessonCompletedEvent;
import com.kiteclass.core.module.lms.mapper.LmsMapper;
import com.kiteclass.core.module.lms.repository.CourseModuleRepository;
import com.kiteclass.core.module.lms.repository.LessonProgressRepository;
import com.kiteclass.core.module.lms.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of {@link LessonProgressService}.
 * Handles student progress tracking and course completion calculations.
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonProgressServiceImpl implements LessonProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final LmsMapper lmsMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public LessonProgressResponse completeLesson(Long lessonId, Long userId) {
        log.info("Completing lesson {} for user {}", lessonId, userId);

        // Verify lesson exists
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", (Object) lessonId));

        // BR-LMS-019 (GAP-1116): paid lessons require an active enrollment to complete;
        // trial lessons can be completed without enrollment (free preview). Previously this
        // check was a no-op log line, letting non-enrolled students mark paid lessons done.
        if (!lesson.isTrialLesson()) {
            CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(lesson.getModuleId())
                    .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) lesson.getModuleId()));
            verifyStudentEnrollment(userId, module.getCourseId());
        }

        // Find or create progress record (BR-LMS-009: one record per user per lesson)
        Optional<LessonProgress> existingProgress = lessonProgressRepository
                .findByUserIdAndLessonIdAndDeletedFalse(userId, lessonId);

        LessonProgress progress;
        boolean wasAlreadyCompleted = false;

        if (existingProgress.isPresent()) {
            progress = existingProgress.get();
            wasAlreadyCompleted = progress.getCompleted();

            if (!wasAlreadyCompleted) {
                // Mark as completed (BR-LMS-010: idempotent operation)
                progress.markAsCompleted();
                progress = lessonProgressRepository.save(progress);
                log.info("Marked lesson {} as completed for user {}", lessonId, userId);

                // Publish event for downstream processing
                eventPublisher.publishEvent(new LessonCompletedEvent(this, userId, lessonId));
            } else {
                log.debug("Lesson {} already completed by user {}", lessonId, userId);
            }
        } else {
            // Create new progress record
            progress = LessonProgress.builder()
                    .userId(userId)
                    .lessonId(lessonId)
                    .build();
            progress.markAsCompleted();
            // Note: instanceId will be set automatically by JPA @PrePersist in BaseEntity
            progress = lessonProgressRepository.save(progress);

            log.info("Created progress record and marked lesson {} as completed for user {}", lessonId, userId);

            // Publish event
            eventPublisher.publishEvent(new LessonCompletedEvent(this, userId, lessonId));
        }

        return lmsMapper.toProgressResponse(progress);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(Long courseId, Long userId) {
        log.info("Calculating course progress for user {} in course {}", userId, courseId);

        // Verify course exists
        courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) courseId));

        // BR-LMS-004: Calculate progress = (completedLessons / totalLessons) * 100
        long totalLessons = lessonRepository.countLessonsByCourseId(courseId);
        long completedLessons = lessonProgressRepository
                .countCompletedLessonsByCourseIdAndUserId(courseId, userId);

        double progressPercent = totalLessons > 0
                ? (completedLessons * 100.0) / totalLessons
                : 0.0;

        // Round to 1 decimal place
        progressPercent = Math.round(progressPercent * 10) / 10.0;

        log.debug("Course {} progress for user {}: {}/{} lessons ({}%)",
                courseId, userId, completedLessons, totalLessons, progressPercent);

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .userId(userId)
                .totalLessons((int) totalLessons)
                .completedLessons((int) completedLessons)
                .progressPercent(progressPercent)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LessonProgressResponse getLessonProgress(Long lessonId, Long userId) {
        log.info("Fetching lesson progress for user {} in lesson {}", userId, lessonId);

        // Verify lesson exists
        lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", (Object) lessonId));

        // Find progress record
        Optional<LessonProgress> progressOpt = lessonProgressRepository
                .findByUserIdAndLessonIdAndDeletedFalse(userId, lessonId);

        return progressOpt.map(lmsMapper::toProgressResponse).orElse(null);
    }

    /**
     * Verifies the student has an ACTIVE enrollment in ANY class of the course
     * (BR-LMS-002 / BR-LMS-019). Mirrors the enrollment check in
     * {@code LmsServiceImpl#getLessonForStudent} so paid-lesson progress tracking
     * stays consistent with paid-lesson content access.
     *
     * @param studentId the student user ID
     * @param courseId the course ID
     * @throws PermissionDeniedException if the student has no active enrollment in any class
     */
    private void verifyStudentEnrollment(Long studentId, Long courseId) {
        log.debug("Verifying enrollment for student {} in course {}", studentId, courseId);

        java.util.List<com.kiteclass.core.module.clazz.entity.Class> courseClasses = classRepository
                .findByCourseIdAndDeletedFalse(courseId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        boolean hasActiveEnrollment = !courseClasses.isEmpty() && courseClasses.stream()
                .map(com.kiteclass.core.module.clazz.entity.Class::getId)
                .anyMatch(classId -> enrollmentRepository
                        .existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                                studentId, classId, EnrollmentStatus.ACTIVE));

        if (!hasActiveEnrollment) {
            log.warn("Student {} not enrolled in course {} - cannot complete paid lesson",
                    studentId, courseId);
            throw new PermissionDeniedException("STUDENT_NOT_ENROLLED_IN_COURSE");
        }
    }
}
