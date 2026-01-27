package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status values for Attendance entity.
 *
 * <p>Represents student attendance status with:
 * <ul>
 *   <li>Display names in Vietnamese</li>
 *   <li>Short codes for compact display</li>
 *   <li>Color classes for UI styling</li>
 *   <li>Points deduction for gamification</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Getter
public enum AttendanceStatus {

    PRESENT("Có mặt", "P", "bg-green-500"),
    ABSENT("Vắng", "V", "bg-red-500"),
    LATE("Đi trễ", "T", "bg-yellow-500"),
    EXCUSED("Có phép", "CP", "bg-blue-500"),
    MAKEUP("Học bù", "HB", "bg-purple-500");

    private final String displayNameVi;
    private final String shortCode;
    private final String colorClass;

    AttendanceStatus(String displayNameVi, String shortCode, String colorClass) {
        this.displayNameVi = displayNameVi;
        this.shortCode = shortCode;
        this.colorClass = colorClass;
    }

    /**
     * Returns points deduction for gamification system.
     *
     * <p>Points impact:
     * <ul>
     *   <li>PRESENT: 0 (no deduction)</li>
     *   <li>LATE: -5 points</li>
     *   <li>EXCUSED: 0 (no deduction)</li>
     *   <li>ABSENT: -10 points</li>
     *   <li>MAKEUP: 0 (no deduction)</li>
     * </ul>
     *
     * @return points to deduct (negative value)
     */
    public int getPointsDeduction() {
        return switch (this) {
            case PRESENT -> 0;
            case LATE -> -5;
            case EXCUSED -> 0;
            case ABSENT -> -10;
            case MAKEUP -> 0;
        };
    }
}
