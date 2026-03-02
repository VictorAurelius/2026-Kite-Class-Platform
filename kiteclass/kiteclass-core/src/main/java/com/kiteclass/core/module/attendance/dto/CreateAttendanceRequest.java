package com.kiteclass.core.module.attendance.dto;

import com.kiteclass.core.common.constant.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new attendance record.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAttendanceRequest {

    /**
     * ID of the enrollment to mark attendance for.
     * Required, must reference an existing active enrollment.
     */
    @NotNull(message = "Enrollment ID is required")
    @Positive(message = "Enrollment ID must be positive")
    private Long enrollmentId;

    /**
     * ID of the class session.
     * Required, must reference an existing session.
     */
    @NotNull(message = "Session ID is required")
    @Positive(message = "Session ID must be positive")
    private Long sessionId;

    /**
     * Attendance status.
     * Required, determines points awarded/deducted.
     */
    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    /**
     * Additional notes about the attendance record.
     * Optional, max 500 characters.
     */
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
