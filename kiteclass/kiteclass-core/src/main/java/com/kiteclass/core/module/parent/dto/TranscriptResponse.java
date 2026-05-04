package com.kiteclass.core.module.parent.dto;

import java.math.BigDecimal;

/**
 * Read-only transcript projection exposed to a parent for one of their linked
 * children.
 *
 * <p>Phase 1A (GAP-321 K-12 LEGAL): minimum-viable transcript view to satisfy
 * Luật Giáo dục 2019 Đ.83 Khoản 2 right-to-information for parents. PDPL Decree
 * 13/2023 Art 16 children-data special protection: only fields a parent has the
 * legal right to see are projected — no internal grade-component breakdown,
 * teacher remarks, or audit metadata leaks here.
 *
 * <p>Phase 1B (GAP-321b — sister gap): subject-level grade breakdown, conduct
 * rating, attendance summary will arrive as separate DTOs.
 *
 * @param transcriptId   primary key (opaque to parent UI; useful for caching)
 * @param studentId      child's id (always matches the path parameter)
 * @param semester       e.g. "Spring 2026" / "Fall 2025"
 * @param academicYear   year the semester starts (e.g. 2026)
 * @param totalCredits   credits earned this semester
 * @param semesterGpa    semester GPA on 0.00–4.00 scale (nullable until calc)
 * @param cumulativeGpa  cumulative GPA across all semesters (nullable)
 * @param totalCourses   total courses taken
 * @param passedCourses  courses passed
 * @param failedCourses  courses failed
 * @author KiteClass Team
 * @since 2.18.0 (Wave 18b1 — GAP-321 Phase 1A)
 */
public record TranscriptResponse(
        Long transcriptId,
        Long studentId,
        String semester,
        Integer academicYear,
        BigDecimal totalCredits,
        BigDecimal semesterGpa,
        BigDecimal cumulativeGpa,
        Integer totalCourses,
        Integer passedCourses,
        Integer failedCourses
) {
}
