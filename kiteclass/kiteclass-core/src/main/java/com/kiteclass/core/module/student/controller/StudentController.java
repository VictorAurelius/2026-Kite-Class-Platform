package com.kiteclass.core.module.student.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.auth.dto.SetPasswordRequest;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
import com.kiteclass.core.module.student.service.StudentService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Student operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>POST /api/v1/students - Create student</li>
 *   <li>GET /api/v1/students/{id} - Get student by ID</li>
 *   <li>GET /api/v1/students - Search students</li>
 *   <li>PUT /api/v1/students/{id} - Update student</li>
 *   <li>DELETE /api/v1/students/{id} - Delete student</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Student", description = "Student management APIs")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-b", "controller", "student"})
public class StudentController {

    private final StudentService studentService;

    /**
     * Creates a new student.
     *
     * @param request the create request with student details
     * @param tenantId the tenant ID (instance ID) from X-Tenant-Id header
     * @return ApiResponse with created student data and HTTP 201
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'PRINCIPAL', 'TEACHER', 'STAFF', 'PLATFORM_ADMIN')")
    @Operation(summary = "Create a new student", description = "Creates a new student with the provided information")
    public ApiResponse<StudentResponse> createStudent(
            @Valid @RequestBody CreateStudentRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = true) java.util.UUID tenantId) {
        log.info("REST request to create student: {}, tenantId: {}", request.name(), tenantId);
        StudentResponse response = studentService.createStudent(request, tenantId);
        return ApiResponse.success(response, "Student created successfully");
    }

    /**
     * Set/reset a student's KC-native login password (KC-9 student-auth, Wave auth-1
     * Hướng B — mirrors {@code POST /api/v1/teachers/{id}/credentials}).
     *
     * <p>Owner/teacher action — provisions the student's login credential so the
     * student can log in via {@code POST /api/v1/tenant-auth/login}. No Zalo/SMS OTP.
     *
     * @param id      student id (tenant-scoped)
     * @param request the new password
     * @return ApiResponse with success message
     */
    @PostMapping("/{id}/credentials")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "Set/reset a student's login password",
            description = "Provisions KC-native login for the student (KC-9, Wave auth-1). Owner/teacher only.")
    public ApiResponse<Void> setStudentCredential(
            @Parameter(description = "Student ID") @PathVariable Long id,
            @Valid @RequestBody SetPasswordRequest request) {
        log.info("REST request to set login credential for student id={}", id);
        studentService.provisionCredential(id, request.password());
        return ApiResponse.success(null, "Đặt mật khẩu học sinh thành công");
    }

    /**
     * Retrieves a student by ID.
     *
     * @param id the student ID
     * @return ApiResponse with student data and HTTP 200
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'PRINCIPAL', 'TEACHER', 'STAFF', 'PLATFORM_ADMIN')")
    @Operation(summary = "Get student by ID", description = "Retrieves a student's information by their ID")
    public ApiResponse<StudentResponse> getStudentById(
            @Parameter(description = "Student ID") @PathVariable Long id) {
        log.debug("REST request to get student with ID: {}", id);
        StudentResponse response = studentService.getStudentById(id);
        return ApiResponse.success(response);
    }

    /**
     * Searches students with filters and pagination.
     *
     * @param search the search keyword (name or email)
     * @param status the student status filter
     * @param page   page number (0-indexed)
     * @param size   page size
     * @param sort   sort field (default: name)
     * @return ApiResponse with page of students and HTTP 200
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'PRINCIPAL', 'TEACHER', 'STAFF', 'PLATFORM_ADMIN')")
    @Operation(summary = "Search students", description = "Searches students with optional filters and pagination")
    public ApiResponse<PageResponse<StudentResponse>> getStudents(
            @Parameter(description = "Search keyword (name or email)") @RequestParam(required = false) String search,
            @Parameter(description = "Student status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'name,asc' or 'name,desc')") @RequestParam(defaultValue = "name") String sort) {
        log.debug("REST request to search students: search='{}', status='{}', page={}, size={}", search, status, page, size);

        // Parse sort string (format: "field,direction")
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1]) ?
                Sort.Direction.DESC : Sort.Direction.ASC;

        // Convert camelCase to snake_case for native SQL queries
        String dbColumnName = toSnakeCase(sortField);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, dbColumnName));
        PageResponse<StudentResponse> response = studentService.getStudents(search, status, pageable);

        return ApiResponse.success(response);
    }

    /**
     * Converts camelCase field name to snake_case database column name.
     * Used for native SQL queries where JPA naming strategy doesn't apply.
     *
     * @param camelCase the camelCase field name
     * @return the snake_case column name
     */
    private String toSnakeCase(String camelCase) {
        // Convert camelCase to snake_case using regex
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Updates an existing student.
     *
     * @param id      the student ID
     * @param request the update request with new values
     * @return ApiResponse with updated student data and HTTP 200
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'PRINCIPAL', 'TEACHER', 'STAFF', 'PLATFORM_ADMIN')")
    @Operation(summary = "Update student", description = "Updates an existing student's information")
    public ApiResponse<StudentResponse> updateStudent(
            @Parameter(description = "Student ID") @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        log.info("REST request to update student with ID: {}", id);
        StudentResponse response = studentService.updateStudent(id, request);
        return ApiResponse.success(response, "Student updated successfully");
    }

    /**
     * Soft-deletes a student.
     *
     * @param id the student ID
     * @return ApiResponse with success message and HTTP 200
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'PRINCIPAL', 'TEACHER', 'STAFF', 'PLATFORM_ADMIN')")
    @Operation(summary = "Delete student", description = "Soft-deletes a student (sets deleted flag)")
    public ApiResponse<Void> deleteStudent(
            @Parameter(description = "Student ID") @PathVariable Long id) {
        log.info("REST request to delete student with ID: {}", id);
        studentService.deleteStudent(id);
        return ApiResponse.success(null, "Student deleted successfully");
    }
}
