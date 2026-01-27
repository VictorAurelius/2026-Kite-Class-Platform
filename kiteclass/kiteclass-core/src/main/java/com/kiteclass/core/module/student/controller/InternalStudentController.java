package com.kiteclass.core.module.student.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.service.StudentService;
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
 * Internal REST API controller for Student operations (Service-to-Service only).
 *
 * <p>This controller provides internal APIs for Gateway service to:
 * <ul>
 *   <li>Retrieve student profiles for authentication context</li>
 *   <li>Create student records during user registration</li>
 *   <li>Delete student records when user account is deleted</li>
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
 * @since 2.11.0
 */
@Slf4j
@RestController
@RequestMapping("/internal/students")
@RequiredArgsConstructor
@Hidden  // Hide from public Swagger UI
@Tag(name = "Internal Student API", description = "Internal APIs for service-to-service communication (Gateway only)")
public class InternalStudentController {

    private final StudentService studentService;

    /**
     * Get student profile by ID (Internal API).
     *
     * <p>Called by Gateway during login flow to retrieve student profile
     * when {@code userType = STUDENT}.
     *
     * <p><strong>Authentication:</strong> Requires {@code X-Internal-Request: true} header.
     *
     * @param id the student ID (matches {@code User.referenceId} in Gateway)
     * @param internalHeader the internal request header (validated by filter)
     * @return the student profile wrapped in ApiResponse
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get student profile by ID (Internal)",
               description = "Retrieve student profile for Gateway authentication flow. " +
                           "Requires X-Internal-Request header.")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(
            @PathVariable Long id,
            @RequestHeader("X-Internal-Request") String internalHeader) {

        log.info("Internal API: Get student profile, id={}", id);

        StudentResponse student = studentService.getStudentById(id);

        log.debug("Internal API: Student profile retrieved successfully, id={}", id);

        return ResponseEntity.ok(ApiResponse.success(student));
    }

    /**
     * Create student record (Internal API).
     *
     * <p>Called by Gateway during student registration flow to create
     * student profile in Core service.
     *
     * <p><strong>Flow:</strong>
     * <ol>
     *   <li>Gateway creates User record (without referenceId)</li>
     *   <li>Gateway calls this endpoint to create Student in Core</li>
     *   <li>Gateway updates User.referenceId with Student.id</li>
     * </ol>
     *
     * <p><strong>Authentication:</strong> Requires {@code X-Internal-Request: true} header.
     *
     * @param request the student creation request
     * @param internalHeader the internal request header (validated by filter)
     * @return the created student profile wrapped in ApiResponse
     */
    @PostMapping
    @Operation(summary = "Create student (Internal)",
               description = "Create student profile during Gateway registration flow. " +
                           "Requires X-Internal-Request header.")
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request,
            @RequestHeader("X-Internal-Request") String internalHeader) {

        log.info("Internal API: Create student, email={}", request.email());

        StudentResponse student = studentService.createStudent(request);

        log.info("Internal API: Student created successfully, id={}, email={}",
                student.id(), student.email());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(student));
    }

    /**
     * Delete student record (Internal API).
     *
     * <p>Called by Gateway when user account is deleted. Performs soft delete
     * of student record in Core service.
     *
     * <p><strong>Note:</strong> This is a soft delete. The student record is
     * marked as deleted but not removed from database.
     *
     * <p><strong>Authentication:</strong> Requires {@code X-Internal-Request: true} header.
     *
     * @param id the student ID to delete
     * @param internalHeader the internal request header (validated by filter)
     * @return success response wrapped in ApiResponse
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete student (Internal)",
               description = "Soft delete student when Gateway user account is deleted. " +
                           "Requires X-Internal-Request header.")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(
            @PathVariable Long id,
            @RequestHeader("X-Internal-Request") String internalHeader) {

        log.info("Internal API: Delete student, id={}", id);

        studentService.deleteStudent(id);

        log.info("Internal API: Student deleted successfully, id={}", id);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
