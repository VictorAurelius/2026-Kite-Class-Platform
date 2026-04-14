package com.kiteclass.core.module.academicyear.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Semester within an academic year.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-SEM-001: Belongs to 1 academic year</li>
 *   <li>BR-SEM-002: Unique (academicYear, type) — 1 HK1 per year</li>
 *   <li>BR-SEM-003: endDate > startDate</li>
 *   <li>BR-SEM-004: examStartDate/endDate optional, within semester period</li>
 * </ul>
 *
 * @since 3.15.0 (GAP-053, ADR-002)
 */
@Entity
@Table(
        name = "semesters",
        indexes = {
                @Index(name = "idx_semesters_year_type", columnList = "academic_year_id,type", unique = true),
                @Index(name = "idx_semesters_instance_id", columnList = "instance_id"),
                @Index(name = "idx_semesters_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Semester extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private SemesterType type;

    /**
     * Display name (e.g., "HK1 năm học 2026-2027").
     */
    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Exam period start (optional — for semester finals).
     */
    @Column(name = "exam_start_date")
    private LocalDate examStartDate;

    @Column(name = "exam_end_date")
    private LocalDate examEndDate;

    /**
     * Convenience: check if given date falls within this semester.
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
