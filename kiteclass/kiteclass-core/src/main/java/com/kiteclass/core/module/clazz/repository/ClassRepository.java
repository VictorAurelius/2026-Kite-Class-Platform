package com.kiteclass.core.module.clazz.repository;

import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.module.clazz.entity.Class;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Class entity.
 *
 * <p>All query methods include {@code deleted = false} filter.
 * Multi-tenant filtering is applied via Hibernate @Filter from BaseEntity.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public interface ClassRepository extends JpaRepository<Class, Long> {

    /**
     * Finds a class by ID that is not deleted.
     * NOTE: Use this instead of findById() to respect soft-delete.
     *
     * @param id class ID
     * @return Optional containing class if found and not deleted
     */
    Optional<Class> findByIdAndDeletedFalse(Long id);

    /**
     * Finds all classes for a given course (not deleted), paginated.
     *
     * @param courseId course ID
     * @param pageable pagination params
     * @return page of classes
     */
    Page<Class> findByCourseIdAndDeletedFalse(Long courseId, Pageable pageable);

    /**
     * Finds classes for a course filtered by status (not deleted).
     *
     * @param courseId course ID
     * @param status   class status filter
     * @param pageable pagination params
     * @return page of classes
     */
    Page<Class> findByCourseIdAndStatusAndDeletedFalse(Long courseId, ClassStatus status, Pageable pageable);

    /**
     * Checks if a class name already exists within the same course and tenant.
     *
     * @param name       class name
     * @param courseId   course ID
     * @param instanceId tenant instance ID
     * @return true if name exists
     */
    boolean existsByNameAndCourseIdAndInstanceIdAndDeletedFalse(String name, Long courseId, UUID instanceId);

    /**
     * Checks if a class code is already taken (globally, codes must be unique).
     *
     * @param classCode the class code to check
     * @return true if code already exists
     */
    boolean existsByClassCodeAndDeletedFalse(String classCode);

    /**
     * Finds a class by its enrollment code.
     * Used during student self-enrollment.
     *
     * @param classCode the enrollment code
     * @return Optional containing class if code is valid
     */
    Optional<Class> findByClassCodeAndDeletedFalse(String classCode);

    /**
     * Counts classes for a given course (not deleted).
     *
     * @param courseId course ID
     * @return count of classes
     */
    long countByCourseIdAndDeletedFalse(Long courseId);

    /**
     * Finds classes by status for a tenant (for background tasks).
     *
     * @param status     class status
     * @param instanceId tenant instance ID
     * @param pageable   pagination
     * @return page of classes
     */
    @Query("SELECT c FROM Class c WHERE c.status = :status AND c.instanceId = :instanceId AND c.deleted = false")
    Page<Class> findByStatusAndInstanceId(
            @Param("status") ClassStatus status,
            @Param("instanceId") UUID instanceId,
            Pageable pageable);
}
