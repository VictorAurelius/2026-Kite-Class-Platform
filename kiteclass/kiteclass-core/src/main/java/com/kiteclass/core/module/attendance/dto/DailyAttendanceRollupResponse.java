package com.kiteclass.core.module.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Per-(student, date) attendance roll-up — counts every status across the
 * day's tiết list.
 *
 * <p>{@code allDayAbsent} is true when {@code absentCount + lateCount >= 7},
 * the threshold TT 22/2021/TT-BGDĐT uses for "vắng cả ngày" reporting; LATE
 * counts are intentionally lumped with ABSENT here because the regulation
 * treats both as missed instructional time for the daily metric.
 *
 * @since GAP-323b Phase 1B (Wave 18b2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAttendanceRollupResponse {

    private Long studentId;
    private Long classId;
    private LocalDate date;

    private long periodCount;
    private long presentCount;
    private long absentCount;
    private long lateCount;
    private long excusedCount;
    private long makeupCount;

    private boolean allDayAbsent;
}
