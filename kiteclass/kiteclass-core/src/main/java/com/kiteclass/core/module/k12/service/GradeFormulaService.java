package com.kiteclass.core.module.k12.service;

import java.math.BigDecimal;

/**
 * Strategy Pattern — TT 22/2021 grading formula service.
 *
 * <p>Per {@code design-patterns.md} §1.3 — every Strategy interface MUST javadoc
 * the swap reason. This interface exists because Vietnamese MOET periodically
 * amends the K-12 grading thông tư (TT 22/2021 succeeded TT 26/2020 succeeded
 * TT 58/2011); each amendment may change weights or aggregation rules.
 * Implementations:
 * <ul>
 *   <li>{@link GradeFormulaServiceImpl} — TT 22/2021/TT-BGDĐT (current default)</li>
 * </ul>
 * Selected by Spring component injection. Future TT amendment swaps the bean,
 * not the callers.
 *
 * <p>Formulas implemented per TT 22/2021 Đ.7:
 * <ul>
 *   <li>ĐTBmHK (semester average) = (TB.TX + GK*2 + CK*3) / 6</li>
 *   <li>ĐTBmCN (annual average) = (ĐTBmHK1 + 2*ĐTBmHK2) / 3</li>
 * </ul>
 *
 * <p>All results use {@link java.math.RoundingMode#HALF_EVEN} with scale=1
 * per MOET reporting convention (BR-GRADEBOOK-005).
 *
 * <p>Reference: BR-GRADEBOOK-001..005 in
 * {@code documents/01-business/kiteclass/multi-subject-gradebook/rules.md}.
 *
 * @since 5.x (Wave 19 Bucket B — GAP-323c Phase 1C v1)
 */
public interface GradeFormulaService {

    /**
     * Compute ĐTBmHK — semester average for a student in one subject section.
     *
     * <p>Formula: {@code (TB.TX + GK*2 + CK*3) / 6} where {@code TB.TX} is the
     * arithmetic mean of all TX (regular) scores. Result rounded HALF_EVEN
     * scale=1.
     *
     * @param studentId         student id
     * @param subjectSectionId  subject section id
     * @param semesterId        semester id
     * @return ĐTBmHK rounded scale=1, or {@code null} when component data
     *     missing (no GK or no CK score for this period — partial state)
     */
    BigDecimal computeDTBmHK(Long studentId, Long subjectSectionId, Long semesterId);

    /**
     * Compute ĐTBmCN — annual average across two semesters.
     *
     * <p>Formula: {@code (ĐTBmHK1 + 2*ĐTBmHK2) / 3} weighting HK2 more heavily
     * because it builds on HK1 cumulatively. Result rounded HALF_EVEN scale=1.
     *
     * @param studentId         student id
     * @param subjectSectionId  subject section id
     * @param academicYearId    academic year id (resolves both semesters)
     * @return ĐTBmCN rounded scale=1, or {@code null} when either ĐTBmHK is
     *     unavailable
     */
    BigDecimal computeDTBmCN(Long studentId, Long subjectSectionId, Long academicYearId);
}
