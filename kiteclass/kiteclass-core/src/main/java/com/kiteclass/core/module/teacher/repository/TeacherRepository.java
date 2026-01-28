package com.kiteclass.core.module.teacher.repository;

import com.kiteclass.core.common.constant.TeacherStatus;
import com.kiteclass.core.module.teacher.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Teacher entity.
 *
 * <p>Provides data access methods including:
 * <ul>
 *   <li>Find by ID (excluding soft-deleted records)</li>
 *   <li>Check existence by email</li>
 *   <li>Search teachers with filters (name, email, specialization, status)</li>
 *   <li>Count teachers by status</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    /**
     * Finds a teacher by ID, excluding soft-deleted records.
     *
     * @param id the teacher ID
     * @return Optional containing the teacher if found and not deleted
     */
    Optional<Teacher> findByIdAndDeletedFalse(Long id);

    /**
     * Finds a teacher by email, excluding soft-deleted records.
     *
     * @param email the teacher email
     * @return Optional containing the teacher if found and not deleted
     */
    Optional<Teacher> findByEmailAndDeletedFalse(String email);

    /**
     * Checks if a teacher with given email exists (excluding deleted).
     *
     * @param email the email to check
     * @return true if email exists
     */
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * Searches teachers by name/email/specialization and status with pagination.
     *
     * <p>Search criteria:
     * <ul>
     *   <li>search: matches name, email, or specialization (case-insensitive, partial match)</li>
     *   <li>status: filters by teacher status (null = all statuses)</li>
     * </ul>
     *
     * <p>Only returns non-deleted teachers.
     *
     * @param search the search keyword (can be null)
     * @param status the teacher status filter (can be null)
     * @param pageable pagination parameters
     * @return page of matching teachers
     */
    @Query("""
            SELECT t FROM Teacher t
            WHERE t.deleted = false
            AND (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(t.specialization) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:status IS NULL OR t.status = :status)
            """)
    Page<Teacher> findBySearchCriteria(
            @Param("search") String search,
            @Param("status") TeacherStatus status,
            Pageable pageable
    );

    /**
     * Finds all teachers with given status (excluding deleted).
     *
     * @param status the teacher status
     * @return list of teachers with the status
     */
    List<Teacher> findByStatusAndDeletedFalse(TeacherStatus status);

    /**
     * Counts teachers with given status (excluding deleted).
     *
     * @param status the teacher status
     * @return count of teachers with the status
     */
    long countByStatusAndDeletedFalse(TeacherStatus status);
}
