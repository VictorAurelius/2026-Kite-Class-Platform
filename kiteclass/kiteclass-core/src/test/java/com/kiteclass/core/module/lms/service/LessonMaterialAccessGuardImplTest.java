package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.common.constant.ResourceType;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-1307 — deterministic FK paywall on the storage download path.
 *
 * <p>Verifies {@link LessonMaterialAccessGuardImpl} resolves the file → lesson → course →
 * enrollment chain via the {@code learning_resources.uploaded_file_id} FK (V100), NOT the
 * reverted #2416 {@code url}-substring heuristic, and applies the LMS enrollment paywall:
 * <ul>
 *   <li>FK-linked PAID lesson + non-enrolled student → 403;</li>
 *   <li>FK-linked PAID lesson + enrolled student → allow;</li>
 *   <li>null FK / no linked resource / trial lesson → allow (behaviour unchanged);</li>
 *   <li>staff (elevatedRole) and the uploader → allow (short-circuit, no DB lookup).</li>
 * </ul>
 *
 * @since GAP-1307
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GAP-1307 — LessonMaterialAccessGuardImpl FK-based download paywall")
class LessonMaterialAccessGuardImplTest {

    @Mock private LearningResourceRepository learningResourceRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private LessonAccessGuard lessonAccessGuard;

    @InjectMocks private LessonMaterialAccessGuardImpl guard;

    private static final Long UPLOADED_FILE_ID = 500L;
    private static final Long UPLOADER_ID = 1L;
    private static final Long STUDENT_ID = 7L;
    private static final Long LESSON_ID = 10L;
    private static final Long MODULE_ID = 20L;
    private static final Long COURSE_ID = 30L;

    private LearningResource linkedResource() {
        LearningResource r = LearningResource.builder()
            .lessonId(LESSON_ID)
            .type(ResourceType.PDF)
            .url("https://minio.example.test/tenant/uploads/2026/06/uuid.pdf")
            .title("Chapter 1 Slides")
            .uploadedFileId(UPLOADED_FILE_ID)
            .build();
        r.setId(900L);
        return r;
    }

    private Lesson lesson(boolean trial) {
        Lesson l = Lesson.builder()
            .moduleId(MODULE_ID)
            .title("Lesson 1")
            .orderNumber(1)
            .isTrial(trial)
            .build();
        l.setId(LESSON_ID);
        return l;
    }

    private CourseModule module() {
        CourseModule m = CourseModule.builder()
            .courseId(COURSE_ID)
            .title("Module 1")
            .orderNumber(1)
            .build();
        m.setId(MODULE_ID);
        return m;
    }

    // ── deny / allow on the paid path ─────────────────────────────────────────

    @Test
    @DisplayName("FK-linked PAID lesson + non-enrolled student → 403")
    void paidLesson_nonEnrolled_denied() {
        when(learningResourceRepository.findByUploadedFileIdAndDeletedFalse(UPLOADED_FILE_ID))
            .thenReturn(List.of(linkedResource()));
        when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.of(lesson(false)));
        when(courseModuleRepository.findByIdAndDeletedFalse(MODULE_ID)).thenReturn(Optional.of(module()));
        when(lessonAccessGuard.isStudentEnrolledInCourse(STUDENT_ID, COURSE_ID)).thenReturn(false);

