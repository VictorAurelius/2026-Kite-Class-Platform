package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status values for Course entity.
 *
 * <p>Represents the lifecycle status of a course:
 * <ul>
 *   <li>DRAFT: Course is being created/edited, not visible to students</li>
 *   <li>PUBLISHED: Course is public and students can enroll</li>
 *   <li>ARCHIVED: Course is no longer active, no new enrollments accepted</li>
 * </ul>
 *
 * <p>Status Transitions:
 * <ul>
 *   <li>DRAFT → PUBLISHED: When course is ready and published</li>
 *   <li>PUBLISHED → ARCHIVED: When course is no longer offered</li>
 *   <li>No transitions allowed to DRAFT after publishing</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Getter
public enum CourseStatus {

    /**
     * Course is in draft state.
     * Not visible to students. Can be freely edited or deleted.
     */
    DRAFT("Bản nháp", "Course is being created, not visible to students"),

    /**
     * Course is published and active.
     * Visible to students. Limited edits allowed. Cannot be deleted (must archive first).
     */
    PUBLISHED("Đã xuất bản", "Course is public and students can enroll"),

    /**
     * Course is archived.
     * No new enrollments accepted. Read-only. Existing students can continue.
     */
    ARCHIVED("Đã lưu trữ", "Course is archived, no new enrollments");

    private final String displayNameVi;
    private final String description;

    CourseStatus(String displayNameVi, String description) {
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
            case DRAFT -> "bg-gray-100 text-gray-800";
            case PUBLISHED -> "bg-green-100 text-green-800";
            case ARCHIVED -> "bg-orange-100 text-orange-800";
        };
    }

    /**
     * Checks if course status allows editing all fields.
     *
     * @return true if all fields can be edited
     */
    public boolean allowsFullEdit() {
        return this == DRAFT;
    }

    /**
     * Checks if course status allows limited editing.
     *
     * @return true if limited fields can be edited
     */
    public boolean allowsLimitedEdit() {
        return this == PUBLISHED;
    }

    /**
     * Checks if course status is read-only.
     *
     * @return true if course cannot be edited
     */
    public boolean isReadOnly() {
        return this == ARCHIVED;
    }

    /**
     * Checks if course can be deleted.
     *
     * @return true if course can be deleted
     */
    public boolean allowsDelete() {
        return this == DRAFT;
    }
}
