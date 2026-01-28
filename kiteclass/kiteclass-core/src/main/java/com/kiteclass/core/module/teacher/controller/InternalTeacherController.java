package com.kiteclass.core.module.teacher.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.TeacherResponse;
import com.kiteclass.core.module.teacher.service.TeacherService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal REST API controller for Teacher operations (Service-to-Service only).
 *
 * <p>This controller provides internal APIs for Gateway service to:
 * <ul>
 *   <li>Retrieve teacher profiles for authentication context</li>
 *   <li>Create teacher records during user registration</li>
 *   <li>Delete teacher records when user account is deleted</li>
 * </ul>
 *
 * <p><strong>Security:</strong> These endpoints are protected by {@link com.kiteclass.core.config.InternalRequestFilter}
 * and require {@code X-Internal-Request: true} header. They are NOT accessible
 * from public internet.
 *
 * <p><strong>Note:</strong> These endpoints are hidden from public Swagger documentation
 * using {@code @Hidden} annotation.
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Slf4j
@RestController
@RequestMapping("/internal/teachers")
@RequiredArgsConstructor
@Hidden  // Hide from public Swagger UI
@Tag(name = "Internal Teacher API", description = "Internal APIs for service-to-service communication (Gateway only)")
public class InternalTeacherController {

    private final TeacherService teacherService;

    /**
     * Get teacher profile by ID (Internal API).
     *
     * <p>Called by Gateway during login flow to retrieve teacher profile
     * when {@code userType = TEACHER}.
     *
     * <p><strong>Authentication:</strong> Requires {@code X-Internal-Request: true} header.
     *
     * @param id the teacher ID (matches {@code User.referenceId} in Gateway)
     * @param internalHeader the internal request header (validated by filter)
     * @return the teacher profile wrapped in ApiResponse
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get teacher profile by ID (Internal)",
               description = "Retrieve teacher profile for Gateway authentication flow. " +
                           "Requires X-Internal-Request header.")
    public ResponseEntity<ApiResponse<TeacherResponse>> getTeacher(
            @PathVariable Long id,
            @RequestHeader("X-Internal-Request") String internalHeader) {

        log.info("Internal API: Get teacher profile, id={}", id);

        TeacherResponse teacher = teacherService.getTeacherById(id);

        log.debug("Internal API: Teacher profile retrieved successfully, id={}", id);

        return ResponseEntity.ok(ApiResponse.success(teacher));
    }

    /**
     * Create teacher record (Internal API).
     *
     * <p>Called by Gateway during teacher registration flow to create
     * teacher profile in Core service.
     *
     * <p><strong>Flow:</strong>
     * <ol>
     *   <li>Gateway creates User record (without referenceId)</li>
     *   <li>Gateway calls this endpoint to create Teacher in Core</li>
     *   <li>Gateway updates User.referenceId with Teacher.id</li>
     * </ol>
     *
     * <p><strong>Authentication:</strong> Requires {@code X-Internal-Request: true} header.
     *
     * @param request the teacher creation request
     * @param internalHeader the internal request header (validated by filter)
     * @return the created teacher profile wrapped in ApiResponse
     */
    @PostMapping
    @Operation(summary = "Create teacher (Internal)",
               description = "Create teacher profile during Gateway registration flow. " +
                           "Requires X-Internal-Request header.")
    public ResponseEntity<ApiResponse<TeacherResponse>> createTeacher(
            @Valid @RequestBody CreateTeacherRequest request,
            @RequestHeader("X-Internal-Request") String internalHeader) {

        log.info("Internal API: Create teacher, email={}", request.email());

        TeacherResponse teacher = teacherService.createTeacher(request);

        log.info("Internal API: Teacher created successfully, id={}, email={}",
                teacher.id(), teacher.email());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(teacher));
    }

    /**
     * Delete teacher record (Internal API).
     *
     * <p>Called by Gateway when user account is deleted. Performs soft delete
     * of teacher record in Core service.
     *
     * <p><strong>Note:</strong> This is a soft delete. The teacher record is
     * marked as deleted but not removed from database.
     *
     * <p><strong>Authentication:</strong> Requires {@code X-Internal-Request: true} header.
     *
     * @param id the teacher ID to delete
     * @param internalHeader the internal request header (validated by filter)
     * @return success response wrapped in ApiResponse
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete teacher (Internal)",
               description = "Soft delete teacher when Gateway user account is deleted. " +
                           "Requires X-Internal-Request header.")
    public ResponseEntity<ApiResponse<Void>> deleteTeacher(
            @PathVariable Long id,
            @RequestHeader("X-Internal-Request") String internalHeader) {

        log.info("Internal API: Delete teacher, id={}", id);

        teacherService.deleteTeacher(id);

        log.info("Internal API: Teacher deleted successfully, id={}", id);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
