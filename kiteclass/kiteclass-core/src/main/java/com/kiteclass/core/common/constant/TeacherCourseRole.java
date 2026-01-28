package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Role values for TeacherCourse assignment.
 *
 * <p>Defines the role of a teacher in a course:
 * <ul>
 *   <li>CREATOR: Teacher who created the course, has full control (edit, delete, manage classes)</li>
 *   <li>INSTRUCTOR: Teacher assigned to teach the course, can manage classes and students</li>
 *   <li>ASSISTANT: Teaching assistant with view-only permissions, support role</li>
 * </ul>
 *
 * <p>Permission hierarchy: CREATOR > INSTRUCTOR > ASSISTANT
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Getter
public enum TeacherCourseRole {

    CREATOR("Người tạo", "Full control over course and all classes"),
    INSTRUCTOR("Giảng viên", "Can teach course and manage assigned classes"),
    ASSISTANT("Trợ giảng", "View-only access, support role");

    private final String displayNameVi;
    private final String description;

    TeacherCourseRole(String displayNameVi, String description) {
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
            case CREATOR -> "bg-purple-100 text-purple-800";
            case INSTRUCTOR -> "bg-blue-100 text-blue-800";
            case ASSISTANT -> "bg-gray-100 text-gray-800";
        };
    }
}
