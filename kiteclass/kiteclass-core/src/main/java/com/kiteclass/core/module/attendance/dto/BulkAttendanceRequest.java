package com.kiteclass.core.module.attendance.dto;

import com.kiteclass.core.common.constant.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for bulk attendance marking for a session.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAttendanceRequest {

    /**
     * ID of the class session to mark attendance for.
     * Required, all records will be for this session.
     */
    @NotNull(message = "Session ID is required")
    @Positive(message = "Session ID must be positive")
    private Long sessionId;

    /**
     * List of attendance records to create.
     * Required, at least one record must be provided.
     */
    @NotEmpty(message = "At least one attendance record is required")
    @Valid
    private List<AttendanceRecord> records;

    /**
     * Individual attendance record within a bulk request.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceRecord {

        /**
         * Enrollment ID for this record.
         * Required.
         */
        @NotNull(message = "Enrollment ID is required")
        @Positive(message = "Enrollment ID must be positive")
        private Long enrollmentId;

        /**
         * Attendance status for this record.
         * Required.
         */
        @NotNull(message = "Status is required")
        private AttendanceStatus status;

        /**
         * Optional notes for this specific record.
         * Max 500 characters.
         */
        @Size(max = 500, message = "Notes must not exceed 500 characters")
        private String notes;
    }
}
