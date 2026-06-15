package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.module.lms.entity.CourseModule;
import com.kiteclass.core.module.lms.entity.LearningResource;
import com.kiteclass.core.module.lms.entity.Lesson;
import com.kiteclass.core.module.lms.repository.CourseModuleRepository;
import com.kiteclass.core.module.lms.repository.LearningResourceRepository;
import com.kiteclass.core.module.lms.repository.LessonRepository;
import com.kiteclass.core.module.storage.service.LessonMaterialAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LMS-side implementation of {@link LessonMaterialAccessGuard} — bridges the storage download
 * path to the LMS enrollment paywall (GAP-1307 / BR-LMS-002).
 *
 * <p>Delegates the actual enrollment check to {@link LessonAccessGuard}, the single source of
 * truth already used by the read path ({@code getLessonForStudent}) and write path
 * ({@code completeLesson}). This adds the third surface — file download — to that guard so the
 * "can read paid content" rule cannot drift across the three entry points.
 *
 * <p><b>Deterministic FK link (GAP-1307):</b> the file&harr;lesson link is resolved via the
 * {@code learning_resources.uploaded_file_id} FK (added in V100), NOT the fragile
 * {@code url}-substring heuristic of the reverted #2416 attempt. A file with no FK-linked
 * resource is not recognised as lesson material and is not paywalled (visibility-only,
 * behaviour unchanged).
 *
 * @author KiteClass Team
 * @since GAP-1307
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonMaterialAccessGuardImpl implements LessonMaterialAccessGuard {

    private final LearningResourceRepository learningResourceRepository;
    private final LessonRepository lessonRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final LessonAccessGuard lessonAccessGuard;

    @Override
    public void verifyLessonMaterialDownloadAccess(Long uploadedFileId,
                                                   Long uploaderId,
                                                   Long requesterId,
                                                   boolean elevatedRole) {
        // Staff (teacher/owner/admin) are never paywalled — they manage course content.
        if (elevatedRole) {
            return;
        }
        // The uploader always keeps access to their own file.
        if (requesterId != null && requesterId.equals(uploaderId)) {
            return;
        }
        if (uploadedFileId == null) {
            return;
        }

        // Resolve which lesson(s) this file backs via the deterministic FK (V100).
        List<LearningResource> linkedResources =
                learningResourceRepository.findByUploadedFileIdAndDeletedFalse(uploadedFileId);
        if (linkedResources.isEmpty()) {
            // Not recognised as lesson material → behaviour unchanged (visibility-only).
            return;
        }

        for (LearningResource resource : linkedResources) {
            Lesson lesson = lessonRepository.findByIdAndDeletedFalse(resource.getLessonId()).orElse(null);
            if (lesson == null || lesson.isTrialLesson()) {
                // Free / preview lesson material → not paywalled.
                continue;
            }
            CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(lesson.getModuleId()).orElse(null);
            if (module == null) {
                continue;
            }
            // Non-trial lesson material → require an ACTIVE enrollment in the owning course.
            if (!lessonAccessGuard.isStudentEnrolledInCourse(requesterId, module.getCourseId())) {
                log.warn("Storage paywall deny — user {} not enrolled in course {} backing file {} (lesson {})",
                        requesterId, module.getCourseId(), uploadedFileId, lesson.getId());
                throw new com.kiteclass.core.common.exception.PermissionDeniedException(
                        "STUDENT_NOT_ENROLLED_IN_COURSE");
            }
        }
    }
}
