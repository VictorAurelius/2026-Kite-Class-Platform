package com.kiteclass.core.module.grade.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Transcript entity representing student academic record per semester.
 *
 * <p>Example Transcript:
 * <pre>
 * Student: Nguyen Van A
 * Semester: Spring 2026
 * Academic Year: 2026
 * ─────────────────────────────────────
 * Total Credits: 12.0
 * Semester GPA: 3.45
 * Cumulative GPA: 3.52
 * ─────────────────────────────────────
 * Total Courses: 4
 * Passed: 4
 * Failed: 0
 * </pre>
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-TRANS-001: Unique (student_id, semester, academic_year)</li>
 *   <li>BR-TRANS-002: Auto-generated when semester ends</li>
 *   <li>BR-TRANS-003: GPA calculated from all finalized grades in semester</li>
 *   <li>BR-TRANS-004: Cumulative GPA includes all previous semesters</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Entity
@Table(name = "transcripts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transcripts_student_semester",
                columnNames = {"student_id", "semester", "academic_year"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transcript extends BaseEntity {

    /**
     * Student ID (FK to students.id).
     * Cannot be null.
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * Semester name (e.g., "Spring 2026", "Fall 2025").
     * Max 50 characters.
     */
    @Column(name = "semester", length = 50)
    private String semester;

    /**
     * Academic year (e.g., 2026).
     */
    @Column(name = "academic_year")
    private Integer academicYear;

    /**
     * Total credits earned in this semester.
     * Default: 0.
     */
    @Column(name = "total_credits", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal totalCredits = BigDecimal.ZERO;

    /**
     * Semester GPA (0.0-4.0).
     * Calculated from all courses in this semester.
     */
    @Column(name = "semester_gpa", precision = 3, scale = 2)
    private BigDecimal semesterGpa;

    /**
     * Cumulative GPA (0.0-4.0).
     * Calculated from all courses across all semesters.
     */
    @Column(name = "cumulative_gpa", precision = 3, scale = 2)
    private BigDecimal cumulativeGpa;

    /**
     * Total number of courses taken in this semester.
     * Default: 0.
     */
    @Column(name = "total_courses", nullable = false)
    @Builder.Default
    private Integer totalCourses = 0;

    /**
     * Number of courses passed.
     * Default: 0.
     */
    @Column(name = "passed_courses", nullable = false)
    @Builder.Default
    private Integer passedCourses = 0;

    /**
     * Number of courses failed.
     * Default: 0.
     */
    @Column(name = "failed_courses", nullable = false)
    @Builder.Default
    private Integer failedCourses = 0;

    // ==================== Business Methods ====================

    /**
     * Calculate semester GPA from list of finalized grades.
     *
     * <p>Formula: GPA = Σ(grade.gpa * course.credits) / Σ(credits)
     *
     * <p>Business Rule BR-TRANS-GPA-001: Weighted average by credits
     *
     * @return semester GPA (0.0-4.0)
     */
    public BigDecimal calculateSemesterGpa() {
        // Implementation will be in service layer with actual grade data
        return this.semesterGpa;
    }

    /**
     * Update course statistics.
     *
     * @param totalCourses  total courses in semester
     * @param passedCourses courses passed
     * @param failedCourses courses failed
     */
    public void updateCourseStats(int totalCourses, int passedCourses, int failedCourses) {
        this.totalCourses = totalCourses;
        this.passedCourses = passedCourses;
        this.failedCourses = failedCourses;
    }
}
