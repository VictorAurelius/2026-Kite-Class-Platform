package com.kiteclass.core.module.grade.service;

import com.kiteclass.core.module.grade.dto.request.CreateGradeComponentRequest;
import com.kiteclass.core.module.grade.dto.request.FinalizeGradeRequest;
import com.kiteclass.core.module.grade.dto.request.UpdateGradeComponentRequest;
import com.kiteclass.core.module.grade.dto.response.GradeComponentResponse;
import com.kiteclass.core.module.grade.dto.response.GradeResponse;
import com.kiteclass.core.module.grade.dto.response.GradingSummaryResponse;
import com.kiteclass.core.module.grade.dto.response.TranscriptResponse;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Service interface for Grade operations.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
public interface GradeService {

    /**
     * Initialize grade for a student in a class.
     * Called automatically when student enrolls in class.
     *
     * @param studentId student ID
     * @param classId class ID
     * @return created grade
     */
    GradeResponse initializeGrade(Long studentId, Long classId);

    /**
     * Get grade by ID.
     *
     * @param id grade ID
     * @return grade with components
     */
    GradeResponse getGradeById(Long id);

    /**
     * Get grade by student ID and class ID.
     *
     * @param studentId student ID
     * @param classId class ID
     * @return grade with components
     */
    GradeResponse getStudentGrade(Long studentId, Long classId);

    /**
     * Get all grades by student ID.
     * Used for student's academic record.
     *
     * @param studentId student ID
     * @return list of grades
     */
    List<GradeResponse> getGradesByStudent(Long studentId);

    /**
     * Get all grades by class ID.
     * Used for class grade report.
     *
     * @param classId class ID
     * @return list of grades
     */
    List<GradingSummaryResponse> getGradesByClass(Long classId);

    /**
     * Add or update a grade component.
     * Auto-calculates weighted score.
     *
     * @param request the component request
     * @return created/updated component
     */
    GradeComponentResponse addOrUpdateComponent(@Valid CreateGradeComponentRequest request);

    /**
     * Update existing grade component.
     *
     * @param componentId component ID
     * @param request the update request
     * @return updated component
     */
    GradeComponentResponse updateComponent(Long componentId, @Valid UpdateGradeComponentRequest request);

    /**
     * Delete a grade component (soft delete).
     *
     * @param componentId component ID
     * @param teacherId the teacher ID (permission check)
     */
    void deleteComponent(Long componentId, Long teacherId);

    /**
     * Calculate final score from all components.
     * Updates final_score, letter_grade, and gpa.
     *
     * @param gradeId grade ID
     * @return updated grade
     */
    GradeResponse calculateFinalScore(Long gradeId);

    /**
     * Finalize grade (lock for editing).
     * Only MAIN_TEACHER can finalize.
     *
     * @param gradeId grade ID
     * @param request the finalize request (teacher ID, comments)
     * @return finalized grade
     */
    GradeResponse finalizeGrade(Long gradeId, @Valid FinalizeGradeRequest request);

    /**
     * Unfinalize grade (unlock for editing).
     * Only ADMIN can unfinalize.
     *
     * @param gradeId grade ID
     * @return unfinaliz grade
     */
    GradeResponse unfinalizeGrade(Long gradeId);

    /**
     * Generate transcript for student in a semester.
     * Includes all finalized grades and GPA calculation.
     *
     * @param studentId student ID
     * @param semester semester (e.g., "Spring 2026")
     * @return generated transcript
     */
    TranscriptResponse generateTranscript(Long studentId, String semester);

    /**
     * Get transcript by student ID and semester.
     *
     * @param studentId student ID
     * @param semester semester
     * @return transcript if found
     */
    TranscriptResponse getTranscript(Long studentId, String semester);

    /**
     * Get all transcripts by student ID.
     *
     * @param studentId student ID
     * @return list of transcripts
     */
    List<TranscriptResponse> getTranscriptsByStudent(Long studentId);

    /**
     * Calculate class statistics (average, pass rate, etc.).
     *
     * @param classId class ID
     * @return statistics map
     */
    java.util.Map<String, Object> calculateClassStatistics(Long classId);
}
