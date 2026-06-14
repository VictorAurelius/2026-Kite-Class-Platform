package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single source of truth for the LMS paywall (BR-LMS-002 / BR-LMS-019): a student may only
 * read a paid lesson body or record paid-lesson progress when they hold an ACTIVE enrollment
 * in at least one class of the owning course.
 *
 * <p>GAP-1115 (read path — {@code LmsServiceImpl#getCourseStructureForStudent} /
 * {@code #getLessonForStudent}) and GAP-1116 (write path —
 * {@code LessonProgressServiceImpl#completeLesson}) previously duplicated this enrollment
 * check across two service classes, which risked the "can read paid content" and "can
 * complete paid lesson" rules drifting apart. Both paths now delegate here so the guard
 * is enforced identically (cross-flow-bug-class-sweep: one guard helper, not N copies).
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonAccessGuard {

    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Non-throwing check: does the student hold an ACTIVE enrollment in ANY class of the
     * course? Used by paywall content-stripping (read path) where a soft "locked" outline
     * is returned instead of a hard 403.
     *
     * @param studentId the student user ID
     * @param courseId  the course ID
     * @return true if the student has an ACTIVE enrollment in at least one class of the course
     */
    public boolean isStudentEnrolledInCourse(Long studentId, Long courseId) {
        // All (non-deleted) classes of the course.
        List<com.kiteclass.core.module.clazz.entity.Class> courseClasses = classRepository
                .findByCourseIdAndDeletedFalse(courseId, Pageable.unpaged())
                .getContent();

        if (courseClasses.isEmpty()) {
            return false;
        }

        // ACTIVE enrollment in ANY of those classes counts as enrolled in the course.
        return courseClasses.stream()
                .map(com.kiteclass.core.module.clazz.entity.Class::getId)
                .anyMatch(classId -> enrollmentRepository
                        .existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                                studentId, classId, EnrollmentStatus.ACTIVE));
    }

    /**
     * Throwing variant for hard-gated paid access (lesson detail body + lesson completion).
     *
     * @param studentId the student user ID
     * @param courseId  the course ID
     * @throws PermissionDeniedException ("STUDENT_NOT_ENROLLED_IN_COURSE") if the student has
     *                                   no active enrollment in any class of the course
     */
    public void verifyStudentEnrollment(Long studentId, Long courseId) {
        if (!isStudentEnrolledInCourse(studentId, courseId)) {
            log.warn("Student {} not enrolled in course {} - paid LMS access denied", studentId, courseId);
            throw new PermissionDeniedException("STUDENT_NOT_ENROLLED_IN_COURSE");
        }
        log.debug("Student {} has active enrollment in course {}", studentId, courseId);
    }
}
