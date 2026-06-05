package com.kiteclass.core.module.teacher.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.auth.dto.SetPasswordRequest;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.TeacherResponse;
import com.kiteclass.core.module.teacher.dto.UpdateTeacherRequest;
import com.kiteclass.core.module.teacher.service.TeacherService;
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
 * REST controller for Teacher operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>POST /api/v1/teachers - Create teacher</li>
 *   <li>GET /api/v1/teachers/{id} - Get teacher by ID</li>
 *   <li>GET /api/v1/teachers - Search teachers</li>
 *   <li>PUT /api/v1/teachers/{id} - Update teacher</li>
 *   <li>DELETE /api/v1/teachers/{id} - Delete teacher</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "Teacher", description = "Teacher management APIs")
public class TeacherController {

    private final TeacherService teacherService;

    /**
     * Creates a new teacher.
     *
     * @param request the create request with teacher details
     * @return ApiResponse with created teacher data and HTTP 201
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new teacher", description = "Creates a new teacher with the provided information")
    public ApiResponse<TeacherResponse> createTeacher(@Valid @RequestBody CreateTeacherRequest request) {
        log.info("REST request to create teacher: {}", request.name());
        TeacherResponse response = teacherService.createTeacher(request);
        return ApiResponse.success(response, "Teacher created successfully");
    }

    /**
     * Set/reset a teacher's KC-native login password (Wave auth-1, Hướng B — GAP-725).
     * Admin/owner only — provisions the teacher's login credential.
     *
     * @param id      teacher id (tenant-scoped)
     * @param request the new password
     * @return ApiResponse with success message
     */
    @PostMapping("/{id}/credentials")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','PRINCIPAL')")
    @Operation(summary = "Set/reset a teacher's login password",
            description = "Provisions KC-native login for the teacher (Wave auth-1). Admin/owner only.")
    public ApiResponse<Void> setTeacherCredential(
            @PathVariable Long id,
            @Valid @RequestBody SetPasswordRequest request) {
        log.info("REST request to set login credential for teacher id={}", id);
        teacherService.provisionCredential(id, request.password());
        return ApiResponse.success(null, "Đặt mật khẩu giáo viên thành công");
    }

    /**
     * Retrieves a teacher by ID.
     *
     * @param id the teacher ID
     * @return ApiResponse with teacher data and HTTP 200
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get teacher by ID", description = "Retrieves a teacher's information by their ID")
    public ApiResponse<TeacherResponse> getTeacherById(
            @Parameter(description = "Teacher ID") @PathVariable Long id) {
        log.debug("REST request to get teacher with ID: {}", id);
        TeacherResponse response = teacherService.getTeacherById(id);
        return ApiResponse.success(response);
    }

    /**
     * Searches teachers with filters and pagination.
     *
     * @param search the search keyword (name, email, or specialization)
     * @param status the teacher status filter
     * @param page   page number (0-indexed)
     * @param size   page size
     * @param sort   sort field (default: name)
     * @return ApiResponse with page of teachers and HTTP 200
     */
    @GetMapping
    @Operation(summary = "Search teachers", description = "Searches teachers with optional filters and pagination")
    public ApiResponse<PageResponse<TeacherResponse>> getTeachers(
            @Parameter(description = "Search keyword (name, email, or specialization)") @RequestParam(required = false) String search,
            @Parameter(description = "Teacher status filter (ACTIVE, INACTIVE, ON_LEAVE)") @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'name,asc' or 'name,desc')") @RequestParam(defaultValue = "name") String sort) {
        log.debug("REST request to search teachers: search='{}', status='{}', page={}, size={}", search, status, page, size);

        // Parse sort string (format: "field,direction")
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1]) ?
                Sort.Direction.DESC : Sort.Direction.ASC;

        // Convert camelCase to snake_case for native SQL queries
        String dbColumnName = toSnakeCase(sortField);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, dbColumnName));
        PageResponse<TeacherResponse> response = teacherService.getTeachers(search, status, pageable);

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
     * Updates an existing teacher.
     *
     * @param id      the teacher ID
     * @param request the update request with new values
     * @return ApiResponse with updated teacher data and HTTP 200
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update teacher", description = "Updates an existing teacher's information")
    public ApiResponse<TeacherResponse> updateTeacher(
            @Parameter(description = "Teacher ID") @PathVariable Long id,
            @Valid @RequestBody UpdateTeacherRequest request) {
        log.info("REST request to update teacher with ID: {}", id);
        TeacherResponse response = teacherService.updateTeacher(id, request);
        return ApiResponse.success(response, "Teacher updated successfully");
    }

    /**
     * Soft-deletes a teacher.
     *
     * @param id the teacher ID
     * @return ApiResponse with success message and HTTP 200
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete teacher", description = "Soft-deletes a teacher (sets deleted flag)")
    public ApiResponse<Void> deleteTeacher(
            @Parameter(description = "Teacher ID") @PathVariable Long id) {
        log.info("REST request to delete teacher with ID: {}", id);
        teacherService.deleteTeacher(id);
        return ApiResponse.success(null, "Teacher deleted successfully");
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search teachers by specialization",
            description = "Searches teachers by specialization with partial match and case-insensitive"
    )
    public ApiResponse<PageResponse<TeacherResponse>> searchBySpecialization(
            @Parameter(description = "Specialization search query") @RequestParam String specialization,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") String direction) {

        log.info("REST request to search teachers by specialization: {}", specialization);

        PageResponse<TeacherResponse> results = teacherService.searchBySpecialization(
                specialization, page, size, sortBy, direction);

        return ApiResponse.success(results);
    }
}
