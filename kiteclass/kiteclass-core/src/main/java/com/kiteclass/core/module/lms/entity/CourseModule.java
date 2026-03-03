package com.kiteclass.core.module.lms.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CourseModule entity - 2nd tier in Course → Module → Lesson hierarchy.
 * Represents a logical grouping of lessons within a course.
 *
 * Business Rules:
 * - BR-LMS-005: Modules belong to a course (foreign key to Course)
 * - BR-LMS-006: Order number must be unique within course/module
 * - BR-LMS-007: Cannot delete module if it has lessons (enforced by service layer)
 *
 * @since 2.9.0
 */
@Entity
@Table(
    name = "course_modules",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_course_modules_course_order",
            columnNames = {"course_id", "order_number", "instance_id"}
        )
    },
    indexes = {
        @Index(name = "idx_course_modules_course_id", columnList = "course_id"),
        @Index(name = "idx_course_modules_instance_id", columnList = "instance_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseModule extends BaseEntity {

    /**
     * Foreign key to Course entity (from PR 2.4)
     */
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /**
     * Module title (e.g., "Introduction to Java", "Advanced React Patterns")
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Module description (markdown supported)
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Display order within the course (must be unique per course).
     * Used for sorting modules in the UI.
     */
    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    /**
     * Version field for optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;
}
