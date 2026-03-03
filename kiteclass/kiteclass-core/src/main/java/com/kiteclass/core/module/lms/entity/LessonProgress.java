package com.kiteclass.core.module.lms.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * LessonProgress entity - tracks student progress through lessons.
 *
 * Business Rules:
 * - BR-LMS-009: One progress record per user per lesson
 * - BR-LMS-010: Completing a lesson is idempotent (re-completing doesn't error)
 *
 * Design Note: Tracks userId (not enrollmentId) for future TRIAL_USER support
 *
 * @since 2.9.0
 */
@Entity
@Table(
    name = "lesson_progress",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_lesson_progress_user_lesson",
            columnNames = {"user_id", "lesson_id", "instance_id"}
        )
    },
    indexes = {
        @Index(name = "idx_lesson_progress_user_id", columnList = "user_id"),
        @Index(name = "idx_lesson_progress_lesson_id", columnList = "lesson_id"),
        @Index(name = "idx_lesson_progress_completed", columnList = "completed"),
        @Index(name = "idx_lesson_progress_instance_id", columnList = "instance_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgress extends BaseEntity {

    /**
     * User ID (not enrollmentId - supports future TRIAL_USER tracking)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Foreign key to Lesson entity
     */
    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    /**
     * Whether the lesson has been completed
     */
    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    /**
     * Timestamp when the lesson was completed (null if not completed)
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Progress percentage (0-100).
     * Phase 1: 0 = not started, 100 = completed
     * Future: track intermediate progress for video watching
     */
    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    private Integer progressPercent = 0;

    /**
     * Version field for optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Helper method to mark a lesson as completed (idempotent - BR-LMS-010).
     * Sets completed=true, completedAt=now, progressPercent=100.
     */
    public void markAsCompleted() {
        this.completed = true;
        this.completedAt = LocalDateTime.now();
        this.progressPercent = 100;
    }
}
