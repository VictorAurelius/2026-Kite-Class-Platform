package com.kiteclass.core.module.attendance.dto;

import com.kiteclass.core.common.constant.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an attendance record's status.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAttendanceStatusRequest {

    /**
     * New attendance status.
     * Required.
     */
    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    /**
     * Updated notes.
     * Optional, max 500 characters.
     */
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
