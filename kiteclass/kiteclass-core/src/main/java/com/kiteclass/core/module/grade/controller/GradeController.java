package com.kiteclass.core.module.grade.controller;

import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.grade.dto.request.CreateGradeComponentRequest;
import com.kiteclass.core.module.grade.dto.request.FinalizeGradeRequest;
import com.kiteclass.core.module.grade.dto.request.UpdateGradeComponentRequest;
import com.kiteclass.core.module.grade.dto.response.GradeComponentResponse;
import com.kiteclass.core.module.grade.dto.response.GradeResponse;
import com.kiteclass.core.module.grade.dto.response.GradingSummaryResponse;
import com.kiteclass.core.module.grade.dto.response.TranscriptResponse;
import com.kiteclass.core.module.grade.service.GradeService;
import io.micrometer.core.annotation.Timed;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Grade operations.
 *
 * <p>Per-resource authorization via {@code @authz} ({@code AuthorizationBean})
 * closes the OWASP A01 (Broken Access Control) gap on grade write/calculate/read
 * endpoints — GAP-996c Wave flow-kc6 (cross-flow sweep of GAP-729/991). Each
 * id-scoped endpoint resolves the target grade/component → its class → verifies
 * {@code classes.teacher_id} == actor UUID (or admin bypass), mirroring the
 * attendance (KC-5) precedent so any authenticated tenant user can no longer
 * edit/calculate/finalize/unlock grades they do not own.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@RestController
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-b", "controller", "grade"})
public class GradeController {

    private final GradeService gradeService;

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
     * Initialize grade for a student in a class.
     *
     * <p>Per-resource authz via {@code @authz.hasAccessToClass} (OWASP A01) —
     * Wave 105 Bucket E0.
     */
    @PostMapping("/initialize")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<GradeResponse>> initializeGrade(
            @RequestParam Long studentId,
            @RequestParam Long classId) {

        GradeResponse response = gradeService.initializeGrade(studentId, classId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * Get grade by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authz.hasAccessToGrade(#id)")
    public ResponseEntity<ApiResponse<GradeResponse>> getGradeById(@PathVariable Long id) {
        GradeResponse response = gradeService.getGradeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get grade by student ID and class ID.
     */
    @GetMapping("/student/{studentId}/class/{classId}")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<GradeResponse>> getStudentGrade(
            @PathVariable Long studentId,
            @PathVariable Long classId) {

        GradeResponse response = gradeService.getStudentGrade(studentId, classId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all grades by student ID.
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("@authz.hasAccessToStudent(#studentId)")
    public ResponseEntity<ApiResponse<List<GradeResponse>>> getGradesByStudent(
            @PathVariable Long studentId) {

        List<GradeResponse> response = gradeService.getGradesByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all grades by class ID (summary view).
     *
     * <p>Per-resource authz via {@code @authz.hasAccessToClass} (OWASP A01) —
     * Wave 105 Bucket E0.
     */
    @GetMapping("/class/{classId}")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<List<GradingSummaryResponse>>> getGradesByClass(
            @PathVariable Long classId) {

        List<GradingSummaryResponse> response = gradeService.getGradesByClass(classId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Add or update a grade component.
     */
    @PostMapping("/components")
    @PreAuthorize("@authz.hasAccessToGrade(#request.gradeId)")
    public ResponseEntity<ApiResponse<GradeComponentResponse>> addOrUpdateComponent(
            @Valid @RequestBody CreateGradeComponentRequest request) {

        GradeComponentResponse response = gradeService.addOrUpdateComponent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * Update existing grade component.
     */
    @PutMapping("/components/{id}")
    @PreAuthorize("@authz.hasAccessToGradeComponent(#id)")
    public ResponseEntity<ApiResponse<GradeComponentResponse>> updateComponent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGradeComponentRequest request) {

        GradeComponentResponse response = gradeService.updateComponent(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete a grade component.
     */
    @DeleteMapping("/components/{id}")
    @PreAuthorize("@authz.hasAccessToGradeComponent(#id)")
    public ResponseEntity<ApiResponse<Void>> deleteComponent(
            @PathVariable Long id) {

        // GAP-1301: acting teacher from the authenticated principal (X-User-Reference-Id),
        // NOT the spoofable client X-Teacher-Id header. @authz.hasAccessToGradeComponent
        // already gates ownership (incl. ADMIN/OWNER bypass); the service-layer check below
        // is defense-in-depth against the same token-derived identity.
        gradeService.deleteComponent(id, actingTeacherId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null));
    }

    /**
     * Calculate final score from all components.
     */
    @PostMapping("/{id}/calculate")
    @PreAuthorize("@authz.hasAccessToGrade(#id)")
    public ResponseEntity<ApiResponse<GradeResponse>> calculateFinalScore(@PathVariable Long id) {
        GradeResponse response = gradeService.calculateFinalScore(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Finalize grade (lock for editing).
     */
    @PostMapping("/{id}/finalize")
    @PreAuthorize("@authz.hasAccessToGrade(#id)")
    public ResponseEntity<ApiResponse<GradeResponse>> finalizeGrade(
            @PathVariable Long id,
            @Valid @RequestBody FinalizeGradeRequest request) {

        GradeResponse response = gradeService.finalizeGrade(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Unfinalize grade (unlock for editing).
     */
    @PostMapping("/{id}/unfinalize")
    @PreAuthorize("@authz.hasAccessToGrade(#id)")
    public ResponseEntity<ApiResponse<GradeResponse>> unfinalizeGrade(@PathVariable Long id) {
        GradeResponse response = gradeService.unfinalizeGrade(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Generate transcript for student in a semester.
     */
    @PostMapping("/transcripts/generate")
    @PreAuthorize("@authz.hasAccessToStudent(#studentId)")
    public ResponseEntity<ApiResponse<TranscriptResponse>> generateTranscript(
            @RequestParam Long studentId,
            @RequestParam String semester) {

        TranscriptResponse response = gradeService.generateTranscript(studentId, semester);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * Get transcript by student ID and semester.
     */
    @GetMapping("/transcripts/student/{studentId}/semester/{semester}")
    @PreAuthorize("@authz.hasAccessToStudent(#studentId)")
    public ResponseEntity<ApiResponse<TranscriptResponse>> getTranscript(
            @PathVariable Long studentId,
            @PathVariable String semester) {

        TranscriptResponse response = gradeService.getTranscript(studentId, semester);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all transcripts by student ID.
     */
    @GetMapping("/transcripts/student/{studentId}")
    @PreAuthorize("@authz.hasAccessToStudent(#studentId)")
    public ResponseEntity<ApiResponse<List<TranscriptResponse>>> getTranscriptsByStudent(
            @PathVariable Long studentId) {

        List<TranscriptResponse> response = gradeService.getTranscriptsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Calculate class statistics.
     */
    @GetMapping("/class/{classId}/statistics")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateClassStatistics(
            @PathVariable Long classId) {

        Map<String, Object> response = gradeService.calculateClassStatistics(classId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
