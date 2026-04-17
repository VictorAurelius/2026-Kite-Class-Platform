package com.kiteclass.core.module.grade.entity;

import com.kiteclass.core.common.constant.GradeComponentType;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Grade component entity representing individual score components.
 *
 * <p>Examples:
 * <ul>
 *   <li>ATTENDANCE: Attendance rate (34/36 sessions = 94.4%)</li>
 *   <li>ASSIGNMENT: Assignment scores (Assignment 1, 2, 3...)</li>
 *   <li>MIDTERM: Midterm exam score</li>
 *   <li>FINAL: Final exam score</li>
 *   <li>QUIZ: Quiz scores</li>
 *   <li>PROJECT: Project scores</li>
 *   <li>PARTICIPATION: Class participation</li>
 * </ul>
 *
 * <p>Business Logic:
 * <ul>
 *   <li>BR-COMP-001: weighted_score auto-calculated from score/max_score and weight</li>
 *   <li>BR-COMP-002: Unique (grade_id, component_type, component_ref_id)</li>
 *   <li>BR-COMP-003: ATTENDANCE/ASSIGNMENT components auto-updated via events</li>
 *   <li>BR-COMP-004: Other components manually entered by teacher</li>
 *   <li>BR-COMP-005: weight_percent must be 0-100</li>
 * </ul>
 *
 * <p>Weighted Score Calculation:
 * <pre>
 * weighted_score = (score / max_score * 100) * (weight_percent / 100)
 *
 * Example:
 * - score = 85, max_score = 100, weight_percent = 30
 * - weighted_score = (85/100 * 100) * (30/100) = 85 * 0.3 = 25.5 points
 * </pre>
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Entity
@Table(name = "grade_components", uniqueConstraints = {
        @UniqueConstraint(name = "uk_grade_components_ref",
                columnNames = {"grade_id", "component_type", "component_ref_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeComponent extends BaseEntity {

    /**
     * Parent grade.
     * Many-to-one relationship, cannot be null.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grade_id", nullable = false)
    private Grade grade;

    /**
     * Component type (ATTENDANCE, ASSIGNMENT, MIDTERM, FINAL, etc.).
     * Cannot be null.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 50)
    private GradeComponentType componentType;

    /**
     * Component name (e.g., "Assignment 1", "Midterm Exam", "Attendance").
     * Cannot be null, max 255 characters.
     */
    @Column(name = "component_name", nullable = false)
    private String componentName;

    /**
     * Reference ID to source record.
     * <ul>
     *   <li>ATTENDANCE: null (calculated from attendance records)</li>
     *   <li>ASSIGNMENT: assignment_id (submission ID)</li>
     *   <li>MIDTERM/FINAL/QUIZ/PROJECT: null (manual entry)</li>
     * </ul>
     */
    @Column(name = "component_ref_id")
    private Long componentRefId;

    /**
     * Actual score achieved.
     * Cannot be null, must be >= 0.
     */
    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    /**
     * Maximum possible score.
     * Cannot be null, must be > 0.
     */
    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    /**
     * Weight percentage in final grade (0-100).
     * Cannot be null.
     * Example: 30.0 means this component contributes 30% to final grade.
     */
    @Column(name = "weight_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightPercent;

    /**
     * Weighted score contribution to final grade.
     * Formula: (score/max_score * 100) * (weight_percent/100)
     * Auto-calculated, rounded to 2 decimal places.
     */
    @Column(name = "weighted_score", precision = 5, scale = 2)
    private BigDecimal weightedScore;

    // ==================== Business Methods ====================

    /**
     * Calculate weighted score from score, max_score, and weight_percent.
     *
     * <p>Formula: weighted_score = (score / max_score * 100) * (weight_percent / 100)
     *
     * <p>Example:
     * <pre>
     * score = 85, max_score = 100, weight = 30%
     * percentage = 85/100 * 100 = 85%
     * weighted_score = 85 * 0.3 = 25.5 points
     * </pre>
     *
     * <p>Business Rules:
     * <ul>
     *   <li>BR-COMP-CALC-001: Round to 2 decimal places</li>
     *   <li>BR-COMP-CALC-002: Handle division by zero (max_score > 0 enforced by constraint)</li>
     *   <li>BR-COMP-CALC-003: Result clamped to [0, 100] range</li>
     * </ul>
     *
     * @return weighted score contribution (0-100)
     */
    public BigDecimal calculateWeightedScore() {
        if (this.maxScore.compareTo(BigDecimal.ZERO) == 0) {
            this.weightedScore = BigDecimal.ZERO;
            return BigDecimal.ZERO;
        }

        // Calculate percentage: (score / max_score) * 100
        BigDecimal percentage = this.score
                .divide(this.maxScore, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Apply weight: percentage * (weight_percent / 100)
        BigDecimal weighted = percentage
                .multiply(this.weightPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Clamp to [0, weight_percent] range
        if (weighted.compareTo(BigDecimal.ZERO) < 0) {
            weighted = BigDecimal.ZERO;
        }
        if (weighted.compareTo(this.weightPercent) > 0) {
            weighted = this.weightPercent;
        }

        this.weightedScore = weighted;
        return weighted;
    }

    /**
     * Update score and recalculate weighted score.
     *
     * <p>Business Rule BR-COMP-UPDATE-001: Auto-recalculate when score changes
     *
     * @param newScore the new score value
     */
    public void updateScore(BigDecimal newScore) {
        this.score = newScore;
        calculateWeightedScore();
    }

    /**
     * Update weight and recalculate weighted score.
     *
     * <p>Business Rule BR-COMP-UPDATE-002: Auto-recalculate when weight changes
     *
     * @param newWeight the new weight percentage (0-100)
     */
    public void updateWeight(BigDecimal newWeight) {
        this.weightPercent = newWeight;
        calculateWeightedScore();
    }

    /**
     * Get percentage score (0-100).
     *
     * @return score as percentage of max_score
     */
    public BigDecimal getPercentage() {
        if (this.maxScore.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return this.score
                .divide(this.maxScore, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
