package com.kiteclass.core.module.attendance.service;

import com.kiteclass.core.module.attendance.dto.AttendanceResponse;
import com.kiteclass.core.module.attendance.dto.AttendanceStatsResponse;
import com.kiteclass.core.module.attendance.dto.BulkAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.CreateAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.UpdateAttendanceStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for attendance management.
 *
 * <p>Provides business logic for:
 * <ul>
 *   <li>Marking student attendance for class sessions</li>
 *   <li>Bulk attendance marking</li>
 *   <li>Attendance status updates</li>
 *   <li>Attendance statistics and reporting</li>
 *   <li>Points integration with gamification system</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
public interface AttendanceService {

    /**
     * Mark attendance for a single student.
     *
     * <p>Business Rules Enforced:
     * <ul>
     *   <li>BR-ATTEND-001: Enrollment must exist and be ACTIVE</li>
     *   <li>BR-ATTEND-002: Session must exist and not be COMPLETED/CANCELLED</li>
     *   <li>BR-ATTEND-003: Cannot mark duplicate attendance</li>
     *   <li>BR-ATTEND-004: Only MAIN_TEACHER can mark attendance</li>
     *   <li>BR-ATTEND-005: Auto-detect LATE if marked > 15 min after session start</li>
     *   <li>BR-ATTEND-006: Calculate and award/deduct points</li>
     * </ul>
     *
     * @param request attendance marking request
     * @return created attendance record
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if enrollment or session not found
     * @throws com.kiteclass.core.common.exception.ValidationException if business rules violated
     */
    AttendanceResponse markAttendance(@Valid CreateAttendanceRequest request);

    /**
     * Mark attendance for multiple students in a session (bulk operation).
     *
     * <p>Business Rules Enforced:
     * <ul>
     *   <li>BR-ATTEND-004: Only MAIN_TEACHER can mark attendance</li>
     *   <li>BR-ATTEND-001: All enrollments must exist and be ACTIVE</li>
     *   <li>BR-ATTEND-002: Session must exist and belong to the class</li>
     *   <li>BR-ATTEND-003: Cannot mark duplicate attendance</li>
     * </ul>
     *
     * <p>Updates session.attendanceTaken flag after successful completion.
     * Publishes AttendanceMarkedEvent for future Grade Module integration.
     *
     * @param classId class ID
     * @param sessionId session ID
     * @param request bulk attendance request
     * @param teacherId teacher ID who is marking attendance (must be MAIN_TEACHER)
     * @return list of created attendance records
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if session not found
     * @throws com.kiteclass.core.common.exception.ValidationException if business rules violated
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not MAIN_TEACHER
     */
    List<AttendanceResponse> markBulkAttendance(
        Long classId,
        Long sessionId,
        @Valid BulkAttendanceRequest request,
        Long teacherId
    );

    /**
     * Get attendance record by ID.
     *
     * @param id attendance ID
     * @return attendance record
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if not found
     */
    AttendanceResponse getAttendanceById(Long id);

    /**
     * Get all attendance records for an enrollment (student's attendance history).
     *
     * @param enrollmentId enrollment ID
     * @param pageable pagination info
     * @return page of attendance records
     */
    Page<AttendanceResponse> getAttendanceByEnrollment(Long enrollmentId, Pageable pageable);

    /**
     * Get all attendance records for a session (session roster).
     *
     * @param sessionId session ID
     * @param pageable pagination info
     * @return page of attendance records
     */
    Page<AttendanceResponse> getAttendanceBySession(Long sessionId, Pageable pageable);

    /**
     * Update attendance status.
     *
     * <p>Business Rules Enforced:
     * <ul>
     *   <li>BR-ATTEND-004: Only MAIN_TEACHER can update attendance</li>
     * </ul>
     *
     * <p>Recalculates points and updates student_points table.
     *
     * @param id attendance ID
     * @param request update request
     * @param teacherId teacher ID who is updating attendance (must be MAIN_TEACHER)
     * @return updated attendance record
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not MAIN_TEACHER
     */
    AttendanceResponse updateAttendanceStatus(
        Long id,
        @Valid UpdateAttendanceStatusRequest request,
        Long teacherId
    );

    /**
     * Get attendance statistics for a student.
     *
     * @param studentId student ID
     * @return attendance statistics
     */
    AttendanceStatsResponse getStudentAttendanceStats(Long studentId);

    /**
     * Get attendance statistics for a class.
     *
     * @param classId class ID
     * @return attendance statistics
     */
    AttendanceStatsResponse getClassAttendanceStats(Long classId);

    /**
     * Soft delete an attendance record (admin only).
     *
     * @param id attendance ID
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if not found
     */
    void deleteAttendance(Long id);
}
