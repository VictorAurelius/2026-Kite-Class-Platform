package com.kiteclass.core.module.k12.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.k12.enums.SubjectGradeStatus;
import com.kiteclass.core.module.k12.enums.SubjectGradeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * SubjectGrade — điểm của 1 học sinh cho 1 môn trong 1 học kỳ.
 *
 * <p>VN K-12 grading structure (typical):
 * <ul>
 *   <li>Điểm thường xuyên (regular): continuous assessments, weight 1</li>
 *   <li>Điểm giữa kỳ (midterm): 1 exam, weight 2</li>
 *   <li>Điểm cuối kỳ (final): 1 exam, weight 3</li>
 * </ul>
 * <p>Average = (regular × 1 + midterm × 2 + final × 3) / 6 per TT 22/2021/TT-BGDĐT Đ.7
 *
 * <p>Phase 1C extension (Wave 19 Bucket B — GAP-323c Phase 1C v1):
 * Adds {@link #type}, {@link #weight}, {@link #status}, {@link #reviewedBy},
 * {@link #publishedAt} for Tổ trưởng workflow + per-record assessment typing.
 * Existing rows are backfilled by V55 migration to {@code DRAFT} / {@code TX} / {@code 1.0}
 * for backward compatibility.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-SG-001: Unique (student, subjectSection, semester)</li>
 *   <li>BR-SG-002: All scores 0.0-10.0 (VN 10-point scale)</li>
 *   <li>BR-SG-003: Average auto-computed from components</li>
 *   <li>BR-SG-004: Letter grade derived from average (Giỏi ≥8, Khá ≥6.5, TB ≥5, Yếu &lt;5)</li>
 *   <li>BR-GRADEBOOK-001..005: see {@code documents/01-business/kiteclass/multi-subject-gradebook/rules.md}</li>
 * </ul>
 *
 * @since 3.15.0 (GAP-054); Phase 1C extension 5.x (Wave 19 Bucket B — GAP-323c)
 */
@Entity
@Table(
        name = "subject_grades",
        indexes = {
                @Index(name = "idx_sg_student_section_semester",
                        columnList = "student_id,subject_section_id,semester_id", unique = true),
                @Index(name = "idx_sg_instance_id", columnList = "instance_id"),
                @Index(name = "idx_sg_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectGrade extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_section_id", nullable = false)
    private SubjectSection subjectSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    /**
     * Điểm thường xuyên (regular assessment).
     */
    @Column(name = "regular_score", precision = 4, scale = 2)
    private BigDecimal regularScore;

    /**
     * Điểm giữa kỳ.
     */
    @Column(name = "midterm_score", precision = 4, scale = 2)
    private BigDecimal midtermScore;

    /**
     * Điểm cuối kỳ.
     */
    @Column(name = "final_score", precision = 4, scale = 2)
    private BigDecimal finalScore;

    /**
     * Auto-computed weighted average.
     */
    @Column(name = "average", precision = 4, scale = 2)
    private BigDecimal average;

    /**
     * Letter grade (Giỏi, Khá, Trung bình, Yếu).
     */
    @Column(name = "letter_grade", length = 20)
    private String letterGrade;

    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Phase 1C — assessment type (TX/GK/CK) per TT 22/2021 Điều 7.
     *
     * <p>Existing rows default to {@link SubjectGradeType#TX} via V55 migration
     * (regular continuous assessment was the only kind tracked pre-Phase-1C).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 8)
    private SubjectGradeType type;

    /**
     * Phase 1C — weight multiplier for this record. TX=1.0, GK=2.0, CK=3.0
     * per BR-GRADEBOOK-004. Stored explicitly so future TT amendments can
     * override per-tenant without code change.
     */
    @Column(name = "weight", precision = 4, scale = 2)
    private BigDecimal weight;

    /**
     * Phase 1C — Tổ trưởng approval lifecycle. Existing rows default to
     * {@link SubjectGradeStatus#DRAFT} via V55 migration.
     *
     * <p>Phase 1C v1 (Wave 19) persists the column; full state-machine
     * enforcement deferred to follow-up gap (see GAP-323c Out-of-scope).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16)
    private SubjectGradeStatus status;

    /**
     * Phase 1C — userId of Tổ trưởng who reviewed this grade
     * (when {@link #status} ≥ {@link SubjectGradeStatus#REVIEWED}).
     */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    /**
     * Phase 1C — instant when Hiệu trưởng published this grade
     * (when {@link #status} = {@link SubjectGradeStatus#PUBLISHED}).
     */
    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Recompute average based on current scores.
     * Formula: (regular × 1 + midterm × 2 + final × 3) / 6
     * Requires all 3 scores non-null.
     */
    public void computeAverage() {
        if (regularScore == null || midtermScore == null || finalScore == null) {
            this.average = null;
            this.letterGrade = null;
            return;
        }
        BigDecimal total = regularScore
                .add(midtermScore.multiply(BigDecimal.valueOf(2)))
                .add(finalScore.multiply(BigDecimal.valueOf(3)));
        this.average = total.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);
        this.letterGrade = deriveLetterGrade(this.average);
    }

    /**
     * Derive Vietnamese letter grade from numeric average.
     */
    public static String deriveLetterGrade(BigDecimal avg) {
        if (avg == null) {
            return null;
        }
        if (avg.compareTo(BigDecimal.valueOf(8.0)) >= 0) {
            return "Giỏi";
        }
        if (avg.compareTo(BigDecimal.valueOf(6.5)) >= 0) {
            return "Khá";
        }
        if (avg.compareTo(BigDecimal.valueOf(5.0)) >= 0) {
            return "Trung bình";
        }
        return "Yếu";
    }
}
