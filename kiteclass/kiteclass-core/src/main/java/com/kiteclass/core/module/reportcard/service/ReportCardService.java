package com.kiteclass.core.module.reportcard.service;

import com.kiteclass.core.module.reportcard.dto.ReportCardData;

/**
 * Aggregates K-12 SubjectGrade rows into a per-student per-semester
 * {@link ReportCardData} payload, ready for {@code ReportCardRenderer}
 * (Task 4 of GAP-055).
 *
 * <p>Multi-tenant isolation: relies on Hibernate {@code @Filter} on
 * {@code BaseEntity}-derived repositories. Cross-tenant access surfaces
 * as {@link com.kiteclass.core.common.exception.EntityNotFoundException}
 * rather than 403 — see UC-RC-05.
 *
 * @since 3.18.0 (GAP-055 Phase 1)
 */
public interface ReportCardService {

    /**
     * Build the report card payload for {@code (studentId, semesterId)}.
     *
     * @param studentId student in the current tenant
     * @param semesterId semester in the current tenant
     * @return aggregated report-card data; never null
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException
     *         with code {@code REPORT_CARD_STUDENT_NOT_FOUND},
     *         {@code REPORT_CARD_SEMESTER_NOT_FOUND}, or
     *         {@code REPORT_CARD_NO_GRADES} per BR-RC-AGG-005.
     */
    ReportCardData generateReportCard(Long studentId, Long semesterId);
}
