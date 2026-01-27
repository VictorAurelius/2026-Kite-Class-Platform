package com.kiteclass.core.module.student.repository;

import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.module.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Student entity.
 *
 * <p>Provides data access methods including:
 * <ul>
 *   <li>Find by ID (excluding soft-deleted records)</li>
 *   <li>Check existence by email or phone</li>
 *   <li>Search students with filters (name, email, status)</li>
 *   <li>Count students by status</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * Finds a student by ID, excluding soft-deleted records.
     *
     * @param id the student ID
     * @return Optional containing the student if found and not deleted
     */
    Optional<Student> findByIdAndDeletedFalse(Long id);

    /**
     * Checks if a student with given email exists (excluding deleted).
     *
     * @param email the email to check
     * @return true if email exists
     */
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * Checks if a student with given phone exists (excluding deleted).
     *
     * @param phone the phone number to check
     * @return true if phone exists
     */
    boolean existsByPhoneAndDeletedFalse(String phone);

    /**
     * Searches students by name/email and status with pagination.
     *
     * <p>Search criteria:
     * <ul>
     *   <li>search: matches name or email (case-insensitive, partial match)</li>
     *   <li>status: filters by student status (null = all statuses)</li>
     * </ul>
     *
     * <p>Only returns non-deleted students.
     *
     * @param search the search keyword (can be null)
     * @param status the student status filter (can be null)
     * @param pageable pagination parameters
     * @return page of matching students
     */
    @Query("""
            SELECT s FROM Student s
            WHERE s.deleted = false
            AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:status IS NULL OR s.status = :status)
            """)
    Page<Student> findBySearchCriteria(
            @Param("search") String search,
            @Param("status") StudentStatus status,
            Pageable pageable
    );

    /**
     * Finds all students with given status (excluding deleted).
     *
     * @param status the student status
     * @return list of students with the status
     */
    List<Student> findByStatusAndDeletedFalse(StudentStatus status);

    /**
     * Counts students with given status (excluding deleted).
     *
     * @param status the student status
     * @return count of students with the status
     */
    long countByStatusAndDeletedFalse(StudentStatus status);
}
