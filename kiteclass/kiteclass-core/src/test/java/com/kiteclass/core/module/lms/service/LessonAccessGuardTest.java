package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LessonAccessGuard} — the single shared LMS paywall guard
 * (BR-LMS-002 / BR-LMS-019) used by both the read path ({@code LmsServiceImpl}) and the
 * write path ({@code LessonProgressServiceImpl}). GAP-1115 + GAP-1116.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LessonAccessGuard Tests")
class LessonAccessGuardTest {

    @Mock
    private ClassRepository classRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private LessonAccessGuard lessonAccessGuard;

    private static final Long STUDENT_ID = 200L;
    private static final Long COURSE_ID = 1L;
    private static final Long CLASS_ID = 1000L;

    private com.kiteclass.core.module.clazz.entity.Class courseClass() {
        com.kiteclass.core.module.clazz.entity.Class clazz =
                com.kiteclass.core.module.clazz.entity.Class.builder().courseId(COURSE_ID).name("Class").build();
        clazz.setId(CLASS_ID);
        return clazz;
    }

    @Test
    @DisplayName("isStudentEnrolledInCourse - true when an ACTIVE enrollment exists in a class")
    void isStudentEnrolledInCourse_true_whenActiveEnrollment() {
        when(classRepository.findByCourseIdAndDeletedFalse(eq(COURSE_ID), any()))
                .thenReturn(new PageImpl<>(List.of(courseClass())));
        when(enrollmentRepository.existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                STUDENT_ID, CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(true);

        assertThat(lessonAccessGuard.isStudentEnrolledInCourse(STUDENT_ID, COURSE_ID)).isTrue();
    }

    @Test
    @DisplayName("isStudentEnrolledInCourse - false when the course has NO classes (enrollment repo not queried)")
    void isStudentEnrolledInCourse_false_whenNoClasses() {
        when(classRepository.findByCourseIdAndDeletedFalse(eq(COURSE_ID), any()))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(lessonAccessGuard.isStudentEnrolledInCourse(STUDENT_ID, COURSE_ID)).isFalse();
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    @DisplayName("isStudentEnrolledInCourse - false when classes exist but no ACTIVE enrollment")
    void isStudentEnrolledInCourse_false_whenNoActiveEnrollment() {
        when(classRepository.findByCourseIdAndDeletedFalse(eq(COURSE_ID), any()))
                .thenReturn(new PageImpl<>(List.of(courseClass())));
        when(enrollmentRepository.existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                STUDENT_ID, CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(false);

        assertThat(lessonAccessGuard.isStudentEnrolledInCourse(STUDENT_ID, COURSE_ID)).isFalse();
    }

    @Test
    @DisplayName("verifyStudentEnrollment - does NOT throw when student is enrolled")
    void verifyStudentEnrollment_noThrow_whenEnrolled() {
        when(classRepository.findByCourseIdAndDeletedFalse(eq(COURSE_ID), any()))
                .thenReturn(new PageImpl<>(List.of(courseClass())));
        when(enrollmentRepository.existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                STUDENT_ID, CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(true);

        assertThatCode(() -> lessonAccessGuard.verifyStudentEnrollment(STUDENT_ID, COURSE_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("verifyStudentEnrollment - throws PermissionDenied when not enrolled")
    void verifyStudentEnrollment_throws_whenNotEnrolled() {
        when(classRepository.findByCourseIdAndDeletedFalse(eq(COURSE_ID), any()))
                .thenReturn(new PageImpl<>(List.of(courseClass())));
        when(enrollmentRepository.existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                STUDENT_ID, CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(false);

        assertThatThrownBy(() -> lessonAccessGuard.verifyStudentEnrollment(STUDENT_ID, COURSE_ID))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("STUDENT_NOT_ENROLLED_IN_COURSE");

        verify(enrollmentRepository).existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                STUDENT_ID, CLASS_ID, EnrollmentStatus.ACTIVE);
    }
}
