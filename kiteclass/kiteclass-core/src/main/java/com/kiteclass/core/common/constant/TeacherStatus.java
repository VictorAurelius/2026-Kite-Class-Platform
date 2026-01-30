package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status values for Teacher entity.
 *
 * <p>Represents the current status of a teacher in the system:
 * <ul>
 *   <li>ACTIVE: Currently teaching, can be assigned to new classes</li>
 *   <li>INACTIVE: Temporarily stopped, cannot be assigned to new classes</li>
 *   <li>ON_LEAVE: On leave, current assignments retained but no new assignments</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Getter
public enum TeacherStatus {

    ACTIVE("Đang hoạt động", "Currently teaching, can be assigned to classes"),
    INACTIVE("Tạm ngưng", "Temporarily stopped, cannot be assigned to new classes"),
    ON_LEAVE("Nghỉ phép", "On leave, current assignments retained");

    private final String displayNameVi;
    private final String description;

    TeacherStatus(String displayNameVi, String description) {
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
            case INACTIVE -> "bg-gray-100 text-gray-800";
            case ON_LEAVE -> "bg-yellow-100 text-yellow-800";
        };
    }
}
