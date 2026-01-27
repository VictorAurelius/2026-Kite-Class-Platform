package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status values for Enrollment entity.
 *
 * <p>Represents student enrollment lifecycle:
 * <ul>
 *   <li>PENDING: Enrollment requested, awaiting approval</li>
 *   <li>ACTIVE: Enrollment active, student attending class</li>
 *   <li>COMPLETED: Enrollment completed (class finished)</li>
 *   <li>DROPPED: Student dropped from class</li>
 *   <li>CANCELLED: Enrollment cancelled before start</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Getter
public enum EnrollmentStatus {

    PENDING("Chờ xác nhận", "Enrollment requested, awaiting approval"),
    ACTIVE("Đang học", "Enrollment active, student attending class"),
    COMPLETED("Đã hoàn thành", "Enrollment completed (class finished)"),
    DROPPED("Đã nghỉ", "Student dropped from class"),
    CANCELLED("Đã hủy", "Enrollment cancelled before start");

    private final String displayNameVi;
    private final String description;

    EnrollmentStatus(String displayNameVi, String description) {
        this.displayNameVi = displayNameVi;
        this.description = description;
    }

    /**
     * Checks if enrollment is active.
     *
     * @return true if status is ACTIVE
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Checks if enrollment is finalized (cannot be modified).
     *
     * @return true if status is COMPLETED, DROPPED, or CANCELLED
     */
    public boolean isFinal() {
        return this == COMPLETED || this == DROPPED || this == CANCELLED;
    }
}
