package com.kiteclass.core.module.attendance.controller;

import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.module.attendance.dto.AttendanceResponse;
import com.kiteclass.core.module.attendance.dto.AttendanceStatsResponse;
import com.kiteclass.core.module.attendance.dto.BulkAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.CreateAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.UpdateAttendanceStatusRequest;
import com.kiteclass.core.module.attendance.service.AttendanceService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for attendance management.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Marking student attendance</li>
 *   <li>Bulk attendance marking</li>
 *   <li>Attendance status updates</li>
 *   <li>Attendance queries and statistics</li>
 * </ul>
 *
 * <p><strong>GAP-1300 — recording-teacher authz hardening.</strong> The teacher-attributed
 * write endpoints ({@code markBulkAttendance}, {@code updateAttendanceStatus}) derive the
 * acting teacher from the authenticated principal (gateway-injected
 * {@code X-User-Reference-Id} → {@link UserContext#getCurrentReferenceId()}), NOT the former
 * client-supplied {@code X-Teacher-Id} header which the gateway does NOT control (per GAP-814)
 * and was therefore spoofable. ADMIN/OWNER (no numeric reference id) bypass the per-class
 * MAIN_TEACHER check at the service layer ({@code AuthorizationBean.isAdmin()}).
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Attendance management APIs")
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * Resolve the acting teacher's numeric id from the authenticated principal
     * (gateway-injected {@code X-User-Reference-Id} → {@link UserContext}), NOT from any
     * client-supplied header (GAP-1300). Returns {@code null} for ADMIN/OWNER, who carry no
     * numeric reference id; the service layer bypasses the MAIN_TEACHER check for them.
     *
     * @return the authenticated teacher's reference id, or {@code null} for admin/owner
     */
    private Long actingTeacherId() {
        return UserContext.getCurrentReferenceId();
    }

    /**
     * Mark attendance for a single student.
     *
     * @param request attendance marking request
     * @return created attendance record
     */
    @PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToEnrollment(#request.enrollmentId)")
    @PostMapping
    @Operation(summary = "Mark attendance for a student",
               description = "Creates a new attendance record. Validates enrollment and prevents duplicates. "
                       + "Per-resource authz via @authz.hasAccessToEnrollment (OWASP A01) — GAP-991 cross-flow sweep of GAP-729.")
    public ResponseEntity<AttendanceResponse> markAttendance(
            @Valid @RequestBody CreateAttendanceRequest request) {
        log.info("POST /api/v1/attendance - Marking attendance for enrollment {} in session {}",
                request.getEnrollmentId(), request.getSessionId());

        AttendanceResponse response = attendanceService.markAttendance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Mark attendance for multiple students in a session (bulk operation).
     * Only MAIN_TEACHER can mark attendance.
     *
     * @param classId class ID
     * @param sessionId session ID
     * @param request bulk attendance request
     * @param teacherId teacher ID from header (must be MAIN_TEACHER)
     * @return list of created attendance records
     */
    @PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToClass(#classId)")
    @PostMapping("/classes/{classId}/sessions/{sessionId}/attendance")
    @Operation(summary = "Bulk mark attendance for a session",
               description = "Creates multiple attendance records for a session. Only MAIN_TEACHER can mark attendance. "
                       + "Per-resource authz via @authz.hasAccessToClass (OWASP A01) — Wave 105 Bucket E0.")
    public ResponseEntity<List<AttendanceResponse>> markBulkAttendance(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @Parameter(description = "Session ID") @PathVariable Long sessionId,
            @Valid @RequestBody BulkAttendanceRequest request) {
        Long teacherId = actingTeacherId();
        log.info("POST /api/v1/classes/{}/sessions/{}/attendance - Bulk marking attendance for {} students by teacher {}",
                classId, sessionId, request.getRecords().size(), teacherId);

        List<AttendanceResponse> responses = attendanceService.markBulkAttendance(
                classId, sessionId, request, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Get attendance record by ID.
     *
     * @param id attendance ID
     * @return attendance record
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get attendance record by ID")
    public ResponseEntity<AttendanceResponse> getAttendance(
            @Parameter(description = "Attendance ID") @PathVariable Long id) {
        log.debug("GET /api/v1/attendance/{}", id);

        AttendanceResponse response = attendanceService.getAttendanceById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all attendance records for an enrollment (student's attendance history).
     *
     * @param enrollmentId enrollment ID
     * @param pageable pagination parameters
     * @return page of attendance records
     */
    @GetMapping("/enrollment/{enrollmentId}")
    @Operation(summary = "Get attendance history for an enrollment",
               description = "Returns all attendance records for a specific enrollment.")
    public ResponseEntity<Page<AttendanceResponse>> getAttendanceByEnrollment(
            @Parameter(description = "Enrollment ID") @PathVariable Long enrollmentId,
            @PageableDefault(sort = "markedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.debug("GET /api/v1/attendance/enrollment/{}", enrollmentId);

        Page<AttendanceResponse> response = attendanceService.getAttendanceByEnrollment(
                enrollmentId, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get all attendance records for a session (session roster).
     *
     * @param classId class ID
     * @param sessionId session ID
     * @param pageable pagination parameters
     * @return page of attendance records
     */
    @PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToClass(#classId)")
    @GetMapping("/classes/{classId}/sessions/{sessionId}/attendance")
    @Operation(summary = "Get attendance roster for a session",
               description = "Returns all attendance records for a specific session. "
                       + "Per-resource authz via @authz.hasAccessToClass (OWASP A01) — Wave 105 Bucket E0.")
    public ResponseEntity<Page<AttendanceResponse>> getAttendanceBySession(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @Parameter(description = "Session ID") @PathVariable Long sessionId,
            @PageableDefault(sort = "enrollmentId", direction = Sort.Direction.ASC)
            Pageable pageable) {
        log.debug("GET /api/v1/classes/{}/sessions/{}/attendance", classId, sessionId);

        Page<AttendanceResponse> response = attendanceService.getAttendanceBySession(
                sessionId, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get all attendance records for a session by session id alone (session roster).
     *
     * <p>FE↔BE contract fix (GAP-1165): the frontend
     * {@code attendanceApi.getAttendanceBySession(sessionId)} calls
     * {@code GET /api/v1/attendance/session/{sessionId}} (no classId), which had no
     * backend mapping → 404 on the attendance reports page. This sibling of the
     * class-scoped roster endpoint resolves ownership from the session itself.
     *
     * <p>Per-resource authz via {@code @authz.hasAccessToSession} (OWASP A01) —
     * resolves session → class → {@code classes.teacher_id} == actor, OR admin.
     *
     * @param sessionId session ID
     * @param pageable pagination parameters
     * @return page of attendance records
     */
    @PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToSession(#sessionId)")
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get attendance roster for a session (by session id)",
               description = "Returns all attendance records for a specific session, resolved by session id alone. "
                       + "Per-resource authz via @authz.hasAccessToSession (OWASP A01) — GAP-1165 FE↔BE contract fix.")
    public ResponseEntity<Page<AttendanceResponse>> getAttendanceBySessionId(
            @Parameter(description = "Session ID") @PathVariable Long sessionId,
            @PageableDefault(sort = "enrollmentId", direction = Sort.Direction.ASC)
            Pageable pageable) {
        log.debug("GET /api/v1/attendance/session/{}", sessionId);

        Page<AttendanceResponse> response = attendanceService.getAttendanceBySession(
                sessionId, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get attendance statistics for a student.
     *
     * <p><strong>OWASP A01 per-resource guard (GAP-1428):</strong> student attendance
     * stats are an OWNED resource — previously this endpoint had NO {@code @PreAuthorize},
     * so an unauthenticated GET returned 200 with real data (no-auth leak, KC-5).
     * Now guarded by {@code @authz.hasAccessToStudent(#studentId)} for consistency with
     * the class-scoped sibling {@link #getClassStats(Long)} (which uses
     * {@code @authz.hasAccessToClass}). STAFF bypass mirrors the class endpoint.
     *
     * @param studentId student ID
     * @return attendance statistics
     */
    @GetMapping("/stats/student/{studentId}")
    @PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToStudent(#studentId)")
    @Operation(summary = "Get attendance statistics for a student",
               description = "Returns aggregated attendance statistics including rates and counts. "
                       + "Per-resource authz via @authz.hasAccessToStudent (OWASP A01) — GAP-1428 KC-5 no-auth leak fix.")
    public ResponseEntity<AttendanceStatsResponse> getStudentStats(
            @Parameter(description = "Student ID") @PathVariable Long studentId) {
        log.debug("GET /api/v1/attendance/stats/student/{}", studentId);

        AttendanceStatsResponse response = attendanceService.getStudentAttendanceStats(studentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get attendance statistics for a class.
     *
     * <p><strong>OWASP A01 per-resource guard (GAP-729):</strong> class attendance
     * stats are an OWNED resource — guarded by {@code @authz.hasAccessToClass(#classId)}
     * for consistency with the two session-level attendance endpoints above (cross-flow
     * sweep: this was the lone unguarded class-scoped sibling).
     *
     * @param classId class ID
     * @return attendance statistics
     */
    @GetMapping("/stats/class/{classId}")
    @PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToClass(#classId)")
    @Operation(summary = "Get attendance statistics for a class",
               description = "Returns aggregated attendance statistics for all students in a class.")
    public ResponseEntity<AttendanceStatsResponse> getClassStats(
            @Parameter(description = "Class ID") @PathVariable Long classId) {
        log.debug("GET /api/v1/attendance/stats/class/{}", classId);

        AttendanceStatsResponse response = attendanceService.getClassAttendanceStats(classId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update attendance status.
     * Only MAIN_TEACHER can update attendance.
     *
     * @param id attendance ID
     * @param request status update request
     * @param teacherId teacher ID from header (must be MAIN_TEACHER)
     * @return updated attendance record
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')")
    @Operation(summary = "Update attendance status",
               description = "Updates attendance status and recalculates points. Only MAIN_TEACHER "
                       + "(or ADMIN/OWNER) can update. Acting teacher derived from the authenticated "
                       + "principal (X-User-Reference-Id), NOT a client header (GAP-1300).")
    public ResponseEntity<AttendanceResponse> updateAttendanceStatus(
            @Parameter(description = "Attendance ID") @PathVariable Long id,
            @Valid @RequestBody UpdateAttendanceStatusRequest request) {
        Long teacherId = actingTeacherId();
        log.info("PATCH /api/v1/attendance/{} - Updating to status {} by teacher {}",
                id, request.getStatus(), teacherId);

        AttendanceResponse response = attendanceService.updateAttendanceStatus(id, request, teacherId);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft delete an attendance record (admin only).
     *
     * @param id attendance ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete attendance record",
               description = "Soft deletes an attendance record (admin only).")
    public ResponseEntity<Void> deleteAttendance(
            @Parameter(description = "Attendance ID") @PathVariable Long id) {
        log.info("DELETE /api/v1/attendance/{}", id);

        attendanceService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }
}
