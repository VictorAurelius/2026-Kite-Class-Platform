package com.kiteclass.core.module.k12.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HomeroomClass (Lớp chính) — K-12 school model.
 *
 * <p>Represents a cohort of students studying multiple subjects together.
 * Example: "10A1" = Grade 10, Section A1, ~30 students, 1 GVCN (homeroom teacher).
 *
 * <p>Each HomeroomClass maps to multiple SubjectSections (1 per subject in curriculum).
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-HRC-001: Belongs to 1 academic year</li>
 *   <li>BR-HRC-002: Unique (tenant, academicYear, grade+section)</li>
 *   <li>BR-HRC-003: Has 1 homeroom teacher (GVCN) — GAP-056</li>
 *   <li>BR-HRC-004: Capacity typically 30-50 students</li>
 * </ul>
 *
 * <p>ADR-001 Strangler Fig: coexists với existing Class entity.
 * Tenant opts into K-12 model via feature flag.
 *
 * @since 3.15.0 (GAP-054)
 */
@Entity
@Table(
        name = "homeroom_classes",
        indexes = {
                @Index(name = "idx_hrc_academic_year", columnList = "academic_year_id"),
                @Index(name = "idx_hrc_grade_section", columnList = "grade,section"),
                @Index(name = "idx_hrc_homeroom_teacher", columnList = "homeroom_teacher_id"),
                @Index(name = "idx_hrc_instance_id", columnList = "instance_id"),
                @Index(name = "idx_hrc_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeroomClass extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    /**
     * Grade level — "1"..."12" cho K-12, "ĐH" cho đại học.
     */
    @Column(name = "grade", nullable = false, length = 10)
    private String grade;

    /**
     * Section identifier within grade — "A1", "B2", "C", etc.
     */
    @Column(name = "section", nullable = false, length = 20)
    private String section;

    /**
     * ID of homeroom teacher (Giáo viên Chủ nhiệm / GVCN).
     * FK soft reference — full relation managed at service layer to avoid circular deps.
     */
    @Column(name = "homeroom_teacher_id")
    private Long homeroomTeacherId;

    @Column(name = "capacity", nullable = false)
    @Builder.Default
    private Integer capacity = 40;

    @Column(name = "current_enrolled", nullable = false)
    @Builder.Default
    private Integer currentEnrolled = 0;

    @Column(name = "description", length = 500)
    private String description;

    /**
     * Convenience: get full display name "10A1".
     */
    public String getFullName() {
        return grade + section;
    }

    /**
     * Check if class has capacity for new enrollment.
     */
    public boolean hasCapacity() {
        return currentEnrolled < capacity;
    }
}
