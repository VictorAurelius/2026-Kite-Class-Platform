package com.kiteclass.core.module.lms.controller;

import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.lms.dto.request.CreateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLearningResourceRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLessonRequest;
import com.kiteclass.core.module.lms.dto.request.ReorderRequest;
import com.kiteclass.core.module.lms.dto.request.UpdateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.UpdateLessonRequest;
import com.kiteclass.core.module.lms.dto.response.CompletionRosterResponse;
import com.kiteclass.core.module.lms.dto.response.CourseModuleDetailResponse;
import com.kiteclass.core.module.lms.dto.response.CourseModuleResponse;
import com.kiteclass.core.module.lms.dto.response.LearningResourceResponse;
import com.kiteclass.core.module.lms.dto.response.LessonDetailResponse;
import com.kiteclass.core.module.lms.dto.response.LessonResponse;
import com.kiteclass.core.module.lms.service.LmsService;
import com.kiteclass.core.module.storage.dto.PresignedUploadRequest;
import com.kiteclass.core.module.storage.dto.PresignedUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 *   <li>Guest: View course structure + trial lessons (no identity header)</li>
 *   <li>Student: View all lessons + track progress ({@code X-User-Reference-Id})</li>
 *   <li>Teacher / Owner / Admin: Full CRUD on modules/lessons —
 *       role-gated by {@code @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")}</li>
 * </ul>
 *
 * <p><strong>GAP-1299 — authoring authz hardening.</strong> Every authoring/mutation
 * endpoint is (1) role-gated so STUDENT/PARENT are blocked entirely, and (2) derives
 * the acting teacher id from the authenticated principal (gateway-injected
 * {@code X-User-Reference-Id} → {@link UserContext#getCurrentReferenceId()}). The former
 * client-supplied {@code X-Teacher-Id} request header — which the gateway does NOT control
 * (per GAP-814) and which was therefore spoofable — is no longer read as an identity source:
 * a teacher cannot act as another teacher by setting it, and a STUDENT cannot impersonate a
 * teacher at all. ADMIN/OWNER (no numeric reference id) bypass per-course ownership at the
 * service layer ({@code LmsServiceImpl.verifyCourseOwnership}).
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

    /**
     * Resolve the acting teacher's numeric id from the authenticated principal
     * (gateway-injected {@code X-User-Reference-Id} → {@link UserContext}), NOT from any
     * client-supplied header (GAP-1299). Returns {@code null} for ADMIN/OWNER, who carry no
     * numeric reference id; the service layer bypasses per-course ownership for them.
     *
     * @return the authenticated teacher's reference id, or {@code null} for admin/owner
     */
    private Long actingTeacherId() {
        return UserContext.getCurrentReferenceId();
    }

    // ==================== Public/Student Endpoints ====================

    /**
     * Get course structure (modules + lessons).
     * Dual-mode: guest (X-User-Reference-Id missing) vs student (present).
     *
     * @param courseId the course ID
     * @param userId optional student reference ID (null for guest access)
     * @return list of modules with lessons
     */
    @GetMapping("/courses/{courseId}/modules")
    @Operation(summary = "Get course structure",
               description = "Guest: returns trial lessons only. Student: returns all lessons (requires enrollment).")
    public ApiResponse<List<CourseModuleDetailResponse>> getCourseStructure(
            @PathVariable Long courseId,
            @Parameter(description = "Student reference ID (optional for guest access)")
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
     * Dual-mode: guest (X-User-Reference-Id missing) vs student (present).
     *
     * @param lessonId the lesson ID
     * @param userId optional student reference ID (null for guest access)
     * @return lesson detail with resources
     */
    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Get lesson detail",
               description = "Guest: trial lessons only. Student: all lessons (requires enrollment for paid lessons).")
    public ApiResponse<LessonDetailResponse> getLesson(
            @PathVariable Long lessonId,
            @Parameter(description = "Student reference ID (optional for guest access)")
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
     * Create a new module for a course (teacher/owner/admin only).
     *
     * @param courseId the course ID
     * @param request create module request
     * @return created module response
     */
    @PostMapping("/courses/{courseId}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Create course module (teacher only)",
               description = "Only the course owner (or owner/admin) can create modules. "
                       + "Order number must be unique within course.")
    public ApiResponse<CourseModuleResponse> createModule(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateCourseModuleRequest request) {

        Long teacherId = actingTeacherId();
        log.info("POST /api/v1/lms/courses/{}/modules - teacherId: {}", courseId, teacherId);
        return ApiResponse.success(lmsService.createModule(courseId, request, teacherId));
    }

    /**
     * Update an existing module (teacher/owner/admin only).
     *
     * @param moduleId the module ID
     * @param request update module request
     * @return updated module response
     */
    @PutMapping("/modules/{moduleId}")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Update course module (teacher only)",
               description = "Only the course owner (or owner/admin) can update modules.")
    public ApiResponse<CourseModuleResponse> updateModule(
            @PathVariable Long moduleId,
            @Valid @RequestBody UpdateCourseModuleRequest request) {

        Long teacherId = actingTeacherId();
        log.info("PUT /api/v1/lms/modules/{} - teacherId: {}", moduleId, teacherId);
        return ApiResponse.success(lmsService.updateModule(moduleId, request, teacherId));
    }

    /**
     * Delete a module (teacher/owner/admin only).
     *
     * @param moduleId the module ID
     * @return success response
     */
    @DeleteMapping("/modules/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Delete course module (teacher only)",
               description = "Only the course owner (or owner/admin) can delete modules. "
                       + "Cannot delete if module has lessons.")
    public ApiResponse<Void> deleteModule(
            @PathVariable Long moduleId) {

        Long teacherId = actingTeacherId();
        log.info("DELETE /api/v1/lms/modules/{} - teacherId: {}", moduleId, teacherId);
        lmsService.deleteModule(moduleId, teacherId);
        return ApiResponse.success(null);
    }

    /**
     * Get module detail (teacher/owner/admin only).
     *
     * @param moduleId the module ID
     * @return module detail with all lessons
     */
    @GetMapping("/modules/{moduleId}")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Get module detail (teacher only)",
               description = "Only the course owner (or owner/admin) can view module details.")
    public ApiResponse<CourseModuleDetailResponse> getModule(
            @PathVariable Long moduleId) {

        Long teacherId = actingTeacherId();
        log.info("GET /api/v1/lms/modules/{} - teacherId: {}", moduleId, teacherId);
        return ApiResponse.success(lmsService.getModule(moduleId, teacherId));
    }

    // ==================== Teacher Endpoints - Lesson CRUD ====================

    /**
     * Create a new lesson for a module (teacher/owner/admin only).
     *
     * @param moduleId the module ID
     * @param request create lesson request
     * @return created lesson response
     */
    @PostMapping("/modules/{moduleId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Create lesson (teacher only)",
               description = "Only the course owner (or owner/admin) can create lessons. "
                       + "Order number must be unique within module.")
    public ApiResponse<LessonResponse> createLesson(
            @PathVariable Long moduleId,
            @Valid @RequestBody CreateLessonRequest request) {

        Long teacherId = actingTeacherId();
        log.info("POST /api/v1/lms/modules/{}/lessons - teacherId: {}", moduleId, teacherId);
        return ApiResponse.success(lmsService.createLesson(moduleId, request, teacherId));
    }

    /**
     * Update an existing lesson (teacher/owner/admin only).
     *
     * @param lessonId the lesson ID
     * @param request update lesson request
     * @return updated lesson response
     */
    @PutMapping("/lessons/{lessonId}/manage")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Update lesson (teacher only)",
               description = "Only the course owner (or owner/admin) can update lessons.")
    public ApiResponse<LessonResponse> updateLesson(
            @PathVariable Long lessonId,
            @Valid @RequestBody UpdateLessonRequest request) {

        Long teacherId = actingTeacherId();
        log.info("PUT /api/v1/lms/lessons/{}/manage - teacherId: {}", lessonId, teacherId);
        return ApiResponse.success(lmsService.updateLesson(lessonId, request, teacherId));
    }

    /**
     * Delete a lesson (teacher/owner/admin only).
     *
     * @param lessonId the lesson ID
     * @return success response
     */
    @DeleteMapping("/lessons/{lessonId}/manage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Delete lesson (teacher only)",
               description = "Only the course owner (or owner/admin) can delete lessons.")
    public ApiResponse<Void> deleteLesson(
            @PathVariable Long lessonId) {

        Long teacherId = actingTeacherId();
        log.info("DELETE /api/v1/lms/lessons/{}/manage - teacherId: {}", lessonId, teacherId);
        lmsService.deleteLesson(lessonId, teacherId);
        return ApiResponse.success(null);
    }

    /**
     * Get lesson detail for teacher (teacher/owner/admin only).
     *
     * @param lessonId the lesson ID
     * @return lesson detail with resources
     */
    @GetMapping("/lessons/{lessonId}/manage")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Get lesson detail (teacher only)",
               description = "Only the course owner (or owner/admin) can view lesson management details.")
    public ApiResponse<LessonDetailResponse> getLessonForTeacher(
            @PathVariable Long lessonId) {

        Long teacherId = actingTeacherId();
        log.info("GET /api/v1/lms/lessons/{}/manage - teacherId: {}", lessonId, teacherId);
        return ApiResponse.success(lmsService.getLessonForTeacher(lessonId, teacherId));
    }

    // ==================== Teacher Endpoints - Learning Resource CRUD ====================

    /**
     * Add a learning resource to a lesson (teacher/owner/admin only).
     *
     * @param lessonId the lesson ID
     * @param request create resource request
     * @return created resource response
     */
    @PostMapping("/lessons/{lessonId}/resources")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Add learning resource (teacher only)",
               description = "Only the course owner (or owner/admin) can add resources to lessons.")
    public ApiResponse<LearningResourceResponse> addResource(
            @PathVariable Long lessonId,
            @Valid @RequestBody CreateLearningResourceRequest request) {

        Long teacherId = actingTeacherId();
        log.info("POST /api/v1/lms/lessons/{}/resources - teacherId: {}", lessonId, teacherId);
        return ApiResponse.success(lmsService.addResource(lessonId, request, teacherId));
    }

    /**
     * Delete a learning resource (teacher/owner/admin only).
     *
     * @param resourceId the resource ID
     * @return success response
     */
    @DeleteMapping("/resources/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Delete learning resource (teacher only)",
               description = "Only the course owner (or owner/admin) can delete resources.")
    public ApiResponse<Void> deleteResource(
            @PathVariable Long resourceId) {

        Long teacherId = actingTeacherId();
        log.info("DELETE /api/v1/lms/resources/{} - teacherId: {}", resourceId, teacherId);
        lmsService.deleteResource(resourceId, teacherId);
        return ApiResponse.success(null);
    }

    // ==================== Teacher Endpoints - Reorder (drag-drop) ====================

    /**
     * Reorder all modules of a course atomically (teacher/owner/admin only).
     * Send the FULL ordered set of the course's modules with their new order numbers.
     *
     * @param courseId the course ID
     * @param request full ordered set of modules with new order numbers
     * @return reordered modules (ascending)
     */
    @PutMapping("/courses/{courseId}/modules/reorder")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Reorder course modules (teacher only)",
               description = "Atomic batch update of module order numbers. Send the FULL ordered set of modules.")
    public ApiResponse<List<CourseModuleResponse>> reorderModules(
            @PathVariable Long courseId,
            @Valid @RequestBody ReorderRequest request) {

        Long teacherId = actingTeacherId();
        log.info("PUT /api/v1/lms/courses/{}/modules/reorder - teacherId: {}", courseId, teacherId);
        return ApiResponse.success(
                lmsService.reorderModules(courseId, request, teacherId), "Modules reordered successfully");
    }

    /**
     * Reorder all lessons within a module atomically (teacher/owner/admin only).
     * Send the FULL ordered set of the module's lessons with their new order numbers.
     *
     * @param moduleId the module ID
     * @param request full ordered set of lessons with new order numbers
     * @return reordered lessons (ascending)
     */
    @PutMapping("/modules/{moduleId}/lessons/reorder")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Reorder lessons within a module (teacher only)",
               description = "Atomic batch update of lesson order numbers. Send the FULL ordered set of lessons.")
    public ApiResponse<List<LessonResponse>> reorderLessons(
            @PathVariable Long moduleId,
            @Valid @RequestBody ReorderRequest request) {

        Long teacherId = actingTeacherId();
        log.info("PUT /api/v1/lms/modules/{}/lessons/reorder - teacherId: {}", moduleId, teacherId);
        return ApiResponse.success(
                lmsService.reorderLessons(moduleId, request, teacherId), "Lessons reordered successfully");
    }

    // ==================== Teacher Endpoints - Resource Upload (presigned) ====================

    /**
     * Request a presigned upload URL for a lesson learning-resource file (teacher/owner/admin only).
     *
     * <p>Reuses the central storage pipeline (MinIO/S3). Client PUTs the file to the
     * returned URL, confirms via the storage API, then calls
     * {@code POST /lessons/{lessonId}/resources} to persist the resource metadata.
     *
     * @param lessonId the lesson the resource will belong to
     * @param request file metadata (name, size, mime, type, access level)
     * @return presigned upload response (fileId + uploadUrl + expiresAt)
     */
    @PostMapping("/lessons/{lessonId}/resources/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Request presigned upload URL for a lesson resource (teacher only)",
               description = "Returns a presigned PUT URL (MinIO/S3). Client uploads the file, confirms, "
                       + "then POST /lessons/{lessonId}/resources to persist the resource metadata.")
    public ApiResponse<PresignedUploadResponse> requestResourceUploadUrl(
            @PathVariable Long lessonId,
            @Valid @RequestBody PresignedUploadRequest request) {

        Long teacherId = actingTeacherId();
        log.info("POST /api/v1/lms/lessons/{}/resources/upload-url - teacherId: {}", lessonId, teacherId);
        return ApiResponse.success(lmsService.generateResourceUploadUrl(lessonId, request, teacherId));
    }

    // ==================== Teacher Endpoints - Completion Roster ====================

    /**
     * Get the completion roster for a course (teacher/owner/admin only).
     * Returns per-student lesson-completion summary. Only the course owner can view.
     *
     * @param courseId the course ID
     * @return completion roster
     */
    @GetMapping("/courses/{courseId}/completion-roster")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    @Operation(summary = "Get completion roster for a course (teacher only)",
               description = "Per-student lesson-completion summary. Only the course owner (or owner/admin) can view it.")
    public ApiResponse<CompletionRosterResponse> getCompletionRoster(
            @PathVariable Long courseId) {

        Long teacherId = actingTeacherId();
        log.info("GET /api/v1/lms/courses/{}/completion-roster - teacherId: {}", courseId, teacherId);
        return ApiResponse.success(lmsService.getCompletionRoster(courseId, teacherId));
    }
}
