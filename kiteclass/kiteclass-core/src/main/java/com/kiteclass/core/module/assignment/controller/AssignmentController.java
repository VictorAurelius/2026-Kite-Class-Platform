package com.kiteclass.core.module.assignment.controller;

import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.assignment.dto.request.CreateAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.request.GradeSubmissionRequest;
import com.kiteclass.core.module.assignment.dto.request.SubmitAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.request.UpdateAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.response.AssignmentResponse;
import com.kiteclass.core.module.assignment.dto.response.SubmissionResponse;
import com.kiteclass.core.module.assignment.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Assignment operations.
 *
 * <p><strong>OWASP A01 authz classification (GAP-837):</strong>
 * <ul>
 *   <li><strong>OWNED → {@code @authz.hasAccessToClass(#classId)}:</strong>
 *       {@code getAssignmentsByClass} (teacher view of full assignment list incl.
 *       drafts), {@code getPendingGradingByClass} (teacher grading queue)</li>
 *   <li><strong>SHARED (student-visible) → tenant-filter only:</strong>
 *       {@code getPublishedAssignmentsByClass} — by design exposed to enrolled
 *       students (only published items, not drafts). Tightening to teacher-only
 *       would block intended student access pattern.</li>
 *   <li><strong>id-scoped (GAP-1301 hardened):</strong> create / update / publish / close /
 *       delete / grade / submission ops are (1) role-gated
 *       {@code @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")} so STUDENT/PARENT are
 *       blocked, and (2) derive the acting teacher from the authenticated principal
 *       (gateway-injected {@code X-User-Reference-Id} → {@link UserContext}). The former
 *       client-supplied {@code X-Teacher-Id} header — which the gateway does NOT control
 *       (per GAP-814) and was therefore spoofable — is no longer read as an identity source.
 *       ADMIN/OWNER (no numeric reference id) bypass the per-class MAIN_TEACHER check at the
 *       service layer ({@code AuthorizationBean.isAdmin()}). A finer per-resource
 *       {@code hasAccessToAssignment(#id)} helper remains a GAP-837 follow-up.</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    /**
     * Resolve the acting teacher's numeric id from the authenticated principal
     * (gateway-injected {@code X-User-Reference-Id} → {@link UserContext}), NOT from any
     * client-supplied header (GAP-1301). Returns {@code null} for ADMIN/OWNER, who carry no
     * numeric reference id; the service layer bypasses the MAIN_TEACHER check for them.
     *
     * @return the authenticated teacher's reference id, or {@code null} for admin/owner
     */
    private Long actingTeacherId() {
        return UserContext.getCurrentReferenceId();
    }

    /**
     * Create a new assignment (MAIN_TEACHER only).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request) {

        AssignmentResponse response = assignmentService.createAssignment(request, actingTeacherId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }

    /**
     * Update an assignment (MAIN_TEACHER only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssignmentRequest request) {

        AssignmentResponse response = assignmentService.updateAssignment(id, request, actingTeacherId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Publish an assignment (make visible to students).
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> publishAssignment(
            @PathVariable Long id) {

        AssignmentResponse response = assignmentService.publishAssignment(id, actingTeacherId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Close an assignment (no more submissions).
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> closeAssignment(
            @PathVariable Long id) {

        AssignmentResponse response = assignmentService.closeAssignment(id, actingTeacherId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete an assignment (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(
            @PathVariable Long id) {

        assignmentService.deleteAssignment(id, actingTeacherId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(ApiResponse.success(null));
    }

    /**
     * Get assignment by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignmentById(
            @PathVariable Long id) {

        AssignmentResponse response = assignmentService.getAssignmentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all assignments for a class (teacher view — includes drafts).
     *
     * <p>OWASP A01 per-resource guard (GAP-837): OWNED, teacher-only. The
     * published variant {@link #getPublishedAssignmentsByClass(Long)} is the
     * student-facing alternative (SHARED, tenant-filter only).
     */
    @GetMapping("/class/{classId}")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getAssignmentsByClass(
            @PathVariable Long classId) {

        List<AssignmentResponse> responses = assignmentService.getAssignmentsByClass(classId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get published assignments for a class (student view).
     */
    @GetMapping("/class/{classId}/published")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getPublishedAssignmentsByClass(
            @PathVariable Long classId) {

        List<AssignmentResponse> responses = assignmentService.getPublishedAssignmentsByClass(classId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Submit an assignment (student).
     *
     * <p>OWASP A01 (GAP-1527): role-gated to STUDENT so a TEACHER/ADMIN cannot
     * spoof a student submission. The acting student id is still derived from the
     * gateway-forwarded {@code X-User-Id} header (gateway-controlled identity).
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitAssignment(
            @Valid @RequestBody SubmitAssignmentRequest request,
            @RequestHeader("X-User-Id") Long studentId) {

        SubmissionResponse response = assignmentService.submitAssignment(request, studentId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }

    /**
     * Grade a submission (MAIN_TEACHER or assigned grader).
     */
    @PostMapping("/submissions/{id}/grade")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> gradeSubmission(
            @PathVariable Long id,
            @Valid @RequestBody GradeSubmissionRequest request) {

        SubmissionResponse response = assignmentService.gradeSubmission(id, request, actingTeacherId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Return graded submission to student.
     */
    @PostMapping("/submissions/{id}/return")
    @PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> returnSubmission(
            @PathVariable Long id) {

        SubmissionResponse response = assignmentService.returnSubmission(id, actingTeacherId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get submission by ID.
     *
     * <p>OWASP A01 (GAP-1527): role-gated read — submissions + grades are
     * teacher/staff-facing; STUDENT/PARENT are blocked from arbitrary id lookup.
     */
    @GetMapping("/submissions/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getSubmissionById(
            @PathVariable Long id) {

        SubmissionResponse response = assignmentService.getSubmissionById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all submissions for an assignment.
     *
     * <p>OWASP A01 (GAP-1527): teacher/staff-facing grading surface — role-gated.
     */
    @GetMapping("/{assignmentId}/submissions")
    @PreAuthorize("hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissionsByAssignment(
            @PathVariable Long assignmentId) {

        List<SubmissionResponse> responses = assignmentService.getSubmissionsByAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get student's submission for an assignment.
     *
     * <p>OWASP A01 (GAP-1527): teacher/staff-facing — role-gated. A finer
     * per-student ownership helper (so a student reads only their own) is a
     * follow-up (mirrors GAP-837 hasAccessToAssignment deferral).
     */
    @GetMapping("/{assignmentId}/submissions/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getStudentSubmission(
            @PathVariable Long assignmentId,
            @PathVariable Long studentId) {

        SubmissionResponse response = assignmentService.getStudentSubmission(assignmentId, studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all submissions by student.
     *
     * <p>OWASP A01 (GAP-1527): teacher/staff-facing — role-gated.
     */
    @GetMapping("/submissions/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissionsByStudent(
            @PathVariable Long studentId) {

        List<SubmissionResponse> responses = assignmentService.getSubmissionsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get pending grading submissions for a class (teacher view).
     *
     * <p>OWASP A01 per-resource guard (GAP-837): OWNED, teacher-only.
     */
    @GetMapping("/class/{classId}/pending-grading")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getPendingGradingByClass(
            @PathVariable Long classId) {

        List<SubmissionResponse> responses = assignmentService.getPendingGradingByClass(classId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
