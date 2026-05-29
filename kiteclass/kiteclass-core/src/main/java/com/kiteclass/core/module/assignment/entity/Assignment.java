package com.kiteclass.core.module.assignment.entity;

import com.kiteclass.core.common.constant.AssignmentStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Assignment entity - Teacher-created assignments for classes.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment extends BaseEntity {

    /**
     * Class ID this assignment belongs to.
     */
    @Column(name = "class_id", nullable = false)
    private Long classId;

    /**
     * Assignment title.
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Assignment description.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Detailed instructions for students.
     */
    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    /**
     * Due date and time.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    /**
     * Maximum score for this assignment.
     */
    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    /**
     * Weight percentage in final grade (0-100%).
     */
    @Column(name = "weight_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightPercent;

    /**
     * Whether late submissions are allowed.
     */
    @Column(name = "allow_late_submission", nullable = false)
    private Boolean allowLateSubmission;

    /**
     * Late penalty percentage per day (default 10%).
     */
    @Column(name = "late_penalty_percent", precision = 5, scale = 2)
    private BigDecimal latePenaltyPercent;

    /**
     * Assignment status (DRAFT, PUBLISHED, CLOSED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssignmentStatus status;

    // GAP-795: the audit actor column `created_by` is now a UUID owned by BaseEntity
    // (@CreatedBy, populated from UserContext = X-User-Id). This entity previously
    // shadowed it with a Long to stash the numeric X-Teacher-Id — a misuse of the
    // audit column that conflicted with the UUID migration. The override is removed;
    // `created_by` now holds the actor UUID via BaseEntity auditing. Teacher attribution
    // remains derivable via the TeacherClass MAIN_TEACHER relationship checked at create time.

    /**
     * Publish the assignment (make visible to students).
     */
    public void publish() {
        if (this.status == AssignmentStatus.DRAFT) {
            this.status = AssignmentStatus.PUBLISHED;
        }
    }

    /**
     * Close the assignment (no more submissions).
     */
    public void close() {
        if (this.status == AssignmentStatus.PUBLISHED) {
            this.status = AssignmentStatus.CLOSED;
        }
    }

    /**
     * Check if assignment is published and accepting submissions.
     */
    public boolean isAcceptingSubmissions() {
        return this.status == AssignmentStatus.PUBLISHED &&
               (allowLateSubmission || LocalDateTime.now().isBefore(dueDate));
    }

    /**
     * Calculate late penalty for a submission date.
     *
     * @param submissionDate the submission date
     * @return penalty multiplier (0.0 to 1.0), where 1.0 = no penalty
     */
    public BigDecimal calculateLatePenaltyMultiplier(LocalDateTime submissionDate) {
        if (submissionDate.isBefore(dueDate) || submissionDate.isEqual(dueDate)) {
            return BigDecimal.ONE; // No penalty for on-time submissions
        }

        if (!allowLateSubmission) {
            return BigDecimal.ZERO; // Late not allowed = 0 score
        }

        // Calculate days late (ceiling to penalize partial days)
        long hoursLate = java.time.Duration.between(dueDate, submissionDate).toHours();
        int daysLate = (int) Math.ceil(hoursLate / 24.0);

        // Calculate penalty: 1 - (daysLate * penalty%)
        BigDecimal totalPenalty = latePenaltyPercent
                .multiply(BigDecimal.valueOf(daysLate))
                .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);

        BigDecimal multiplier = BigDecimal.ONE.subtract(totalPenalty);

        // Ensure multiplier is between 0 and 1
        if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (multiplier.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }

        return multiplier;
    }
}
