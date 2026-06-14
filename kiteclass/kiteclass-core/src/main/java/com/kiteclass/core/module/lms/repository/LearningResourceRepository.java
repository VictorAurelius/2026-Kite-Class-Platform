package com.kiteclass.core.module.lms.repository;

import com.kiteclass.core.common.constant.ResourceType;
import com.kiteclass.core.module.lms.entity.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LearningResource entity.
 * Supports finding resources by lesson and filtering by type.
 *
 * @since 2.9.0
 */
@Repository
public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {

    /**
     * Find all resources for a lesson.
     * Only returns non-deleted resources.
     *
     * @param lessonId the lesson ID
     * @return list of resources
     */
    List<LearningResource> findByLessonIdAndDeletedFalse(Long lessonId);

    /**
     * Find resources by lesson and type.
     *
     * @param lessonId the lesson ID
     * @param type the resource type
     * @return list of resources matching the type
     */
    List<LearningResource> findByLessonIdAndTypeAndDeletedFalse(Long lessonId, ResourceType type);

    /**
     * Find a resource by ID (non-deleted only).
     *
     * @param id the resource ID
     * @return Optional containing the resource if found and not deleted
     */
    Optional<LearningResource> findByIdAndDeletedFalse(Long id);

    /**
     * Count non-deleted resources for a lesson.
     *
     * @param lessonId the lesson ID
     * @return count of resources
     */
    long countByLessonIdAndDeletedFalse(Long lessonId);

    /**
     * Find non-deleted resources whose {@code url} contains the given fragment.
     *
     * <p>GAP-1307: used by the storage download paywall to resolve which lesson(s) a stored
     * file backs. The {@code url} column is free text (S3 key, presigned URL, external link,
     * YouTube, ...) — there is no clean FK to {@code uploaded_files}. The storage key (which
     * embeds a random UUID, so collisions are effectively impossible) is matched as a substring.
     * A non-match means the file is not recognised as lesson material and is NOT paywalled
     * (see {@code LessonMaterialAccessGuard} PARTIAL note).
     *
     * @param urlFragment the storage path / key fragment to match within {@code url}
     * @return matching non-deleted learning resources
     */
    List<LearningResource> findByUrlContainingAndDeletedFalse(String urlFragment);
}
