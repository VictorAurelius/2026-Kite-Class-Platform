package com.kiteclass.core.module.enrollment.controller;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.dto.EnrollmentResponse;
import com.kiteclass.core.module.enrollment.dto.UpdateEnrollmentStatusRequest;
import com.kiteclass.core.module.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for enrollment management.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Enrolling students in classes</li>
 *   <li>Managing enrollment status</li>
 *   <li>Querying enrollments</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Enrollment management APIs")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * Enroll a student in a class.
     *
     * @param request enrollment request data
     * @return created enrollment
     */
    @PostMapping
    @Operation(summary = "Enroll a student in a class",
               description = "Creates a new enrollment. Validates capacity and prevents duplicates.")
    public ResponseEntity<EnrollmentResponse> enrollStudent(
            @Valid @RequestBody CreateEnrollmentRequest request) {
        log.info("POST /api/v1/enrollments - Enrolling student {} in class {}",
                request.getStudentId(), request.getClassId());

        EnrollmentResponse response = enrollmentService.enrollStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get enrollment by ID.
     *
     * @param id enrollment ID
     * @return enrollment data
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get enrollment by ID")
    public ResponseEntity<EnrollmentResponse> getEnrollment(
            @Parameter(description = "Enrollment ID") @PathVariable Long id) {
        log.debug("GET /api/v1/enrollments/{}", id);

        EnrollmentResponse response = enrollmentService.getEnrollmentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all enrollments for a student.
     *
     * @param studentId student ID
     * @param pageable pagination parameters
     * @return page of enrollments
     */
    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get all enrollments for a student")
    public ResponseEntity<Page<EnrollmentResponse>> getEnrollmentsByStudent(
            @Parameter(description = "Student ID") @PathVariable Long studentId,
            @PageableDefault(sort = "enrollmentDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.debug("GET /api/v1/enrollments/student/{}", studentId);

        Page<EnrollmentResponse> response = enrollmentService.getEnrollmentsByStudent(
                studentId, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get all enrollments for a class.
     *
     * @param classId class ID
     * @param status optional status filter
     * @param pageable pagination parameters
     * @return page of enrollments
     */
    @GetMapping("/class/{classId}")
    @Operation(summary = "Get all enrollments for a class")
    public ResponseEntity<Page<EnrollmentResponse>> getEnrollmentsByClass(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @Parameter(description = "Filter by status (optional)")
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(sort = "enrollmentDate", direction = Sort.Direction.ASC)
            Pageable pageable) {
        log.debug("GET /api/v1/enrollments/class/{} with status={}", classId, status);

        Page<EnrollmentResponse> response;
        if (status != null) {
            response = enrollmentService.getEnrollmentsByClassAndStatus(
                    classId, status, pageable
            );
        } else {
            response = enrollmentService.getEnrollmentsByClass(classId, pageable);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Update enrollment status.
     *
     * @param id enrollment ID
     * @param request status update request
     * @return updated enrollment
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update enrollment status",
               description = "Updates enrollment status (e.g., PENDING_PAYMENT → ACTIVE)")
    public ResponseEntity<EnrollmentResponse> updateEnrollmentStatus(
            @Parameter(description = "Enrollment ID") @PathVariable Long id,
            @Valid @RequestBody UpdateEnrollmentStatusRequest request) {
        log.info("PUT /api/v1/enrollments/{}/status to {}", id, request.getStatus());

        EnrollmentResponse response = enrollmentService.updateEnrollmentStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Withdraw a student from a class.
     *
     * @param id enrollment ID
     * @return updated enrollment
     */
    @PutMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw a student from a class",
               description = "Sets enrollment status to WITHDRAWN")
    public ResponseEntity<EnrollmentResponse> withdrawStudent(
            @Parameter(description = "Enrollment ID") @PathVariable Long id) {
        log.info("PUT /api/v1/enrollments/{}/withdraw", id);

        EnrollmentResponse response = enrollmentService.withdrawStudent(id);
        return ResponseEntity.ok(response);
    }
}
