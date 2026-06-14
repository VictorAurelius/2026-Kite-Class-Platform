package com.kiteclass.core.module.attendance.dto;

import com.kiteclass.core.common.constant.AttendanceStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Per-period attendance create request (single record).
 *
 * <p>{@code recordedBy} is derived by the controller from the authenticated
 * principal ({@code X-User-Reference-Id} → {@code UserContext}), not the body and
 * not a client-supplied header — the spoofable {@code X-Teacher-Id} header was
 * dropped in GAP-1300 (same convention as {@code AttendanceController}).
 *
 * @since GAP-323b Phase 1B (Wave 18b2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendancePeriodCreateRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Long classId;

    @NotNull
    private Long subjectSectionId;

    /**
     * Tiết number 1..10 per TT 22/2021/TT-BGDĐT (V51 enforces at DB level).
     */
    @NotNull
    @Min(1)
    @Max(10)
    private Integer periodNo;

    @NotNull
    private LocalDate date;

    @NotNull
    private AttendanceStatus status;

    @Size(max = 500)
    private String notes;
}
