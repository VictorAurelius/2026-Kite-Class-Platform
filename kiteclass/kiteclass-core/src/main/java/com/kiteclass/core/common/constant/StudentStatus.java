package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status values for Student entity.
 *
 * <p>Represents the lifecycle of a student in the system:
 * <ul>
 *   <li>PENDING: Registration pending confirmation</li>
 *   <li>ACTIVE: Currently enrolled in classes</li>
 *   <li>INACTIVE: Temporarily not attending</li>
 *   <li>GRADUATED: Completed all courses</li>
 *   <li>DROPPED: Left the center</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Getter
public enum StudentStatus {

    PENDING("Chờ xác nhận", "Registration pending"),
    ACTIVE("Đang học", "Currently enrolled in classes"),
    INACTIVE("Tạm nghỉ", "Temporarily not attending"),
    GRADUATED("Đã tốt nghiệp", "Completed all courses"),
    DROPPED("Đã nghỉ học", "Left the center");

    private final String displayNameVi;
    private final String description;

    StudentStatus(String displayNameVi, String description) {
        this.displayNameVi = displayNameVi;
        this.description = description;
    }

    /**
     * Returns color class for UI styling.
     *
     * @return Tailwind CSS color classes
     */
    public String getColorClass() {
        return switch (this) {
            case ACTIVE -> "bg-green-100 text-green-800";
            case INACTIVE -> "bg-yellow-100 text-yellow-800";
            case GRADUATED -> "bg-blue-100 text-blue-800";
            case DROPPED -> "bg-gray-100 text-gray-800";
            case PENDING -> "bg-orange-100 text-orange-800";
        };
    }
}
