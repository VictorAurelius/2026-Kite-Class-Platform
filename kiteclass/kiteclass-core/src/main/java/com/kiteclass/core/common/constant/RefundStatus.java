package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status of refund requests.
 *
 * <p>Represents refund approval workflow:
 * <ul>
 *   <li>PENDING: Refund requested, awaiting approval</li>
 *   <li>APPROVED: Refund approved by admin</li>
 *   <li>REJECTED: Refund rejected</li>
 *   <li>COMPLETED: Refund processed and completed</li>
 *   <li>CANCELLED: Refund request cancelled</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Getter
public enum RefundStatus {

    PENDING("Chờ duyệt", false),
    APPROVED("Đã duyệt", false),
    REJECTED("Đã từ chối", true),
    COMPLETED("Đã hoàn tiền", true),
    CANCELLED("Đã hủy", true);

    private final String displayNameVi;
    private final boolean isFinal;  // Cannot change after this status

    RefundStatus(String displayNameVi, boolean isFinal) {
        this.displayNameVi = displayNameVi;
        this.isFinal = isFinal;
    }

    /**
     * Checks if refund can be approved.
     *
     * @return true if status is PENDING
     */
    public boolean canApprove() {
        return this == PENDING;
    }

    /**
     * Checks if refund can be rejected.
     *
     * @return true if status is PENDING
     */
    public boolean canReject() {
        return this == PENDING;
    }

    /**
     * Checks if refund can be processed.
     *
     * @return true if status is APPROVED
     */
    public boolean canProcess() {
        return this == APPROVED;
    }
}
