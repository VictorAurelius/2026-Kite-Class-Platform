package com.kiteclass.core.module.assignment.controller;

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
 * @author KiteClass Team
 * @since 2.7.1
 */
@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    /**
     * Create a new assignment (MAIN_TEACHER only).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request,
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        AssignmentResponse response = assignmentService.createAssignment(request, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }

    /**
     * Update an assignment (MAIN_TEACHER only).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssignmentRequest request,
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        AssignmentResponse response = assignmentService.updateAssignment(id, request, teacherId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Publish an assignment (make visible to students).
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<AssignmentResponse>> publishAssignment(
            @PathVariable Long id,
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        AssignmentResponse response = assignmentService.publishAssignment(id, teacherId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Close an assignment (no more submissions).
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<AssignmentResponse>> closeAssignment(
            @PathVariable Long id,
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        AssignmentResponse response = assignmentService.closeAssignment(id, teacherId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete an assignment (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(
            @PathVariable Long id,
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        assignmentService.deleteAssignment(id, teacherId);
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
     * Get all assignments for a class.
     */
    @GetMapping("/class/{classId}")
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
     */
    @PostMapping("/submit")
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
    public ResponseEntity<ApiResponse<SubmissionResponse>> gradeSubmission(
            @PathVariable Long id,
            @Valid @RequestBody GradeSubmissionRequest request,
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        SubmissionResponse response = assignmentService.gradeSubmission(id, request, teacherId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Return graded submission to student.
     */
    @PostMapping("/submissions/{id}/return")
    public ResponseEntity<ApiResponse<SubmissionResponse>> returnSubmission(
            @PathVariable Long id,
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        SubmissionResponse response = assignmentService.returnSubmission(id, teacherId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get submission by ID.
     */
    @GetMapping("/submissions/{id}")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getSubmissionById(
            @PathVariable Long id) {

        SubmissionResponse response = assignmentService.getSubmissionById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all submissions for an assignment.
     */
    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissionsByAssignment(
            @PathVariable Long assignmentId) {

        List<SubmissionResponse> responses = assignmentService.getSubmissionsByAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get student's submission for an assignment.
     */
    @GetMapping("/{assignmentId}/submissions/student/{studentId}")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getStudentSubmission(
            @PathVariable Long assignmentId,
            @PathVariable Long studentId) {

        SubmissionResponse response = assignmentService.getStudentSubmission(assignmentId, studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all submissions by student.
     */
    @GetMapping("/submissions/student/{studentId}")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissionsByStudent(
            @PathVariable Long studentId) {

        List<SubmissionResponse> responses = assignmentService.getSubmissionsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get pending grading submissions for a class.
     */
    @GetMapping("/class/{classId}/pending-grading")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getPendingGradingByClass(
            @PathVariable Long classId) {

        List<SubmissionResponse> responses = assignmentService.getPendingGradingByClass(classId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
