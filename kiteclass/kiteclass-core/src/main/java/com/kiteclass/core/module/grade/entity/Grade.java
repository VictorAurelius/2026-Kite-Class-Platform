package com.kiteclass.core.module.grade.entity;

import com.kiteclass.core.common.constant.GradeStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Grade entity representing final grade for a student in a class.
 *
 * <p>Business Logic:
 * <ul>
 *   <li>BR-GRADE-001: Unique (student_id, class_id) - one grade per student per class</li>
 *   <li>BR-GRADE-002: Final score calculated from weighted grade components</li>
 *   <li>BR-GRADE-003: Letter grade and GPA mapped from final_score using grading_scale</li>
 *   <li>BR-GRADE-004: Status lifecycle: IN_PROGRESS → FINALIZED → PASSED/FAILED</li>
 *   <li>BR-GRADE-005: Pass/Fail determined by final_score vs pass_threshold</li>
 *   <li>BR-GRADE-006: Cannot finalize if component weights != 100%</li>
 *   <li>BR-GRADE-007: Finalized grades are immutable (require unfinalizing)</li>
 * </ul>
 *
 * <p>Relationships:
 * <ul>
 *   <li>Grade → Student (many-to-one)</li>
 *   <li>Grade → Class (many-to-one)</li>
 *   <li>Grade → GradeComponents (one-to-many, cascade all)</li>
 * </ul>
 *
 * <p>Grade Calculation Formula:
 * <pre>
 * final_score = Σ(component.weighted_score)
 * where weighted_score = (score/max_score * 100) * (weight_percent/100)
 * </pre>
 *
 * <p>Example:
 * <pre>
 * Attendance: 34/36 sessions (94.4%) * 10% weight = 9.44 points
 * Assignment: 87.67/100 * 30% weight = 26.30 points
 * Midterm: 82/100 * 25% weight = 20.50 points
 * Final: 88/100 * 35% weight = 30.80 points
 * ───────────────────────────────────────────────
 * final_score = 87.04/100 (B+, GPA 3.3)
 * </pre>
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Entity
@Table(name = "grades", uniqueConstraints = {
        @UniqueConstraint(name = "uk_grades_student_class_type", columnNames = {"student_id", "class_id", "grade_type"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grade extends BaseEntity {

    /**
     * Student ID (FK to students.id).
     * Cannot be null.
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * Class ID (FK to classes.id).
     * Cannot be null.
     */
    @Column(name = "class_id", nullable = false)
    private Long classId;

    /**
     * Final calculated score (0-100).
     * Null if not yet calculated.
     */
    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    /**
     * Letter grade (A+, A, B+, B, C+, C, D+, D, F).
     * Mapped from final_score using grading_scale.
     * Null if not yet calculated.
     */
    @Column(name = "letter_grade", length = 5)
    private String letterGrade;

    /**
     * GPA (0.0-4.0).
     * Mapped from letter_grade using grading_scale.
     * Null if not yet calculated.
     */
    @Column(name = "gpa", precision = 3, scale = 2)
    private BigDecimal gpa;

    /**
     * Grade status.
     * Default: IN_PROGRESS.
     * Lifecycle: IN_PROGRESS → FINALIZED → PASSED/FAILED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private GradeStatus status = GradeStatus.IN_PROGRESS;

    /**
     * Pass threshold score (0-100).
     * Default: 50.0.
     * Student passes if final_score >= pass_threshold.
     */
    @Column(name = "pass_threshold", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal passThreshold = BigDecimal.valueOf(50.0);

    /**
     * Teacher comments and feedback.
     * Max 2000 characters.
     */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    /**
     * Timestamp when final_score was calculated.
     */
    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    /**
     * Timestamp when grade was finalized.
     * Finalized grades are locked and cannot be changed.
     */
    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    /**
     * Teacher ID who finalized the grade.
     */
    @Column(name = "finalized_by")
    private Long finalizedBy;

    /**
     * Grade components (attendance, assignments, exams, etc.).
     * One-to-many relationship, cascade all operations.
     */
    @OneToMany(mappedBy = "grade", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GradeComponent> components = new ArrayList<>();

    // ==================== Business Methods ====================

    /**
     * Add a grade component to this grade.
     *
     * @param component the component to add
     */
    public void addComponent(GradeComponent component) {
        components.add(component);
        component.setGrade(this);
    }

    /**
     * Remove a grade component from this grade.
     *
     * @param component the component to remove
     */
    public void removeComponent(GradeComponent component) {
        components.remove(component);
        component.setGrade(null);
    }

    /**
     * Calculate final score from all grade components.
     *
     * <p>Formula: final_score = Σ(component.weighted_score)
     *
     * <p>Business Rules:
     * <ul>
     *   <li>BR-GRADE-CALC-001: Sum all weighted_score values</li>
     *   <li>BR-GRADE-CALC-002: Round to 2 decimal places</li>
     *   <li>BR-GRADE-CALC-003: Clamp result to [0, 100] range</li>
     *   <li>BR-GRADE-CALC-004: Update calculated_at timestamp</li>
     * </ul>
     *
     * @return calculated final score (0-100)
     */
    public BigDecimal calculateFinalScore() {
        BigDecimal sum = components.stream()
                .map(GradeComponent::getWeightedScore)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Round to 2 decimal places
        sum = sum.setScale(2, RoundingMode.HALF_UP);

        // Clamp to [0, 100] range
        if (sum.compareTo(BigDecimal.ZERO) < 0) {
            sum = BigDecimal.ZERO;
        }
        if (sum.compareTo(BigDecimal.valueOf(100)) > 0) {
            sum = BigDecimal.valueOf(100);
        }

        this.finalScore = sum;
        this.calculatedAt = LocalDateTime.now();

        return sum;
    }

    /**
     * Validate that component weights sum to 100%.
     *
     * <p>Business Rule BR-GRADE-006: Cannot finalize if weights != 100%
     *
     * @return true if weights sum to 100%, false otherwise
     */
    public boolean isWeightsSumValid() {
        BigDecimal sum = components.stream()
                .map(GradeComponent::getWeightPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.compareTo(BigDecimal.valueOf(100)) == 0;
    }

    /**
     * Finalize this grade.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>BR-GRADE-FIN-001: Requires valid weights (sum = 100%)</li>
     *   <li>BR-GRADE-FIN-002: Calculate final_score if not already done</li>
     *   <li>BR-GRADE-FIN-003: Set status to FINALIZED</li>
     *   <li>BR-GRADE-FIN-004: Determine PASSED/FAILED based on pass_threshold</li>
     *   <li>BR-GRADE-FIN-005: Set finalized_at timestamp</li>
     *   <li>BR-GRADE-FIN-006: Record finalizedBy teacher ID</li>
     * </ul>
     *
     * @param teacherId the teacher ID finalizing the grade
     * @throws IllegalStateException if weights don't sum to 100%
     */
    public void finalize(Long teacherId) {
        if (!isWeightsSumValid()) {
            throw new IllegalStateException("Cannot finalize grade: component weights must sum to 100%");
        }

        // Calculate final score if not already done
        if (this.finalScore == null) {
            calculateFinalScore();
        }

        // Determine pass/fail
        if (this.finalScore.compareTo(this.passThreshold) >= 0) {
            this.status = GradeStatus.PASSED;
        } else {
            this.status = GradeStatus.FAILED;
        }

        this.finalizedAt = LocalDateTime.now();
        this.finalizedBy = teacherId;
    }

    /**
     * Unfinalize this grade (allow modifications).
     *
     * <p>Business Rule BR-GRADE-003: Admin can unfinalize to allow changes
     *
     * <p>Note: This resets status to IN_PROGRESS but preserves final_score
     */
    public void unfinalize() {
        this.status = GradeStatus.IN_PROGRESS;
        this.finalizedAt = null;
        this.finalizedBy = null;
    }

    /**
     * Check if this grade is finalized.
     *
     * @return true if status is FINALIZED, PASSED, or FAILED
     */
    public boolean isFinalized() {
        return status == GradeStatus.FINALIZED
                || status == GradeStatus.PASSED
                || status == GradeStatus.FAILED;
    }

    /**
     * Check if student passed.
     *
     * @return true if status is PASSED
     */
    public boolean isPassed() {
        return status == GradeStatus.PASSED;
    }

    /**
     * Check if student failed.
     *
     * @return true if status is FAILED
     */
    public boolean isFailed() {
        return status == GradeStatus.FAILED;
    }

    /**
     * Set letter grade and GPA from grading scale.
     *
     * <p>This method should be called after calculating final_score
     * to map the numeric score to letter grade and GPA.
     *
     * @param letterGrade the letter grade (A+, A, B+, etc.)
     * @param gpa         the GPA value (0.0-4.0)
     */
    public void setGradeMapping(String letterGrade, BigDecimal gpa) {
        this.letterGrade = letterGrade;
        this.gpa = gpa;
    }
}
