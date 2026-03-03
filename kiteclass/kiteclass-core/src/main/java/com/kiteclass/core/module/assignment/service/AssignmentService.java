package com.kiteclass.core.module.assignment.service;

import com.kiteclass.core.module.assignment.dto.request.*;
import com.kiteclass.core.module.assignment.dto.response.AssignmentResponse;
import com.kiteclass.core.module.assignment.dto.response.SubmissionResponse;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Service interface for Assignment operations.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
public interface AssignmentService {

    /**
     * Create a new assignment (MAIN_TEACHER only).
     *
     * @param request the create request
     * @param teacherId the teacher ID
     * @return created assignment
     */
    AssignmentResponse createAssignment(@Valid CreateAssignmentRequest request, Long teacherId);

    /**
     * Update an assignment (MAIN_TEACHER only).
     *
     * @param id assignment ID
     * @param request the update request
     * @param teacherId the teacher ID
     * @return updated assignment
     */
    AssignmentResponse updateAssignment(Long id, @Valid UpdateAssignmentRequest request, Long teacherId);

    /**
     * Publish an assignment (make visible to students).
     *
     * @param id assignment ID
     * @param teacherId the teacher ID
     * @return published assignment
     */
    AssignmentResponse publishAssignment(Long id, Long teacherId);

    /**
     * Close an assignment (no more submissions).
     *
     * @param id assignment ID
     * @param teacherId the teacher ID
     * @return closed assignment
     */
    AssignmentResponse closeAssignment(Long id, Long teacherId);

    /**
     * Delete an assignment (soft delete).
     *
     * @param id assignment ID
     * @param teacherId the teacher ID
     */
    void deleteAssignment(Long id, Long teacherId);

    /**
     * Get assignment by ID.
     *
     * @param id assignment ID
     * @return assignment
     */
    AssignmentResponse getAssignmentById(Long id);

    /**
     * Get all assignments for a class.
     *
     * @param classId class ID
     * @return list of assignments
     */
    List<AssignmentResponse> getAssignmentsByClass(Long classId);

    /**
     * Get published assignments for a class (student view).
     *
     * @param classId class ID
     * @return list of published assignments
     */
    List<AssignmentResponse> getPublishedAssignmentsByClass(Long classId);

    /**
     * Submit an assignment (student).
     *
     * @param request the submit request
     * @param studentId the student ID
     * @return created submission
     */
    SubmissionResponse submitAssignment(@Valid SubmitAssignmentRequest request, Long studentId);

    /**
     * Grade a submission (MAIN_TEACHER or assigned grader).
     *
     * @param submissionId submission ID
     * @param request the grade request
     * @param teacherId the teacher ID
     * @return graded submission
     */
    SubmissionResponse gradeSubmission(Long submissionId, @Valid GradeSubmissionRequest request, Long teacherId);

    /**
     * Return graded submission to student (send feedback notification).
     *
     * @param submissionId submission ID
     * @param teacherId the teacher ID
     * @return returned submission
     */
    SubmissionResponse returnSubmission(Long submissionId, Long teacherId);

    /**
     * Get submission by ID.
     *
     * @param id submission ID
     * @return submission
     */
    SubmissionResponse getSubmissionById(Long id);

    /**
     * Get all submissions for an assignment.
     *
     * @param assignmentId assignment ID
     * @return list of submissions
     */
    List<SubmissionResponse> getSubmissionsByAssignment(Long assignmentId);

    /**
     * Get student's submission for an assignment.
     *
     * @param assignmentId assignment ID
     * @param studentId student ID
     * @return submission if found
     */
    SubmissionResponse getStudentSubmission(Long assignmentId, Long studentId);

    /**
     * Get all submissions by student.
     *
     * @param studentId student ID
     * @return list of submissions
     */
    List<SubmissionResponse> getSubmissionsByStudent(Long studentId);

    /**
     * Get pending grading submissions for a class.
     *
     * @param classId class ID
     * @return list of pending submissions
     */
    List<SubmissionResponse> getPendingGradingByClass(Long classId);
}
