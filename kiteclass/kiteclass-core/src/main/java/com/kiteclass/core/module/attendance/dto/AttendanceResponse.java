package com.kiteclass.core.module.attendance.dto;

import com.kiteclass.core.common.constant.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Response DTO for attendance data.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    /**
     * Attendance record ID.
     */
    private Long id;

    /**
     * Enrollment ID.
     */
    private Long enrollmentId;

    /**
     * Student name (populated from enrollment).
     */
    private String studentName;

    /**
     * Session ID.
     */
    private Long sessionId;

    /**
     * Session number (populated from session).
     */
    private Integer sessionNumber;

    /**
     * Attendance status.
     */
    private AttendanceStatus status;

    /**
     * Timestamp when attendance was marked.
     */
    private LocalDateTime markedDate;

    /**
     * Teacher ID who marked the attendance.
     */
    private Long markedBy;

    /**
     * Teacher name (populated from teacher).
     */
    private String markedByName;

    /**
     * Additional notes.
     */
    private String notes;

    /**
     * Points awarded/deducted.
     */
    private Integer pointsAwarded;

    /**
     * Creation timestamp.
     */
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    private Instant updatedAt;
}
