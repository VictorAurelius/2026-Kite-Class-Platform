package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.module.lms.entity.CourseModule;
import com.kiteclass.core.module.lms.entity.LearningResource;
import com.kiteclass.core.module.lms.entity.Lesson;
import com.kiteclass.core.module.lms.repository.CourseModuleRepository;
import com.kiteclass.core.module.lms.repository.LearningResourceRepository;
import com.kiteclass.core.module.lms.repository.LessonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-1307 — storage download enrollment-paywall guard.
 *
 * <p>Verifies {@link LessonMaterialAccessGuardImpl}: a non-enrolled student must be denied a
 * presigned download URL for a file that backs a paid (non-trial) lesson, while enrolled
 * students, the uploader, staff, trial-lesson material, and non-lesson files are unaffected.
 *
 * @since GAP-1307
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GAP-1307 — Lesson material download paywall guard")
class LessonMaterialAccessGuardImplTest {

    @Mock private LearningResourceRepository learningResourceRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private LessonAccessGuard lessonAccessGuard;

    @InjectMocks private LessonMaterialAccessGuardImpl guard;

    private static final String STORAGE_PATH = "tenantA/uploads/2026/06/abc-uuid.pdf";
    private static final Long UPLOADER_ID = 100L;     // teacher who uploaded the material
    private static final Long STUDENT_ID = 200L;      // student requesting download
    private static final Long LESSON_ID = 10L;
    private static final Long MODULE_ID = 20L;
    private static final Long COURSE_ID = 30L;

    private void linkFileToLesson(boolean trial) {
        LearningResource resource = LearningResource.builder()
            .lessonId(LESSON_ID)
            .url("https://cdn.kite.test/" + STORAGE_PATH)
            .title("Paid material")
            .build();
        Lesson lesson = Lesson.builder().moduleId(MODULE_ID).title("Lesson 1").isTrial(trial).build();
        lesson.setId(LESSON_ID);
        CourseModule module = CourseModule.builder().courseId(COURSE_ID).title("Module 1").build();

        when(learningResourceRepository.findByUrlContainingAndDeletedFalse(STORAGE_PATH))
            .thenReturn(List.of(resource));
        lenient().when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.of(lesson));
        lenient().when(courseModuleRepository.findByIdAndDeletedFalse(MODULE_ID)).thenReturn(Optional.of(module));
    }

    @Test
    @DisplayName("Non-enrolled student is denied a paid-lesson material download (403)")
    void nonEnrolledStudent_paidLesson_denied() {
        linkFileToLesson(false);
        when(lessonAccessGuard.isStudentEnrolledInCourse(STUDENT_ID, COURSE_ID)).thenReturn(false);

        assertThatThrownBy(() -> guard.verifyLessonMaterialDownloadAccess(
                STORAGE_PATH, UPLOADER_ID, STUDENT_ID, false))
            .isInstanceOf(PermissionDeniedException.class)
            .hasMessageContaining("STUDENT_NOT_ENROLLED_IN_COURSE");
    }

    @Test
    @DisplayName("Enrolled student downloads a paid-lesson material (allowed)")
    void enrolledStudent_paidLesson_allowed() {
        linkFileToLesson(false);
        when(lessonAccessGuard.isStudentEnrolledInCourse(STUDENT_ID, COURSE_ID)).thenReturn(true);

        assertThatCode(() -> guard.verifyLessonMaterialDownloadAccess(
                STORAGE_PATH, UPLOADER_ID, STUDENT_ID, false))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Trial/preview lesson material is never paywalled")
    void trialLesson_allowed() {
        linkFileToLesson(true);

        assertThatCode(() -> guard.verifyLessonMaterialDownloadAccess(
                STORAGE_PATH, UPLOADER_ID, STUDENT_ID, false))
            .doesNotThrowAnyException();

        verify(lessonAccessGuard, never()).isStudentEnrolledInCourse(STUDENT_ID, COURSE_ID);
    }

    @Test
    @DisplayName("Staff (elevatedRole) bypass the enrollment paywall without touching the LMS graph")
    void staff_bypassesPaywall() {
        assertThatCode(() -> guard.verifyLessonMaterialDownloadAccess(
                STORAGE_PATH, UPLOADER_ID, STUDENT_ID, true))
            .doesNotThrowAnyException();

        verify(learningResourceRepository, never()).findByUrlContainingAndDeletedFalse(STORAGE_PATH);
    }

    @Test
    @DisplayName("Uploader keeps access to their own file")
    void uploader_allowed() {
        assertThatCode(() -> guard.verifyLessonMaterialDownloadAccess(
                STORAGE_PATH, UPLOADER_ID, UPLOADER_ID, false))
            .doesNotThrowAnyException();

        verify(learningResourceRepository, never()).findByUrlContainingAndDeletedFalse(STORAGE_PATH);
    }

    @Test
    @DisplayName("Non-lesson file (no resource link) is not paywalled — behaviour unchanged")
    void nonLessonFile_allowed() {
        when(learningResourceRepository.findByUrlContainingAndDeletedFalse(STORAGE_PATH))
            .thenReturn(List.of());

        assertThatCode(() -> guard.verifyLessonMaterialDownloadAccess(
                STORAGE_PATH, UPLOADER_ID, STUDENT_ID, false))
            .doesNotThrowAnyException();

        verify(lessonAccessGuard, never()).isStudentEnrolledInCourse(STUDENT_ID, COURSE_ID);
    }
}
