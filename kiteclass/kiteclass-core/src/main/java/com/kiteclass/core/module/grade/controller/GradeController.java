package com.kiteclass.core.module.grade.controller;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Grade operations.
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
     * Initialize grade for a student in a class.
     *
     * <p>Per-resource authz via {@code @authz.hasAccessToClass} (OWASP A01) —
     * Wave 105 Bucket E0.
     */
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
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
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
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
    public ResponseEntity<ApiResponse<Void>> deleteComponent(
            @PathVariable Long id,
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        gradeService.deleteComponent(id, teacherId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null));
    }

    /**
     * Calculate final score from all components.
     */
    @PostMapping("/{id}/calculate")
    public ResponseEntity<ApiResponse<GradeResponse>> calculateFinalScore(@PathVariable Long id) {
        GradeResponse response = gradeService.calculateFinalScore(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Finalize grade (lock for editing).
     */
    @PostMapping("/{id}/finalize")
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
    public ResponseEntity<ApiResponse<GradeResponse>> unfinalizeGrade(@PathVariable Long id) {
        GradeResponse response = gradeService.unfinalizeGrade(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Generate transcript for student in a semester.
     */
    @PostMapping("/transcripts/generate")
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
