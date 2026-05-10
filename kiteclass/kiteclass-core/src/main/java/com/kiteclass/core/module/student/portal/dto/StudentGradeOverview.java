package com.kiteclass.core.module.student.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Per-subject grade summary row for the kc-student grades index screen
 * (GAP-269b).
 *
 * <p>Surfaces the running average + most-recent published mark across one
 * student's enrolled subjects. The detail screen
 * ({@code GET /api/v1/students/me/grades/{subjectId}}) returns a separate
 * {@link StudentGradeDetailResponse} payload with per-entry breakdown.
 *
 * @since GAP-269b (Wave 51 Bucket B)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeOverview {

    private Long subjectId;
    private String subjectName;

    /** Running average across published grades; null when no grades yet. */
    private BigDecimal average;

    /** Highest grade published this term (TX/GK/CK aggregated). */
    private BigDecimal highest;

    /** Lowest grade published this term. */
    private BigDecimal lowest;

    /** Total count of published grade entries for this subject. */
    private Integer entryCount;
}
