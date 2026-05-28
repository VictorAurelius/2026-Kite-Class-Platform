package com.kiteclass.core.module.lms.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.lms.dto.request.CreateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLearningResourceRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLessonRequest;
import com.kiteclass.core.module.lms.dto.request.UpdateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.UpdateLessonRequest;
import com.kiteclass.core.module.lms.dto.response.CourseModuleDetailResponse;
import com.kiteclass.core.module.lms.dto.response.CourseModuleResponse;
import com.kiteclass.core.module.lms.dto.response.LearningResourceResponse;
import com.kiteclass.core.module.lms.dto.response.LessonDetailResponse;
import com.kiteclass.core.module.lms.dto.response.LessonResponse;
import com.kiteclass.core.module.lms.service.LmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for LMS (Learning Management System) operations.
 * Provides endpoints for course structure, lessons, and learning resources.
 *
 * <p>Supports three access levels:
 * <ul>
 *   <li>Guest: View course structure + trial lessons (NO X-User-Id header)</li>
 *   <li>Student: View all lessons + track progress (X-User-Id header)</li>
 *   <li>Teacher: Full CRUD on modules/lessons (X-Teacher-Id header)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/lms")
@RequiredArgsConstructor
@Tag(name = "LMS", description = "Learning Management System APIs - Course structure, lessons, and resources")
public class LmsController {

    private final LmsService lmsService;

    // ==================== Public/Student Endpoints ====================

    /**
     * Get course structure (modules + lessons).
     * Dual-mode: guest (X-User-Id missing) vs student (X-User-Id present).
     *
     * @param courseId the course ID
     * @param userId optional student user ID (null for guest access)
     * @return list of modules with lessons
     */
    @GetMapping("/courses/{courseId}/modules")
    @Operation(summary = "Get course structure",
               description = "Guest: returns trial lessons only. Student: returns all lessons (requires enrollment).")
    public ApiResponse<List<CourseModuleDetailResponse>> getCourseStructure(
            @PathVariable Long courseId,
            @Parameter(description = "Student user ID (optional for guest access)")
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long userId) {

        log.info("GET /api/v1/lms/courses/{}/modules - userId: {}", courseId, userId);

        if (userId != null) {
            // Student access: all lessons
            return ApiResponse.success(lmsService.getCourseStructureForStudent(courseId, userId));
        } else {
            // Guest access: trial lessons only
            return ApiResponse.success(lmsService.getCourseStructurePublic(courseId));
        }
    }

