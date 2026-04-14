package com.kiteclass.core.module.k12.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.academicyear.entity.Semester;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SubjectGrade — điểm của 1 học sinh cho 1 môn trong 1 học kỳ.
 *
 * <p>VN K-12 grading structure (typical):
 * <ul>
 *   <li>Điểm thường xuyên (regular): continuous assessments, weight 1</li>
 *   <li>Điểm giữa kỳ (midterm): 1 exam, weight 2</li>
 *   <li>Điểm cuối kỳ (final): 1 exam, weight 3</li>
 * </ul>
 * <p>Average = (regular × 1 + midterm × 2 + final × 3) / 6
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-SG-001: Unique (student, subjectSection, semester)</li>
 *   <li>BR-SG-002: All scores 0.0-10.0 (VN 10-point scale)</li>
 *   <li>BR-SG-003: Average auto-computed from components</li>
 *   <li>BR-SG-004: Letter grade derived from average (Giỏi ≥8, Khá ≥6.5, TB ≥5, Yếu <5)</li>
 * </ul>
 *
 * @since 3.15.0 (GAP-054)
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
