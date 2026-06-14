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
}
