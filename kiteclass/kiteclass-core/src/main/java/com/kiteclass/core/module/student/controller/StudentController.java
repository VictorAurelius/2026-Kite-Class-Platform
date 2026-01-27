package com.kiteclass.core.module.student.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
import com.kiteclass.core.module.student.service.StudentService;
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
public class StudentController {

    private final StudentService studentService;

    /**
     * Creates a new student.
     *
     * @param request the create request with student details
     * @return ApiResponse with created student data and HTTP 201
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new student", description = "Creates a new student with the provided information")
    public ApiResponse<StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        log.info("REST request to create student: {}", request.name());
        StudentResponse response = studentService.createStudent(request);
        return ApiResponse.success(response, "Student created successfully");
    }

    /**
     * Retrieves a student by ID.
     *
     * @param id the student ID
     * @return ApiResponse with student data and HTTP 200
     */
    @GetMapping("/{id}")
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
    @Operation(summary = "Search students", description = "Searches students with optional filters and pagination")
    public ApiResponse<PageResponse<StudentResponse>> getStudents(
            @Parameter(description = "Search keyword (name or email)") @RequestParam(required = false) String search,
            @Parameter(description = "Student status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sort) {
        log.debug("REST request to search students: search='{}', status='{}', page={}, size={}", search, status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        PageResponse<StudentResponse> response = studentService.getStudents(search, status, pageable);

        return ApiResponse.success(response);
    }

    /**
     * Updates an existing student.
     *
     * @param id      the student ID
     * @param request the update request with new values
     * @return ApiResponse with updated student data and HTTP 200
     */
    @PutMapping("/{id}")
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
    @Operation(summary = "Delete student", description = "Soft-deletes a student (sets deleted flag)")
    public ApiResponse<Void> deleteStudent(
            @Parameter(description = "Student ID") @PathVariable Long id) {
        log.info("REST request to delete student with ID: {}", id);
        studentService.deleteStudent(id);
        return ApiResponse.success(null, "Student deleted successfully");
    }
}
