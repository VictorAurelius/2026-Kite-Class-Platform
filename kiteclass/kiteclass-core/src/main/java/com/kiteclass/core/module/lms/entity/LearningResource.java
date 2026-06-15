package com.kiteclass.core.module.lms.entity;

import com.kiteclass.core.common.constant.ResourceType;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LearningResource entity - supplemental materials attached to lessons.
 * Represents additional resources like PDFs, slides, code samples, etc.
 *
 * Phase 1 Status: Optional nice-to-have, basic implementation
 * Future: Integration with File Storage Module (PR 2.10.1) for uploads
 *
 * @since 2.9.0
 */
@Entity
@Table(
    name = "learning_resources",
    indexes = {
        @Index(name = "idx_learning_resources_lesson_id", columnList = "lesson_id"),
        @Index(name = "idx_learning_resources_type", columnList = "type"),
        @Index(name = "idx_learning_resources_instance_id", columnList = "instance_id"),
        @Index(name = "idx_learning_resources_uploaded_file_id", columnList = "uploaded_file_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningResource extends BaseEntity {

    /**
     * Foreign key to Lesson entity
     */
    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    /**
     * Resource type (VIDEO, PDF, SLIDE, AUDIO, LINK, CODE, OTHER)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ResourceType type;

    /**
     * Resource URL (S3 URL, external link, YouTube URL, etc.)
     */
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    /**
     * Resource title (e.g., "Chapter 1 Slides", "Exercise Solutions")
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * File size in bytes (optional, null for external links)
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * GAP-1307: deterministic FK link to the backing {@code uploaded_files} row (storage
     * module) when this resource is a stored material. Nullable — external links / YouTube /
     * legacy rows have no backing uploaded file and stay un-paywalled.
     *
     * <p>Scalar FK (not a {@code @ManyToOne}) to keep the LMS entity free of a storage import
     * and to mirror the existing {@code lessonId} convention. Used by the storage download
     * paywall ({@code LessonMaterialAccessGuard}) to resolve file → lesson → course →
     * enrollment without the fragile {@code url}-substring heuristic.
     */
    @Column(name = "uploaded_file_id")
    private Long uploadedFileId;
}