        assertThatThrownBy(() ->
            guard.verifyLessonMaterialDownloadAccess(UPLOADED_FILE_ID, UPLOADER_ID, STUDENT_ID, false))
            .isInstanceOf(PermissionDeniedException.class)
            .hasMessageContaining("STUDENT_NOT_ENROLLED_IN_COURSE");
    }

    @Test
    @DisplayName("FK-linked PAID lesson + enrolled student → allow")
    void paidLesson_enrolled_allowed() {
        when(learningResourceRepository.findByUploadedFileIdAndDeletedFalse(UPLOADED_FILE_ID))
            .thenReturn(List.of(linkedResource()));
        when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.of(lesson(false)));
        when(courseModuleRepository.findByIdAndDeletedFalse(MODULE_ID)).thenReturn(Optional.of(module()));
        when(lessonAccessGuard.isStudentEnrolledInCourse(STUDENT_ID, COURSE_ID)).thenReturn(true);

        assertThatCode(() ->
            guard.verifyLessonMaterialDownloadAccess(UPLOADED_FILE_ID, UPLOADER_ID, STUDENT_ID, false))
            .doesNotThrowAnyException();
    }

    // ── allow short-circuits (no enrollment lookup) ───────────────────────────

    @Test
    @DisplayName("null FK (non-lesson file) → allow, no resource lookup")
    void nullFk_allowed() {
        assertThatCode(() ->
            guard.verifyLessonMaterialDownloadAccess(null, UPLOADER_ID, STUDENT_ID, false))
            .doesNotThrowAnyException();

        verify(learningResourceRepository, never()).findByUploadedFileIdAndDeletedFalse(anyLong());
    }

    @Test
    @DisplayName("FK present but no linked resource → allow (behaviour unchanged)")
    void noLinkedResource_allowed() {
        when(learningResourceRepository.findByUploadedFileIdAndDeletedFalse(UPLOADED_FILE_ID))
            .thenReturn(List.of());

        assertThatCode(() ->
            guard.verifyLessonMaterialDownloadAccess(UPLOADED_FILE_ID, UPLOADER_ID, STUDENT_ID, false))
            .doesNotThrowAnyException();

        verify(lessonAccessGuard, never()).isStudentEnrolledInCourse(anyLong(), anyLong());
    }

    @Test
    @DisplayName("trial/free lesson material → allow (no paywall)")
    void trialLesson_allowed() {
        when(learningResourceRepository.findByUploadedFileIdAndDeletedFalse(UPLOADED_FILE_ID))
            .thenReturn(List.of(linkedResource()));
        when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.of(lesson(true)));

        assertThatCode(() ->
            guard.verifyLessonMaterialDownloadAccess(UPLOADED_FILE_ID, UPLOADER_ID, STUDENT_ID, false))
            .doesNotThrowAnyException();

        verify(lessonAccessGuard, never()).isStudentEnrolledInCourse(anyLong(), anyLong());
    }

    @Test
    @DisplayName("staff (elevatedRole) → allow, no DB lookup at all")
    void staff_allowed_shortCircuit() {
        assertThatCode(() ->
            guard.verifyLessonMaterialDownloadAccess(UPLOADED_FILE_ID, UPLOADER_ID, STUDENT_ID, true))
            .doesNotThrowAnyException();

        verify(learningResourceRepository, never()).findByUploadedFileIdAndDeletedFalse(anyLong());
    }

    @Test
    @DisplayName("uploader downloads own file → allow, no DB lookup")
    void uploader_allowed_shortCircuit() {
        assertThatCode(() ->
            guard.verifyLessonMaterialDownloadAccess(UPLOADED_FILE_ID, UPLOADER_ID, UPLOADER_ID, false))
            .doesNotThrowAnyException();

        verify(learningResourceRepository, never()).findByUploadedFileIdAndDeletedFalse(anyLong());
    }

    @Test
    @DisplayName("paid lesson with deleted module → allow (defensive, no orphan paywall)")
    void paidLesson_missingModule_allowed() {
        when(learningResourceRepository.findByUploadedFileIdAndDeletedFalse(UPLOADED_FILE_ID))
            .thenReturn(List.of(linkedResource()));
        when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.of(lesson(false)));
        when(courseModuleRepository.findByIdAndDeletedFalse(MODULE_ID)).thenReturn(Optional.empty());

        assertThatCode(() ->
            guard.verifyLessonMaterialDownloadAccess(UPLOADED_FILE_ID, UPLOADER_ID, STUDENT_ID, false))
            .doesNotThrowAnyException();

        verify(lessonAccessGuard, never()).isStudentEnrolledInCourse(anyLong(), anyLong());
    }
}
