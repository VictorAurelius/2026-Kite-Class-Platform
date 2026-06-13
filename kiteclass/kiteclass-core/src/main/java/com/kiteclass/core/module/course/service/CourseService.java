package com.kiteclass.core.module.course.service;

import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.CourseSearchCriteria;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import jakarta.validation.Valid;

/**
 * Service interface for Course business logic.
 *
 * <p>Provides operations for:
 * <ul>
 *   <li>Creating new courses with auto-assignment of CREATOR role</li>
 *   <li>Retrieving courses (by ID or with search filters)</li>
 *   <li>Updating course information (with status-based restrictions)</li>
 *   <li>Soft-deleting courses (only DRAFT courses without classes)</li>
 *   <li>Publishing courses (DRAFT → PUBLISHED)</li>
 *   <li>Archiving courses (PUBLISHED → ARCHIVED)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
public interface CourseService {

    /**
     * Creates a new course.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>BR-COURSE-001: Validates that code is unique</li>
     *   <li>BR-COURSE-003: Auto-creates TeacherCourse with role=CREATOR for the teacher</li>
     *   <li>Validates that teacher exists and is ACTIVE</li>
     *   <li>Sets initial status to DRAFT</li>
     * </ul>
     *
     * @param request the create request with course details
     * @return CourseResponse with created course data
     * @throws com.kiteclass.core.common.exception.DuplicateResourceException if code already exists
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if teacher not found
     */
    CourseResponse createCourse(@Valid CreateCourseRequest request);

    /**
     * Retrieves a course by ID.
     *
     * @param id the course ID
     * @return CourseResponse with course data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found or deleted
     */
    CourseResponse getCourseById(Long id);

    /**
     * Searches courses with filters and pagination.
     *
     * <p>Search criteria:
     * <ul>
     *   <li>search: matches name or code (case-insensitive)</li>
     *   <li>status: filters by course status (null = all statuses)</li>
     *   <li>teacherId: filters by course creator (null = all teachers)</li>
     * </ul>
     *
     * @param criteria the search criteria with filters and pagination
     * @return PageResponse with matching courses
     */
    PageResponse<CourseResponse> getCourses(CourseSearchCriteria criteria);

    /**
     * Updates an existing course.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>BR-COURSE-002: Update restrictions based on status</li>
     *   <li>DRAFT: Can update all fields</li>
     *   <li>PUBLISHED: Can only update description, syllabus, objectives, price, coverImageUrl</li>
     *   <li>ARCHIVED: Cannot update (read-only)</li>
     * </ul>
     *
     * <p>Only updates non-null fields from request.
     * Code and teacherId cannot be changed after creation.
     *
     * @param id      the course ID
     * @param request the update request with new values
     * @return CourseResponse with updated course data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found
     * @throws com.kiteclass.core.common.exception.ValidationException if update not allowed for current status
     */
    CourseResponse updateCourse(Long id, @Valid UpdateCourseRequest request);

    /**
     * Soft-deletes a course.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>BR-COURSE-004: Cannot delete if has active classes (class_count > 0)</li>
     *   <li>Only DRAFT courses can be deleted</li>
     *   <li>PUBLISHED or ARCHIVED courses must be archived instead of deleted</li>
     * </ul>
     *
     * <p>Sets deleted flag to true instead of physically removing the record.
     * Deleted courses are excluded from normal queries.
     *
     * @param id the course ID
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found
     * @throws com.kiteclass.core.common.exception.ValidationException if course cannot be deleted
     */
    void deleteCourse(Long id);

    /**
     * Publishes a course (changes status from DRAFT to PUBLISHED).
     *
     * <p>Business Rules:
     * <ul>
     *   <li>Course must be in DRAFT status</li>
     *   <li>Course must have required fields: name, description, syllabus, objectives, durationWeeks</li>
     * </ul>
     *
     * <p>After publishing:
     * <ul>
     *   <li>Course becomes visible to students</li>
     *   <li>Students can enroll in classes of this course</li>
     *   <li>Limited edits allowed (BR-COURSE-002)</li>
     * </ul>
     *
     * @param id the course ID
     * @return CourseResponse with published course data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found
     * @throws com.kiteclass.core.common.exception.ValidationException if course cannot be published
     */
    CourseResponse publishCourse(Long id);

    /**
     * Unpublishes a course (changes status from PUBLISHED back to DRAFT).
     *
     * <p>Business Rules:
     * <ul>
     *   <li>Course must be in PUBLISHED status (ARCHIVED is terminal, DRAFT is already unpublished)</li>
     * </ul>
     *
     * <p>After unpublishing the course returns to DRAFT so the teacher can make
     * full edits (BR-COURSE-002) and re-publish later. To take a published course
     * out of the catalog permanently use {@link #archiveCourse(Long)} instead.
     *
     * @param id the course ID
     * @return CourseResponse with the unpublished (DRAFT) course data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found
     * @throws com.kiteclass.core.common.exception.ValidationException if course is not PUBLISHED
     */
    CourseResponse unpublishCourse(Long id);

    /**
     * Archives a course (changes status from PUBLISHED to ARCHIVED).
     *
     * <p>Business Rules:
     * <ul>
     *   <li>Course must be in PUBLISHED status</li>
     * </ul>
     *
     * <p>After archiving:
     * <ul>
     *   <li>Course is not visible in course catalog</li>
     *   <li>No new enrollments accepted</li>
     *   <li>Existing students can continue their classes</li>
     *   <li>Course becomes read-only</li>
     * </ul>
     *
     * @param id the course ID
     * @return CourseResponse with archived course data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found
     * @throws com.kiteclass.core.common.exception.ValidationException if course cannot be archived
     */
    CourseResponse archiveCourse(Long id);

    /**
     * Adds a prerequisite to a course.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>Both course and prerequisite must exist and not be deleted</li>
     *   <li>Cannot create self-prerequisite (course cannot require itself)</li>
     *   <li>Cannot create circular dependencies (DFS validation)</li>
     * </ul>
     *
     * <p>Example: "Algebra 2" requires "Algebra 1" → adds Algebra 1 as prerequisite of Algebra 2
     *
     * @param courseId ID of course to add prerequisite to
     * @param prerequisiteId ID of prerequisite course to add
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course or prerequisite not found
     * @throws com.kiteclass.core.common.exception.ValidationException if adding would create circular dependency
     */
    void addPrerequisite(Long courseId, Long prerequisiteId);

    /**
     * Removes a prerequisite from a course.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>Course must exist and not be deleted</li>
     *   <li>If prerequisite doesn't exist in relationship, operation is idempotent (no error)</li>
     * </ul>
     *
     * @param courseId ID of course to remove prerequisite from
     * @param prerequisiteId ID of prerequisite course to remove
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found
     */
    void removePrerequisite(Long courseId, Long prerequisiteId);

    /**
     * Searches courses by level and/or category with pagination.
     *
     * <p>Search criteria:
     * <ul>
     *   <li>level: filters by course difficulty level (null = all levels)</li>
     *   <li>category: filters by course category (null = all categories)</li>
     * </ul>
     *
     * <p>Both parameters are optional. If both are null, returns all courses.
     *
     * @param level     the course level filter (can be null)
     * @param category  the course category filter (can be null)
     * @param page      the page number (0-indexed)
     * @param size      the page size
     * @param sortBy    the field to sort by (default: "name")
     * @param direction the sort direction (ASC or DESC)
     * @return PageResponse with matching courses
     */
    PageResponse<CourseResponse> searchByLevelAndCategory(
            String level,
            String category,
            int page,
            int size,
            String sortBy,
            String direction
    );
}
