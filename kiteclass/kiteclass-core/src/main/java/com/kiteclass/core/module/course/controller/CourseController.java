package com.kiteclass.core.module.course.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.CourseSearchCriteria;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import com.kiteclass.core.module.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Course operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>POST /api/v1/courses - Create course</li>
 *   <li>GET /api/v1/courses/{id} - Get course by ID</li>
 *   <li>GET /api/v1/courses - Search courses</li>
 *   <li>PUT /api/v1/courses/{id} - Update course</li>
 *   <li>DELETE /api/v1/courses/{id} - Delete course</li>
 *   <li>POST /api/v1/courses/{id}/publish - Publish course</li>
 *   <li>POST /api/v1/courses/{id}/archive - Archive course</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course", description = "Course management APIs")
public class CourseController {

    private final CourseService courseService;

    /**
     * Creates a new course.
     *
     * <p>Auto-creates TeacherCourse with role=CREATOR for the teacher.
     * Initial status is DRAFT.
     *
     * @param request the create request with course details
     * @return ApiResponse with created course data and HTTP 201
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new course",
            description = "Creates a new course with DRAFT status and assigns CREATOR role to teacher")
    public ApiResponse<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        log.info("REST request to create course: {}", request.name());
        CourseResponse response = courseService.createCourse(request);
        return ApiResponse.success(response, "Course created successfully");
    }

    /**
     * Retrieves a course by ID.
     *
     * @param id the course ID
     * @return ApiResponse with course data and HTTP 200
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get course by ID", description = "Retrieves a course's information by its ID")
    public ApiResponse<CourseResponse> getCourseById(
            @Parameter(description = "Course ID") @PathVariable Long id) {
        log.debug("REST request to get course with ID: {}", id);
        CourseResponse response = courseService.getCourseById(id);
        return ApiResponse.success(response);
    }

    /**
     * Searches courses with filters and pagination.
     *
     * @param search    the search keyword (name or code)
     * @param status    the course status filter (DRAFT, PUBLISHED, ARCHIVED)
     * @param teacherId the teacher ID filter
     * @param page      page number (0-indexed)
     * @param size      page size
     * @param sort      sort criteria (format: "field,direction")
     * @return ApiResponse with page of courses and HTTP 200
     */
    @GetMapping
    @Operation(summary = "Search courses", description = "Searches courses with optional filters and pagination")
    public ApiResponse<PageResponse<CourseResponse>> getCourses(
            @Parameter(description = "Search keyword (name or code)") @RequestParam(required = false) String search,
            @Parameter(description = "Course status filter (DRAFT, PUBLISHED, ARCHIVED)") @RequestParam(required = false) String status,
            @Parameter(description = "Teacher ID filter") @RequestParam(required = false) Long teacherId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'name,asc' or 'createdAt,desc')") @RequestParam(defaultValue = "createdAt,desc") String sort) {
        log.debug("REST request to search courses: search='{}', status='{}', teacherId='{}', page={}, size={}",
                search, status, teacherId, page, size);

        CourseSearchCriteria criteria = new CourseSearchCriteria(search, status, teacherId, page, size, sort);
        PageResponse<CourseResponse> response = courseService.getCourses(criteria);

        return ApiResponse.success(response);
    }

    /**
     * Updates an existing course.
     *
     * <p>Update restrictions apply based on course status (BR-COURSE-002):
     * <ul>
     *   <li>DRAFT: Can update all fields</li>
     *   <li>PUBLISHED: Can only update description, syllabus, objectives, price, coverImageUrl</li>
     *   <li>ARCHIVED: Read-only, no updates allowed</li>
     * </ul>
     *
     * @param id      the course ID
     * @param request the update request with new values
     * @return ApiResponse with updated course data and HTTP 200
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update course",
            description = "Updates an existing course. Update restrictions apply based on status.")
    public ApiResponse<CourseResponse> updateCourse(
            @Parameter(description = "Course ID") @PathVariable Long id,
            @Valid @RequestBody UpdateCourseRequest request) {
        log.info("REST request to update course with ID: {}", id);
        CourseResponse response = courseService.updateCourse(id, request);
        return ApiResponse.success(response, "Course updated successfully");
    }

    /**
     * Soft-deletes a course.
     *
     * <p>Only DRAFT courses without classes can be deleted.
     * PUBLISHED or ARCHIVED courses should be archived instead.
     *
     * @param id the course ID
     * @return ApiResponse with success message and HTTP 200
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete course",
            description = "Soft-deletes a course. Only DRAFT courses without classes can be deleted.")
    public ApiResponse<Void> deleteCourse(
            @Parameter(description = "Course ID") @PathVariable Long id) {
        log.info("REST request to delete course with ID: {}", id);
        courseService.deleteCourse(id);
        return ApiResponse.success(null, "Course deleted successfully");
    }

    /**
     * Publishes a course (changes status from DRAFT to PUBLISHED).
     *
     * <p>Course must have all required fields filled.
     * After publishing, course becomes visible to students.
     *
     * @param id the course ID
     * @return ApiResponse with published course data and HTTP 200
     */
    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish course",
            description = "Changes course status from DRAFT to PUBLISHED. Makes course visible to students.")
    public ApiResponse<CourseResponse> publishCourse(
            @Parameter(description = "Course ID") @PathVariable Long id) {
        log.info("REST request to publish course with ID: {}", id);
        CourseResponse response = courseService.publishCourse(id);
        return ApiResponse.success(response, "Course published successfully");
    }

    /**
     * Archives a course (changes status from PUBLISHED to ARCHIVED).
     *
     * <p>After archiving, no new enrollments are accepted.
     * Existing students can continue their classes.
     *
     * @param id the course ID
     * @return ApiResponse with archived course data and HTTP 200
     */
    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive course",
            description = "Changes course status from PUBLISHED to ARCHIVED. No new enrollments accepted.")
    public ApiResponse<CourseResponse> archiveCourse(
            @Parameter(description = "Course ID") @PathVariable Long id) {
        log.info("REST request to archive course with ID: {}", id);
        CourseResponse response = courseService.archiveCourse(id);
        return ApiResponse.success(response, "Course archived successfully");
    }
}
