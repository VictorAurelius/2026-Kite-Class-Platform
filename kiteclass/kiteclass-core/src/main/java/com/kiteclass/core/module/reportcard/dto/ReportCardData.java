package com.kiteclass.core.module.reportcard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated data for a single-student per-semester report card (GAP-055 Phase 1).
 *
 * <p>Source: {@code documents/01-business/kiteclass/report-card/rules.md}
 * Layer 1 BR-RC-AGG-001..008 + BR-RC-LAY-001..011.
 *
 * <p>{@code conduct} is intentionally nullable in Phase 1 — GAP-059 (hạnh kiểm) deferred.
 * Templates render "Chưa cập nhật" when null per UC-RC-03.
 *
 * <p>{@code overallAverage} is the simple arithmetic mean of subject averages whose
 * {@code average != null}. Curriculum-weighted overall is Phase 2 follow-up.
 *
 * @since 3.18.0 (GAP-055 Phase 1)
 */
public record ReportCardData(
        Long studentId,
        String studentName,
        LocalDate studentDateOfBirth,
        String homeroomClassLabel,
        String semesterLabel,
        List<SubjectRow> subjects,
        BigDecimal overallAverage,
        String overallLetterGrade,
        String conduct
) {

    /**
     * One row of the report card grade table.
     *
     * <p>Any of {@code regularScore} / {@code midtermScore} / {@code finalScore}
     * may be null — template renders em-dash ("—"), not zero (BR-RC-AGG-007).
     * If at least one input score is null then {@code average} and
     * {@code letterGrade} are also null (per {@code SubjectGrade.computeAverage()}).
     */
    public record SubjectRow(
            String subjectName,
            BigDecimal regularScore,
            BigDecimal midtermScore,
            BigDecimal finalScore,
            BigDecimal average,
            String letterGrade
    ) {
    }
}
