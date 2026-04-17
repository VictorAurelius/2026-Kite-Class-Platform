package com.kiteclass.core.module.grade.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Grading scale configuration entity.
 *
 * <p>Defines letter grade ranges and GPA mappings.
 *
 * <p>Example (Standard Scale):
 * <pre>
 * A+: 95-100 (4.0 GPA, passing)
 * A : 90-94  (4.0 GPA, passing)
 * B+: 85-89  (3.3 GPA, passing)
 * B : 80-84  (3.0 GPA, passing)
 * C+: 75-79  (2.3 GPA, passing)
 * C : 70-74  (2.0 GPA, passing)
 * D+: 65-69  (1.3 GPA, passing)
 * D : 60-64  (1.0 GPA, passing)
 * F : 0-59   (0.0 GPA, failing)
 * </pre>
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-SCALE-001: Ranges must not overlap</li>
 *   <li>BR-SCALE-002: Must cover full 0-100 range</li>
 *   <li>BR-SCALE-003: Default scale auto-created on instance provisioning</li>
 *   <li>BR-SCALE-004: Tenant can customize scales</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Entity
@Table(name = "grading_scales")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingScale extends BaseEntity {

    /**
     * Scale name (e.g., "Standard", "Strict", "Lenient").
     * Cannot be null, max 100 characters.
     */
    @Column(name = "scale_name", nullable = false, length = 100)
    private String scaleName;

    /**
     * Letter grade (A+, A, B+, B, C+, C, D+, D, F).
     * Cannot be null, max 5 characters.
     */
    @Column(name = "letter_grade", nullable = false, length = 5)
    private String letterGrade;

    /**
     * Minimum score for this grade (inclusive).
     * Range: 0-100.
     */
    @Column(name = "min_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal minScore;

    /**
     * Maximum score for this grade (inclusive).
     * Range: 0-100.
     */
    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    /**
     * GPA value (0.0-4.0).
     * Cannot be null.
     */
    @Column(name = "gpa_value", nullable = false, precision = 3, scale = 2)
    private BigDecimal gpaValue;

    /**
     * Whether this is the default scale.
     * Only one default scale per tenant.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Whether this grade is passing.
     * F grade typically has is_passing = false.
     */
    @Column(name = "is_passing", nullable = false)
    @Builder.Default
    private Boolean isPassing = true;

    // ==================== Business Methods ====================

    /**
     * Check if a score falls within this grade range.
     *
     * @param score the score to check
     * @return true if min_score <= score <= max_score
     */
    public boolean containsScore(BigDecimal score) {
        return score.compareTo(minScore) >= 0 && score.compareTo(maxScore) <= 0;
    }
}
