package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Role values for TeacherClass assignment.
 *
 * <p>Defines the role of a teacher in a specific class:
 * <ul>
 *   <li>MAIN_TEACHER: Primary teacher with full control over the class (attendance, grading, etc.)</li>
 *   <li>ASSISTANT: Teaching assistant with limited permissions, support role</li>
 * </ul>
 *
 * <p>Permission hierarchy: MAIN_TEACHER > ASSISTANT
 *
 * <p>Note: Only MAIN_TEACHER can take attendance (BR-TEACHER-008)
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Getter
public enum TeacherClassRole {

    MAIN_TEACHER("Giáo viên chính", "Full control over the class"),
    ASSISTANT("Trợ giảng", "Limited permissions, support role");

    private final String displayNameVi;
    private final String description;

    TeacherClassRole(String displayNameVi, String description) {
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
            case MAIN_TEACHER -> "bg-blue-100 text-blue-800";
            case ASSISTANT -> "bg-gray-100 text-gray-800";
        };
    }
}
