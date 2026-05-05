package com.kiteclass.core.module.k12.enums;

/**
 * SubjectGrade type — assessment kind per TT 22/2021/TT-BGDĐT Đ.7.
 *
 * <p>VN K-12 grading uses three assessment types within a semester:
 * <ul>
 *   <li>{@link #TX} — Điểm thường xuyên (regular continuous assessment)</li>
 *   <li>{@link #GK} — Điểm giữa kỳ (midterm exam, weight 2)</li>
 *   <li>{@link #CK} — Điểm cuối kỳ (final exam, weight 3)</li>
 * </ul>
 *
 * <p>Formula ĐTBmHK = (TB.TX + GK*2 + CK*3) / 6 per Điều 7 TT 22/2021.
 *
 * <p>Reference: BR-GRADEBOOK-001..004 trong
 * {@code documents/01-business/kiteclass/multi-subject-gradebook/rules.md}.
 *
 * @since 5.x (Wave 19 Bucket B — GAP-323c Phase 1C v1)
 */
public enum SubjectGradeType {

    /** Điểm thường xuyên — regular continuous assessment, weight 1. */
    TX,

    /** Điểm giữa kỳ — midterm exam, weight 2. */
    GK,

    /** Điểm cuối kỳ — final exam, weight 3. */
    CK
}
