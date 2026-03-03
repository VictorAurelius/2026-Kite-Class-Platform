package com.kiteclass.core.module.lms.repository;

import com.kiteclass.core.module.lms.entity.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CourseModule entity.
 * Supports finding modules by course with order validation.
 *
 * @since 2.9.0
 */
@Repository
public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    /**
     * Find all modules for a course, ordered by order_number.
     * Only returns non-deleted modules.
     *
     * @param courseId the course ID
     * @return list of modules ordered by order_number
     */
    List<CourseModule> findByCourseIdAndDeletedFalseOrderByOrderNumber(Long courseId);

    /**
     * Find a module by ID (non-deleted only).
     *
     * @param id the module ID
     * @return Optional containing the module if found and not deleted
     */
    Optional<CourseModule> findByIdAndDeletedFalse(Long id);

    /**
     * Check if a module with the given order number already exists for a course.
     * Used to enforce BR-LMS-006: order number must be unique within course.
     *
     * @param courseId the course ID
     * @param orderNumber the order number to check
     * @return true if a module with this order number exists (non-deleted)
     */
    boolean existsByCourseIdAndOrderNumberAndDeletedFalse(Long courseId, Integer orderNumber);

    /**
     * Check if a module with the given order number exists for a course,
     * excluding a specific module ID (used for updates).
     *
     * @param courseId the course ID
     * @param orderNumber the order number to check
     * @param excludeId the module ID to exclude from the check
     * @return true if a module with this order number exists (excluding the specified ID)
     */
    boolean existsByCourseIdAndOrderNumberAndIdNotAndDeletedFalse(
        Long courseId, Integer orderNumber, Long excludeId
    );

    /**
     * Count non-deleted modules for a course.
     *
     * @param courseId the course ID
     * @return count of modules
     */
    long countByCourseIdAndDeletedFalse(Long courseId);
}
