package com.kiteclass.core.module.lms.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lesson entity - 3rd tier in Course → Module → Lesson hierarchy.
 * Represents individual learning content within a module.
 *
 * Business Rules:
 * - BR-LMS-001: Guest can only access lessons where isTrial=true
 * - BR-LMS-002: Student must have active enrollment to access paid lessons
 * - BR-LMS-008: Order number must be unique within module
 *
 * @since 2.9.0
 */
@Entity
@Table(
    name = "lessons",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_lessons_module_order",
            columnNames = {"module_id", "order_number", "instance_id"}
        )
    },
    indexes = {
        @Index(name = "idx_lessons_module_id", columnList = "module_id"),
        @Index(name = "idx_lessons_is_trial", columnList = "is_trial"),
        @Index(name = "idx_lessons_instance_id", columnList = "instance_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson extends BaseEntity {

    /**
     * Foreign key to CourseModule entity
     */
    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    /**
     * Lesson title (e.g., "Variables and Data Types", "Introduction to Hooks")
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Lesson content (markdown supported, text-based learning material)
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * Video URL (YouTube, Vimeo, S3, etc.)
     * Phase 1: stored as text, no Media Service integration yet
     */
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /**
     * Trial lesson flag - controls guest access.
     * If true, guest users can access this lesson without enrollment (BR-LMS-001).
     * If false, only enrolled students can access (BR-LMS-002).
     */
    @Column(name = "is_trial", nullable = false)
    @Builder.Default
    private Boolean isTrial = false;

    /**
     * Display order within the module (must be unique per module).
     * Used for sorting lessons in the UI.
     */
    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    /**
     * Estimated lesson duration in minutes (optional)
     */
    @Column(name = "estimated_duration")
    private Integer estimatedDuration;

    /**
     * Helper method to check if this is a trial lesson.
     * Used in permission checks to allow guest access.
     *
     * @return true if this is a trial lesson, false otherwise
     */
    public boolean isTrialLesson() {
        return Boolean.TRUE.equals(isTrial);
    }
}
