package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status values for ClassSession entity.
 *
 * <p>Represents the status of individual class sessions:
 * <ul>
 *   <li>SCHEDULED: Session scheduled but not yet held</li>
 *   <li>COMPLETED: Session completed</li>
 *   <li>CANCELLED: Session cancelled</li>
 *   <li>MAKEUP: Makeup session for missed class</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Getter
public enum SessionStatus {

    SCHEDULED("Đã lên lịch"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy"),
    MAKEUP("Học bù");

    private final String displayName;

    SessionStatus(String displayName) {
        this.displayName = displayName;
    }
}
