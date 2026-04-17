package com.kiteclass.core.module.assignment.entity;

import com.kiteclass.core.common.constant.SubmissionStatus;
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
 * Submission entity - Student submissions for assignments.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission extends BaseEntity {

    /**
     * Assignment ID this submission belongs to.
     */
    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    /**
     * Student ID who submitted.
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * Submission date and time.
     */
    @Column(name = "submission_date", nullable = false)
    private LocalDateTime submissionDate;

    /**
     * URL to uploaded file (S3/Storage Service).
     */
    @Column(name = "content_url", length = 500)
    private String contentUrl;

    /**
     * Student notes/comments.
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Original score before late penalty.
     */
    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    /**
     * Final score after late penalty applied.
     */
    @Column(name = "adjusted_score", precision = 5, scale = 2)
    private BigDecimal adjustedScore;

    /**
     * Submission status (PENDING, GRADED, RETURNED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status;

    /**
     * Teacher ID who graded this submission.
     */
    @Column(name = "graded_by")
    private Long gradedBy;

    /**
     * Grading timestamp.
     */
    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    /**
     * Teacher feedback.
     */
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    /**
     * Grade this submission with late penalty applied.
     *
     * @param originalScore the raw score (0 to max_score)
     * @param penaltyMultiplier the late penalty multiplier (0.0 to 1.0)
     * @param teacherId the teacher grading this submission
     * @param feedback teacher feedback
     */
    public void grade(BigDecimal originalScore, BigDecimal penaltyMultiplier, Long teacherId, String feedback) {
        this.score = originalScore;
        this.adjustedScore = originalScore.multiply(penaltyMultiplier)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        this.status = SubmissionStatus.GRADED;
        this.gradedBy = teacherId;
        this.gradedAt = LocalDateTime.now();
        this.feedback = feedback;
    }

    /**
     * Return graded submission to student (send feedback notification).
     */
    public void returnToStudent() {
        if (this.status == SubmissionStatus.GRADED) {
            this.status = SubmissionStatus.RETURNED;
        }
    }

    /**
     * Check if submission is late.
     *
     * @param dueDate the assignment due date
     * @return true if submitted after due date
     */
    public boolean isLate(LocalDateTime dueDate) {
        return submissionDate.isAfter(dueDate);
    }
}
