package com.kiteclass.core.module.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for attendance statistics.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceStatsResponse {

    /**
     * Target ID (student ID or class ID).
     */
    private Long targetId;

    /**
     * Target type: "STUDENT" or "CLASS".
     */
    private String targetType;

    /**
     * Total number of sessions.
     */
    private Long totalSessions;

    /**
     * Number of PRESENT records.
     */
    private Long presentCount;

    /**
     * Number of ABSENT records.
     */
    private Long absentCount;

    /**
     * Number of LATE records.
     */
    private Long lateCount;

    /**
     * Number of EXCUSED records.
     */
    private Long excusedCount;

    /**
     * Number of MAKEUP records.
     */
    private Long makeupCount;

    /**
     * Attendance rate percentage (presentCount / totalSessions * 100).
     * Null if no sessions.
     */
    private Double attendanceRate;
}
