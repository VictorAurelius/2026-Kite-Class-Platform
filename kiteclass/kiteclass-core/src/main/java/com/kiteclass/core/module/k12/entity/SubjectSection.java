package com.kiteclass.core.module.k12.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SubjectSection (Lớp bộ môn) — 1 môn học của 1 HomeroomClass.
 *
 * <p>Example: "10A1 - Toán" — học sinh của lớp 10A1 học môn Toán với 1 teacher.
 *
 * <p>Each HomeroomClass has multiple SubjectSections (e.g., 12 sections for 12 subjects).
 * Same students đều tham gia tất cả SubjectSections của homeroom class họ enrolled.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-SSEC-001: Belongs to 1 HomeroomClass + 1 Course</li>
 *   <li>BR-SSEC-002: Unique (homeroomClass, course) — 1 section per subject per class</li>
 *   <li>BR-SSEC-003: Has 1 subject teacher</li>
 *   <li>BR-SSEC-004: Schedule free-form text initially (TODO structured schedule)</li>
 * </ul>
 *
 * @since 3.15.0 (GAP-054)
 */
@Entity
@Table(
        name = "subject_sections",
        indexes = {
                @Index(name = "idx_ssec_homeroom_course", columnList = "homeroom_class_id,course_id", unique = true),
                @Index(name = "idx_ssec_teacher", columnList = "teacher_id"),
                @Index(name = "idx_ssec_instance_id", columnList = "instance_id"),
                @Index(name = "idx_ssec_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homeroom_class_id", nullable = false)
    private HomeroomClass homeroomClass;

    /**
     * Course ID (FK to existing courses table — Course represents subject).
     * Soft reference to avoid tight coupling.
     */
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /**
     * Subject teacher ID.
     */
    @Column(name = "teacher_id")
    private Long teacherId;

    /**
     * Schedule (e.g., "T2,T4,T6 07:00-07:45").
     * Free-form initially; structured schedule in future PR.
     */
    @Column(name = "schedule", length = 200)
    private String schedule;

    /**
     * Weekly hours for this subject (used for curriculum totals).
     */
    @Column(name = "weekly_hours")
    private Integer weeklyHours;
}
