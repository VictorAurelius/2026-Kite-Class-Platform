package com.kiteclass.core.module.attendance.entity;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AttendancePeriod — per-period (per-tiết) attendance for K-12 schools.
 *
 * <p>K-12 fundamentally differs from centers: 5-10 tiết/day, each tiết a
 * different bộ môn teacher. The legacy {@link Attendance} entity tracks one
 * record per (enrollment, session) which fits the center model. K-12 schools
 * (`tenant.vertical_type = 'K12_SCHOOL'`) need finer granularity per
 * TT 22/2021/TT-BGDĐT and the GVCN ≤2 min điểm danh target (P5 AC-OPS-001).
 *
 * <p>Phase 1A (Wave 18b1, GAP-323) ships this entity + read-only API only.
 * Write API, GVCN mobile UI, daily roll-up view, and concurrent load test
 * are deferred to GAP-323b. GradeFormulaService + state machine deferred to
 * GAP-323c.
 *
 * <p>Business Rules (BR-PERIOD-ATT-*, see
 * {@code documents/01-business/kiteclass/period-attendance/rules.md}):
 * <ul>
 *   <li>BR-PERIOD-ATT-001: subject_section_id must reference an existing
 *       SubjectSection (GAP-054 Phase 1)</li>
 *   <li>BR-PERIOD-ATT-002: period_no within typical 1..10 range; the column
 *       allows broader values for half-day / extra periods, service layer
 *       enforces the K-12 contract per tenant vertical_type</li>
 *   <li>BR-PERIOD-ATT-003: unique (student_id, subject_section_id, date,
 *       period_no, instance_id, deleted) — no duplicate attendance for the
 *       same student in the same period of the same day</li>
 *   <li>BR-PERIOD-ATT-004: status reuses {@link AttendanceStatus} (PRESENT,
 *       ABSENT, LATE, EXCUSED, MAKEUP) — same enum as legacy Attendance</li>
 * </ul>
 *
 * <p>Backward compatibility: existing {@link Attendance} per-day records on
 * CENTER tenants are preserved unchanged. CENTER tenants do not write to this
 * table. The {@code tenant.vertical_type} discriminator (V24
 * kitehub-subscription) gates which model a tenant uses.
 *
 * @see Attendance
 * @see com.kiteclass.core.module.k12.entity.SubjectSection
 * @since GAP-323 Phase 1A (Wave 18b1)
 */
@Entity
@Table(
        name = "attendance_period",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_att_period_student_section_date_period",
                        columnNames = {
                                "student_id",
                                "subject_section_id",
                                "date",
                                "period_no",
                                "instance_id",
                                "deleted"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_att_period_student_date", columnList = "student_id,date"),
                @Index(name = "idx_att_period_class_date", columnList = "class_id,date"),
                @Index(name = "idx_att_period_subject_section", columnList = "subject_section_id"),
                @Index(name = "idx_att_period_instance_id", columnList = "instance_id"),
                @Index(name = "idx_att_period_deleted", columnList = "deleted"),
                @Index(name = "idx_att_period_recorded_by", columnList = "recorded_by")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendancePeriod extends BaseEntity {

    /**
     * FK to {@code students.id}. Required.
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * FK to {@code classes.id} (HomeroomClass-side reference for K-12).
     * Required for daily roll-up queries (vắng cả ngày = vắng ≥7 tiết).
     */
    @Column(name = "class_id", nullable = false)
    private Long classId;

    /**
     * FK to {@code subject_sections.id}
     * ({@link com.kiteclass.core.module.k12.entity.SubjectSection}).
     * Identifies which môn (subject) + which lớp bộ môn the period belongs to.
     */
    @Column(name = "subject_section_id", nullable = false)
    private Long subjectSectionId;

    /**
     * Tiết number within the day (1..10 typical). Phase 1A allows the full
     * Integer range at the schema level; service layer + future K-12 CHECK
     * constraint (Phase 1B) enforce the realistic 1..10 contract.
     */
    @Column(name = "period_no", nullable = false)
    private Integer periodNo;

    /**
     * Calendar date the period was held (separates from recorded_at to allow
     * back-dated entry by GVCN within an audit window).
     */
    @Column(name = "date", nullable = false)
    private LocalDate date;

    /**
     * Attendance status — reuses the existing {@link AttendanceStatus} enum
     * to preserve UI conventions and gamification points logic.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    /**
     * Teacher / GVCN user ID who recorded the attendance. Required for audit.
     */
    @Column(name = "recorded_by", nullable = false)
    private Long recordedBy;

    /**
     * Server-side timestamp of recording (separate from {@link #date} which is
     * the lesson day). Used for SLA reporting (≤2 min target) and audit trail.
     */
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    /**
     * Optional free-form note (max 500 chars) for explanations such as
     * "đi học bù tiết khác", "phụ huynh xin phép", etc.
     */
    @Column(name = "notes", length = 500)
    private String notes;
}
