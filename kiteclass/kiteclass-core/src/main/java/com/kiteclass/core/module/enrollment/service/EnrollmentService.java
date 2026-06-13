package com.kiteclass.core.module.enrollment.service;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.dto.EnrollmentResponse;
import com.kiteclass.core.module.enrollment.dto.MyEnrollmentResponse;
import com.kiteclass.core.module.enrollment.dto.UpdateEnrollmentStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for enrollment management.
 *
 * <p>Provides business logic for:
 * <ul>
 *   <li>Enrolling students in classes</li>
 *   <li>Managing enrollment status</li>
 *   <li>Validating capacity and business rules</li>
 *   <li>Publishing enrollment events</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
public interface EnrollmentService {

    /**
     * Enroll a student in a class.
     *
     * <p>Business Rules Enforced:
     * <ul>
     *   <li>BR-ENROLL-001: Class must not be at full capacity</li>
     *   <li>BR-ENROLL-002: Student cannot be already enrolled in the same class</li>
     *   <li>BR-ENROLL-003: Auto-calculate final_amount with discount</li>
     *   <li>BR-ENROLL-005: Cannot enroll in cancelled or archived classes</li>
     * </ul>
     *
     * <p>Publishes ENROLLMENT_CREATED event for downstream processing (invoice generation).
     *
     * @param request enrollment request data
     * @return created enrollment
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if student or class not found
     * @throws com.kiteclass.core.common.exception.ValidationException if business rules violated
     */
    EnrollmentResponse enrollStudent(@Valid CreateEnrollmentRequest request);

    /**
     * Get enrollment by ID.
     *
     * @param id enrollment ID
     * @return enrollment data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if not found
     */
    EnrollmentResponse getEnrollmentById(Long id);

    /**
     * Get all enrollments for a student.
     *
     * @param studentId student ID
     * @param pageable pagination info
     * @return page of enrollments
     */
    Page<EnrollmentResponse> getEnrollmentsByStudent(Long studentId, Pageable pageable);

    /**
     * Get the calling student's OWN enrollments, enriched with class + course
     * names (GAP-1285).
     *
     * <p>Self-scoped: {@code studentId} is the authenticated STUDENT actor's
     * {@code students.id} (resolved by the controller from
     * {@code X-User-Reference-Id}), so this never exposes another student's
     * enrollments. Each enrollment is enriched with {@code classId} +
     * {@code className} + {@code courseId} + {@code courseName} so the kc-student
     * frontend can render "Khóa học của tôi" + "Lớp của tôi" without N+1 lookups.
     *
     * <p>Tenant isolation (Hibernate {@code tenantFilter}) applies to every query
     * — the student, classes, and courses all resolve within the current tenant.
     *
     * @param studentId calling student's {@code students.id}
     * @param pageable pagination info
     * @return page of enriched enrollment responses (empty when no enrollments)
     */
    Page<MyEnrollmentResponse> getMyEnrollments(Long studentId, Pageable pageable);

    /**
     * Get all enrollments for a class.
     *
     * @param classId class ID
     * @param pageable pagination info
     * @return page of enrollments
     */
    Page<EnrollmentResponse> getEnrollmentsByClass(Long classId, Pageable pageable);

    /**
     * Get enrollments by status.
     *
     * @param classId class ID
     * @param status enrollment status
     * @param pageable pagination info
     * @return page of enrollments
     */
    Page<EnrollmentResponse> getEnrollmentsByClassAndStatus(
            Long classId,
            EnrollmentStatus status,
            Pageable pageable
    );

    /**
     * Update enrollment status.
     *
     * <p>Common transitions:
     * <ul>
     *   <li>PENDING_PAYMENT → ACTIVE (payment confirmed)</li>
     *   <li>ACTIVE → WITHDRAWN (student withdraws)</li>
     *   <li>ACTIVE → COMPLETED (class finishes)</li>
     *   <li>PENDING_PAYMENT → CANCELLED (enrollment cancelled)</li>
     * </ul>
     *
     * @param id enrollment ID
     * @param request status update request
     * @return updated enrollment
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if enrollment not found
     */
    EnrollmentResponse updateEnrollmentStatus(
            Long id,
            @Valid UpdateEnrollmentStatusRequest request
    );

    /**
     * Withdraw a student from a class.
     *
     * <p>Sets status to WITHDRAWN and updates class current_enrolled count.
     *
     * @param id enrollment ID
     * @return updated enrollment
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if enrollment not found
     */
    EnrollmentResponse withdrawStudent(Long id);
}
