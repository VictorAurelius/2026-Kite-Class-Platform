package com.kiteclass.core.module.attendance.dto;

import com.kiteclass.core.common.constant.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for per-period (per-tiết) attendance records.
 *
 * @since GAP-323 Phase 1A (Wave 18b1)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendancePeriodResponse {

    private Long id;
    private Long studentId;
    private Long classId;
    private Long subjectSectionId;
    private Integer periodNo;
    private LocalDate date;
    private AttendanceStatus status;
    private Long recordedBy;
    private LocalDateTime recordedAt;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
