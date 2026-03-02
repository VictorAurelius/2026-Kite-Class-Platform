package com.kiteclass.core.module.attendance.entity;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Attendance entity representing student attendance tracking for class sessions.
 *
 * <p>Manages attendance records with:
 * <ul>
 *   <li>Attendance status (PRESENT, ABSENT, LATE, EXCUSED, MAKEUP)</li>
 *   <li>Timestamp and marking teacher tracking</li>
 *   <li>Gamification points integration</li>
 *   <li>Notes and additional context</li>
 * </ul>
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-ATTEND-001: Enrollment must exist and be ACTIVE</li>
 *   <li>BR-ATTEND-002: Session must exist and not be COMPLETED/CANCELLED</li>
 *   <li>BR-ATTEND-003: Cannot mark duplicate attendance for same enrollment+session</li>
 *   <li>BR-ATTEND-004: Only MAIN_TEACHER can mark attendance</li>
 *   <li>BR-ATTEND-005: Late detection if marked > session.startTime + 15 minutes</li>
 *   <li>BR-ATTEND-006: Points calculation based on AttendanceStatus</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Entity
@Table(
        name = "attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_enrollment_session",
                        columnNames = {"enrollment_id", "session_id", "instance_id", "deleted"}
                )
        },
        indexes = {
                @Index(name = "idx_attendance_enrollment_id", columnList = "enrollment_id"),
                @Index(name = "idx_attendance_session_id", columnList = "session_id"),
                @Index(name = "idx_attendance_status", columnList = "status"),
                @Index(name = "idx_attendance_instance_id", columnList = "instance_id"),
                @Index(name = "idx_attendance_deleted", columnList = "deleted"),
                @Index(name = "idx_attendance_marked_date", columnList = "marked_date"),
                @Index(name = "idx_attendance_marked_by", columnList = "marked_by")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance extends BaseEntity {

    /**
     * Foreign key to enrollment.
     * Required, must reference an existing active enrollment.
     */
    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    /**
     * Foreign key to class session.
     * Required, must reference an existing session.
     */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /**
     * Attendance status.
     * Required, determines points awarded/deducted.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    /**
     * Timestamp when attendance was marked.
     * Defaults to current timestamp, used for late detection.
     */
    @Column(name = "marked_date", nullable = false)
    @Builder.Default
    private LocalDateTime markedDate = LocalDateTime.now();

    /**
     * Teacher ID who marked the attendance.
     * Optional, tracks who recorded the attendance.
     */
    @Column(name = "marked_by")
    private Long markedBy;

    /**
     * Additional notes about the attendance record.
     * Optional, max 500 characters for context or explanations.
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Points awarded/deducted for this attendance record.
     * Auto-calculated based on AttendanceStatus:
     * - PRESENT: 0
     * - ABSENT: -10
     * - LATE: -5
     * - EXCUSED: 0
     * - MAKEUP: 0
     */
    @Column(name = "points_awarded")
    @Builder.Default
    private Integer pointsAwarded = 0;

    /**
     * Lifecycle callback to calculate points before persisting.
     * Ensures pointsAwarded is consistent with attendance status.
     */
    @PrePersist
    @PreUpdate
    public void calculatePoints() {
        if (status != null) {
            this.pointsAwarded = status.getPointsDeduction();
        }
    }

    /**
     * Check if student was present.
     *
     * @return true if status is PRESENT, false otherwise
     */
    public boolean isPresent() {
        return this.status == AttendanceStatus.PRESENT;
    }

    /**
     * Check if student was absent.
     *
     * @return true if status is ABSENT, false otherwise
     */
    public boolean isAbsent() {
        return this.status == AttendanceStatus.ABSENT;
    }

    /**
     * Check if student was late.
     *
     * @return true if status is LATE, false otherwise
     */
    public boolean isLate() {
        return this.status == AttendanceStatus.LATE;
    }

    /**
     * Check if absence was excused.
     *
     * @return true if status is EXCUSED, false otherwise
     */
    public boolean isExcused() {
        return this.status == AttendanceStatus.EXCUSED;
    }

    /**
     * Check if this is a makeup session attendance.
     *
     * @return true if status is MAKEUP, false otherwise
     */
    public boolean isMakeup() {
        return this.status == AttendanceStatus.MAKEUP;
    }
}
