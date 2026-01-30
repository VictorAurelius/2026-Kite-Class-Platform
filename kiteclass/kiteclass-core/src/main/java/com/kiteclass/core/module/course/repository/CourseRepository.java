package com.kiteclass.core.module.course.repository;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.module.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Course entity.
 *
 * <p>Provides data access methods including:
 * <ul>
 *   <li>Find by ID or code (excluding soft-deleted records)</li>
 *   <li>Check existence by code</li>
 *   <li>Search courses with filters (name, code, status, teacher)</li>
 *   <li>Find courses by teacher or status</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * Finds a course by ID, excluding soft-deleted records.
     *
     * @param id the course ID
     * @return Optional containing the course if found and not deleted
     */
    Optional<Course> findByIdAndDeletedFalse(Long id);

    /**
     * Finds a course by code, excluding soft-deleted records.
     *
     * @param code the course code
     * @return Optional containing the course if found and not deleted
     */
    Optional<Course> findByCodeAndDeletedFalse(String code);

    /**
     * Checks if a course with given code exists (excluding deleted).
     *
     * @param code the code to check
     * @return true if code exists
     */
    boolean existsByCodeAndDeletedFalse(String code);

    /**
     * Finds all courses for a specific teacher with pagination.
     *
     * @param teacherId the teacher ID
     * @param pageable  pagination parameters
     * @return page of courses created by the teacher
     */
    Page<Course> findByTeacherIdAndDeletedFalse(Long teacherId, Pageable pageable);

    /**
     * Finds all courses with a specific status with pagination.
     *
     * @param status   the course status
     * @param pageable pagination parameters
     * @return page of courses with the status
     */
    Page<Course> findByStatusAndDeletedFalse(CourseStatus status, Pageable pageable);

    /**
     * Searches courses by multiple criteria with pagination.
     *
     * <p>Search criteria:
     * <ul>
     *   <li>search: matches name or code (case-insensitive, partial match)</li>
     *   <li>status: filters by course status (null = all statuses)</li>
     *   <li>teacherId: filters by teacher who created the course (null = all teachers)</li>
     * </ul>
     *
     * <p>Only returns non-deleted courses.
     *
     * @param search    the search keyword (can be null)
     * @param status    the course status filter (can be null)
     * @param teacherId the teacher ID filter (can be null)
     * @param pageable  pagination parameters
     * @return page of matching courses
     */
    @Query("""
            SELECT c FROM Course c
            WHERE c.deleted = false
            AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:status IS NULL OR c.status = :status)
            AND (:teacherId IS NULL OR c.teacherId = :teacherId)
            """)
    Page<Course> findBySearchCriteria(
            @Param("search") String search,
            @Param("status") CourseStatus status,
            @Param("teacherId") Long teacherId,
            Pageable pageable
    );

    /**
     * Finds all courses with given status (excluding deleted).
     *
     * @param status the course status
     * @return list of courses with the status
     */
    List<Course> findByStatusAndDeletedFalse(CourseStatus status);

    /**
     * Counts courses with given status (excluding deleted).
     *
     * @param status the course status
     * @return count of courses with the status
     */
    long countByStatusAndDeletedFalse(CourseStatus status);

    /**
     * Counts courses created by a specific teacher (excluding deleted).
     *
     * @param teacherId the teacher ID
     * @return count of courses created by the teacher
     */
    long countByTeacherIdAndDeletedFalse(Long teacherId);

    /**
     * Finds all PUBLISHED courses with pagination.
     * Useful for course catalog display.
     *
     * @param pageable pagination parameters
     * @return page of published courses
     */
    default Page<Course> findPublishedCourses(Pageable pageable) {
        return findByStatusAndDeletedFalse(CourseStatus.PUBLISHED, pageable);
    }

    /**
     * Finds all DRAFT courses for a teacher with pagination.
     * Useful for teacher's course management dashboard.
     *
     * @param teacherId the teacher ID
     * @param pageable  pagination parameters
     * @return page of draft courses
     */
    @Query("""
            SELECT c FROM Course c
            WHERE c.deleted = false
            AND c.teacherId = :teacherId
            AND c.status = 'DRAFT'
            """)
    Page<Course> findDraftCoursesByTeacher(
            @Param("teacherId") Long teacherId,
            Pageable pageable
    );
}
