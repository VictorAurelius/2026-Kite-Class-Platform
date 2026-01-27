package com.kiteclass.core.module.student.service;

import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Student business logic.
 *
 * <p>Provides operations for:
 * <ul>
 *   <li>Creating new students</li>
 *   <li>Retrieving students (by ID or with search filters)</li>
 *   <li>Updating student information</li>
 *   <li>Soft-deleting students</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
public interface StudentService {

    /**
     * Creates a new student.
     *
     * <p>Validates that email and phone are unique.
     * Sets initial status to ACTIVE.
     *
     * @param request the create request with student details
     * @return StudentResponse with created student data
     * @throws com.kiteclass.core.common.exception.DuplicateResourceException if email or phone already exists
     */
    StudentResponse createStudent(CreateStudentRequest request);

    /**
     * Retrieves a student by ID.
     *
     * @param id the student ID
     * @return StudentResponse with student data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if student not found or deleted
     */
    StudentResponse getStudentById(Long id);

    /**
     * Searches students with filters and pagination.
     *
     * <p>Search criteria:
     * <ul>
     *   <li>search: matches name or email (case-insensitive)</li>
     *   <li>status: filters by student status (null = all statuses)</li>
     * </ul>
     *
     * @param search  the search keyword (can be null)
     * @param status  the student status filter (can be null)
     * @param pageable pagination parameters
     * @return PageResponse with matching students
     */
    PageResponse<StudentResponse> getStudents(String search, String status, Pageable pageable);

    /**
     * Updates an existing student.
     *
     * <p>Only updates non-null fields from request.
     * Validates email/phone uniqueness if changed.
     *
     * @param id      the student ID
     * @param request the update request with new values
     * @return StudentResponse with updated student data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if student not found
     * @throws com.kiteclass.core.common.exception.DuplicateResourceException if new email/phone already exists
     */
    StudentResponse updateStudent(Long id, UpdateStudentRequest request);

    /**
     * Soft-deletes a student.
     *
     * <p>Sets deleted flag to true instead of physically removing the record.
     * Deleted students are excluded from normal queries.
     *
     * @param id the student ID
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if student not found
     */
    void deleteStudent(Long id);
}
