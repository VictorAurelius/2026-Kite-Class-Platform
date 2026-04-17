package com.kiteclass.core.module.gamification.service;

/**
 * Service interface for managing student points in the gamification system.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
public interface PointService {

    /**
     * Award or deduct points for attendance.
     *
     * @param studentId student ID
     * @param attendanceId attendance record ID
     * @param points points to award (positive) or deduct (negative)
     * @param description description of the point transaction
     */
    void awardAttendancePoints(Long studentId, Long attendanceId, Integer points, String description);

    /**
     * Update attendance points (when attendance status is updated).
     * Removes old point record and creates new one.
     *
     * @param studentId student ID
     * @param attendanceId attendance record ID
     * @param newPoints new points value
     * @param description description of the point transaction
     */
    void updateAttendancePoints(Long studentId, Long attendanceId, Integer newPoints, String description);

    /**
     * Get total points for a student.
     *
     * @param studentId student ID
     * @return total points
     */
    Integer getTotalPoints(Long studentId);
}