    /**
     * Get lesson detail.
     * Dual-mode: guest (X-User-Id missing) vs student (X-User-Id present).
     *
     * @param lessonId the lesson ID
     * @param userId optional student user ID (null for guest access)
     * @return lesson detail with resources
     */
    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Get lesson detail",
               description = "Guest: trial lessons only. Student: all lessons (requires enrollment for paid lessons).")
    public ApiResponse<LessonDetailResponse> getLesson(
            @PathVariable Long lessonId,
            @Parameter(description = "Student user ID (optional for guest access)")
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long userId) {

        log.info("GET /api/v1/lms/lessons/{} - userId: {}", lessonId, userId);

        if (userId != null) {
            // Student access
            return ApiResponse.success(lmsService.getLessonForStudent(lessonId, userId));
        } else {
            // Guest access
            return ApiResponse.success(lmsService.getLessonPublic(lessonId));
        }
    }

    // ==================== Teacher Endpoints - Module CRUD ====================

    /**
     * Create a new module for a course (teacher only).
     *
     * @param courseId the course ID
     * @param request create module request
     * @param teacherId teacher user ID (must be course owner)
     * @return created module response
     */
    @PostMapping("/courses/{courseId}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create course module (teacher only)",
               description = "Only course owner can create modules. Order number must be unique within course.")
    public ApiResponse<CourseModuleResponse> createModule(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateCourseModuleRequest request,
            @Parameter(description = "Teacher user ID (must be course owner)", required = true)
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        log.info("POST /api/v1/lms/courses/{}/modules - teacherId: {}", courseId, teacherId);
        return ApiResponse.success(lmsService.createModule(courseId, request, teacherId));
    }

    /**
     * Update an existing module (teacher only).
     *
     * @param moduleId the module ID
     * @param request update module request
     * @param teacherId teacher user ID (must be course owner)
     * @return updated module response
     */
    @PutMapping("/modules/{moduleId}")
    @Operation(summary = "Update course module (teacher only)",
               description = "Only course owner can update modules.")
    public ApiResponse<CourseModuleResponse> updateModule(
            @PathVariable Long moduleId,
            @Valid @RequestBody UpdateCourseModuleRequest request,
            @Parameter(description = "Teacher user ID (must be course owner)", required = true)
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        log.info("PUT /api/v1/lms/modules/{} - teacherId: {}", moduleId, teacherId);
        return ApiResponse.success(lmsService.updateModule(moduleId, request, teacherId));
    }

    /**
     * Delete a module (teacher only).
     *
     * @param moduleId the module ID
     * @param teacherId teacher user ID (must be course owner)
     * @return success response
     */
    @DeleteMapping("/modules/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete course module (teacher only)",
               description = "Only course owner can delete modules. Cannot delete if module has lessons.")
    public ApiResponse<Void> deleteModule(
            @PathVariable Long moduleId,
            @Parameter(description = "Teacher user ID (must be course owner)", required = true)
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        log.info("DELETE /api/v1/lms/modules/{} - teacherId: {}", moduleId, teacherId);
        lmsService.deleteModule(moduleId, teacherId);
        return ApiResponse.success(null);
    }

    /**
     * Get module detail (teacher only).
     *
     * @param moduleId the module ID
     * @param teacherId teacher user ID (must be course owner)
     * @return module detail with all lessons
     */
    @GetMapping("/modules/{moduleId}")
    @Operation(summary = "Get module detail (teacher only)",
               description = "Only course owner can view module details.")
    public ApiResponse<CourseModuleDetailResponse> getModule(
            @PathVariable Long moduleId,
            @Parameter(description = "Teacher user ID (must be course owner)", required = true)
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        log.info("GET /api/v1/lms/modules/{} - teacherId: {}", moduleId, teacherId);
        return ApiResponse.success(lmsService.getModule(moduleId, teacherId));
    }

    // ==================== Teacher Endpoints - Lesson CRUD ====================

    /**
     * Create a new lesson for a module (teacher only).
     *
     * @param moduleId the module ID
     * @param request create lesson request
     * @param teacherId teacher user ID (must be course owner)
     * @return created lesson response
     */
    @PostMapping("/modules/{moduleId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create lesson (teacher only)",
               description = "Only course owner can create lessons. Order number must be unique within module.")
    public ApiResponse<LessonResponse> createLesson(
            @PathVariable Long moduleId,
            @Valid @RequestBody CreateLessonRequest request,
            @Parameter(description = "Teacher user ID (must be course owner)", required = true)
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        log.info("POST /api/v1/lms/modules/{}/lessons - teacherId: {}", moduleId, teacherId);
        return ApiResponse.success(lmsService.createLesson(moduleId, request, teacherId));
    }

    /**
     * Update an existing lesson (teacher only).
     *
     * @param lessonId the lesson ID
     * @param request update lesson request
     * @param teacherId teacher user ID (must be course owner)
     * @return updated lesson response
     */
    @PutMapping("/lessons/{lessonId}/manage")
    @Operation(summary = "Update lesson (teacher only)",
               description = "Only course owner can update lessons.")
    public ApiResponse<LessonResponse> updateLesson(
            @PathVariable Long lessonId,
            @Valid @RequestBody UpdateLessonRequest request,
            @Parameter(description = "Teacher user ID (must be course owner)", required = true)
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        log.info("PUT /api/v1/lms/lessons/{}/manage - teacherId: {}", lessonId, teacherId);
        return ApiResponse.success(lmsService.updateLesson(lessonId, request, teacherId));
    }

    /**
     * Delete a lesson (teacher only).
     *
     * @param lessonId the lesson ID
     * @param teacherId teacher user ID (must be course owner)
     * @return success response
     */
    @DeleteMapping("/lessons/{lessonId}/manage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete lesson (teacher only)",
               description = "Only course owner can delete lessons.")
    public ApiResponse<Void> deleteLesson(
            @PathVariable Long lessonId,
            @Parameter(description = "Teacher user ID (must be course owner)", required = true)
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        log.info("DELETE /api/v1/lms/lessons/{}/manage - teacherId: {}", lessonId, teacherId);
        lmsService.deleteLesson(lessonId, teacherId);
        return ApiResponse.success(null);
    }

    /**
     * Get lesson detail for teacher.
     *
     * @param lessonId the lesson ID
     * @param teacherId teacher user ID (must be course owner)
     * @return lesson detail with resources
     */
    @GetMapping("/lessons/{lessonId}/manage")
    @Operation(summary = "Get lesson detail (teacher only)",
               description = "Only course owner can view lesson management details.")
    public ApiResponse<LessonDetailResponse> getLessonForTeacher(
            @PathVariable Long lessonId,
            @Parameter(description = "Teacher user ID (must be course owner)", required = true)
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        log.info("GET /api/v1/lms/lessons/{}/manage - teacherId: {}", lessonId, teacherId);
        return ApiResponse.success(lmsService.getLessonForTeacher(lessonId, teacherId));
    }

    // ==================== Teacher Endpoints - Learning Resource CRUD ====================

    /**
     * Add a learning resource to a lesson (teacher only).
     *
     * @param lessonId the lesson ID
     * @param request create resource request
     * @param teacherId teacher user ID (must be course owner)
     * @return created resource response
     */
    @PostMapping("/lessons/{lessonId}/resources")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add learning resource (teacher only)",
               description = "Only course owner can add resources to lessons.")
    public ApiResponse<LearningResourceResponse> addResource(
            @PathVariable Long lessonId,
            @Valid @RequestBody CreateLearningResourceRequest request,
            @Parameter(description = "Teacher user ID (must be course owner)", required = true)
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        log.info("POST /api/v1/lms/lessons/{}/resources - teacherId: {}", lessonId, teacherId);
        return ApiResponse.success(lmsService.addResource(lessonId, request, teacherId));
    }

    /**
     * Delete a learning resource (teacher only).
     *
     * @param resourceId the resource ID
     * @param teacherId teacher user ID (must be course owner)
     * @return success response
     */
    @DeleteMapping("/resources/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete learning resource (teacher only)",
               description = "Only course owner can delete resources.")
    public ApiResponse<Void> deleteResource(
            @PathVariable Long resourceId,
            @Parameter(description = "Teacher user ID (must be course owner)", required = true)
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        log.info("DELETE /api/v1/lms/resources/{} - teacherId: {}", resourceId, teacherId);
        lmsService.deleteResource(resourceId, teacherId);
        return ApiResponse.success(null);
    }
}
