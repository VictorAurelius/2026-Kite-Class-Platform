package com.kiteclass.core.module.course.service;

import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.CourseSearchCriteria;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;

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
    CourseResponse createCourse(CreateCourseRequest request);

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
    CourseResponse updateCourse(Long id, UpdateCourseRequest request);

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
}
