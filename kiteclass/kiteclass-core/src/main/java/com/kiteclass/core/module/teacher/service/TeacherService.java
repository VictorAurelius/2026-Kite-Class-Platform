package com.kiteclass.core.module.teacher.service;

import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.TeacherResponse;
import com.kiteclass.core.module.teacher.dto.UpdateTeacherRequest;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Teacher business logic.
 *
 * <p>Provides operations for:
 * <ul>
 *   <li>Creating new teachers</li>
 *   <li>Retrieving teachers (by ID or with search filters)</li>
 *   <li>Updating teacher information</li>
 *   <li>Soft-deleting teachers</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
public interface TeacherService {

    /**
     * Creates a new teacher.
     *
     * <p>Validates that email is unique (BR-TEACHER-001).
     * Sets initial status to ACTIVE.
     *
     * @param request the create request with teacher details
     * @return TeacherResponse with created teacher data
     * @throws com.kiteclass.core.common.exception.DuplicateResourceException if email already exists
     */
    TeacherResponse createTeacher(CreateTeacherRequest request);

    /**
     * Retrieves a teacher by ID.
     *
     * @param id the teacher ID
     * @return TeacherResponse with teacher data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if teacher not found or deleted
     */
    TeacherResponse getTeacherById(Long id);

    /**
     * Searches teachers with filters and pagination.
     *
     * <p>Search criteria:
     * <ul>
     *   <li>search: matches name, email, or specialization (case-insensitive)</li>
     *   <li>status: filters by teacher status (null = all statuses)</li>
     * </ul>
     *
     * @param search  the search keyword (can be null)
     * @param status  the teacher status filter (can be null)
     * @param pageable pagination parameters
     * @return PageResponse with matching teachers
     */
    PageResponse<TeacherResponse> getTeachers(String search, String status, Pageable pageable);

    /**
     * Updates an existing teacher.
     *
     * <p>Only updates non-null fields from request.
     * Validates email uniqueness if changed.
     * Note: Email cannot be changed after creation (ignored in mapper).
     *
     * @param id      the teacher ID
     * @param request the update request with new values
     * @return TeacherResponse with updated teacher data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if teacher not found
     */
    TeacherResponse updateTeacher(Long id, UpdateTeacherRequest request);

    /**
     * Soft-deletes a teacher.
     *
     * <p>Sets deleted flag to true instead of physically removing the record.
     * Deleted teachers are excluded from normal queries.
     *
     * @param id the teacher ID
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if teacher not found
     */
    void deleteTeacher(Long id);
}
