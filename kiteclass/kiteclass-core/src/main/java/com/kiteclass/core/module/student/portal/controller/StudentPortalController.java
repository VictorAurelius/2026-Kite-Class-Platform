package com.kiteclass.core.module.student.portal.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.student.portal.dto.StudentGradeDetailResponse;
import com.kiteclass.core.module.student.portal.dto.StudentGradeOverview;
import com.kiteclass.core.module.student.portal.dto.StudentNotificationFeedResponse;
import com.kiteclass.core.module.student.portal.dto.StudentPaymentSummary;
import com.kiteclass.core.module.student.portal.dto.StudentTodayResponse;
import com.kiteclass.core.module.student.portal.service.StudentPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only kc-student portal endpoints (GAP-269b, Wave 51 Bucket B).
 *
 * <p>Surface mirrors the production frontend screens shipped Wave 49
 * Bucket C ({@code (dashboard)/student/{today,grades,payments,notifications}}).
 * Identity is carried on {@code X-User-Reference-Id} populated by the
 * Gateway from {@code users.reference_id} when {@code userType = STUDENT}.
 *
 * <p>Phase 1 v1 returns shape-stable empty payloads so the FE can swap mock
 * fixtures for real fetch without changing types. Full join logic lands in
 * a follow-up alongside the FE consumer PR — the contract is published now
 * to keep the FE wiring effort decoupled.
 *
 * <p>Errors:
 * <ul>
 *   <li>401 {@code AUTH_REQUIRED} — Gateway didn't forward the student id.</li>
 *   <li>404 {@code STUDENT_PORTAL_SUBJECT_NOT_FOUND} — caller is not enrolled
 *       in the requested {@code subjectId} (raised by service when the join
 *       lands; placeholder today returns empty body).</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since GAP-269b (Wave 51 Bucket B)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/students/me")
@RequiredArgsConstructor
@Tag(name = "Student Portal (GAP-269b)",
        description = "Student-facing read APIs for kc-student frontend")
public class StudentPortalController {

    private final StudentPortalService service;

    @GetMapping("/today")
    @Operation(summary = "Today's class schedule + assignments due today",
            description = "Combines schedulePeriods + assignmentsDueToday for the calling student.")
    public ResponseEntity<ApiResponse<StudentTodayResponse>> getToday(
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long studentId) {
        Long id = requireStudentId(studentId);
        return ResponseEntity.ok(ApiResponse.success(service.getToday(id)));
    }

    @GetMapping("/grades")
    @Operation(summary = "Per-subject grades summary for the calling student")
    public ResponseEntity<ApiResponse<List<StudentGradeOverview>>> getGradesOverview(
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long studentId) {
        Long id = requireStudentId(studentId);
        return ResponseEntity.ok(ApiResponse.success(service.getGradesOverview(id)));
    }

    @GetMapping("/grades/{subjectId}")
    @Operation(summary = "Grade detail for one subject",
            description = "404 STUDENT_PORTAL_SUBJECT_NOT_FOUND if not enrolled in subjectId.")
    public ResponseEntity<ApiResponse<StudentGradeDetailResponse>> getSubjectDetail(
            @Parameter(description = "Subject ID") @PathVariable Long subjectId,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long studentId) {
        Long id = requireStudentId(studentId);
        return ResponseEntity.ok(ApiResponse.success(service.getSubjectDetail(id, subjectId)));
    }

    @GetMapping("/payments")
    @Operation(summary = "Invoice list for the calling student",
            description = "Read-only — settlement happens via owner-side payment flow.")
    public ResponseEntity<ApiResponse<List<StudentPaymentSummary>>> getPayments(
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long studentId) {
        Long id = requireStudentId(studentId);
        return ResponseEntity.ok(ApiResponse.success(service.getPayments(id)));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Cursor-paginated notification feed for the calling student",
            description = "limit clamped to [1..100]; nextCursor=null when no further page.")
    public ResponseEntity<ApiResponse<StudentNotificationFeedResponse>> getNotifications(
            @Parameter(description = "Opaque cursor; omit for first page")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "Page size 1..100; default 20")
            @RequestParam(required = false) Integer limit,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long studentId) {
        Long id = requireStudentId(studentId);
        return ResponseEntity.ok(ApiResponse.success(
                service.getNotifications(id, cursor, limit)));
    }

    private Long requireStudentId(Long studentId) {
        if (studentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        return studentId;
    }
}
