package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status values for Class entity.
 *
 * <p>Represents the lifecycle of a class:
 * <ul>
 *   <li>DRAFT: Class is being configured</li>
 *   <li>SCHEDULED: Class scheduled but not started</li>
 *   <li>IN_PROGRESS: Class currently active</li>
 *   <li>COMPLETED: Class ended normally</li>
 *   <li>CANCELLED: Class cancelled</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Getter
public enum ClassStatus {

    DRAFT("Nháp", "Class is being configured"),
    SCHEDULED("Đã lên lịch", "Class scheduled but not started"),
    IN_PROGRESS("Đang diễn ra", "Class currently active"),
    COMPLETED("Đã hoàn thành", "Class ended normally"),
    CANCELLED("Đã hủy", "Class cancelled");

    private final String displayNameVi;
    private final String description;

    ClassStatus(String displayNameVi, String description) {
        this.displayNameVi = displayNameVi;
        this.description = description;
    }
}
