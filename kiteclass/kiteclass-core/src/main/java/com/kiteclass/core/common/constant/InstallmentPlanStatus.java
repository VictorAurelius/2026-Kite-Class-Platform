package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status of installment payment plans.
 *
 * <p>Represents installment plan approval workflow:
 * <ul>
 *   <li>PENDING: Plan requested, awaiting approval</li>
 *   <li>APPROVED: Plan approved by admin</li>
 *   <li>REJECTED: Plan rejected</li>
 *   <li>ACTIVE: Plan active, installments being paid</li>
 *   <li>COMPLETED: All installments paid</li>
 *   <li>CANCELLED: Plan cancelled</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Getter
public enum InstallmentPlanStatus {

    PENDING("Chờ duyệt", false),
    APPROVED("Đã duyệt", false),
    REJECTED("Đã từ chối", true),
    ACTIVE("Đang hoạt động", false),
    COMPLETED("Đã hoàn thành", true),
    CANCELLED("Đã hủy", true);

    private final String displayNameVi;
    private final boolean isFinal;  // Cannot change after this status

    InstallmentPlanStatus(String displayNameVi, boolean isFinal) {
        this.displayNameVi = displayNameVi;
        this.isFinal = isFinal;
    }

    /**
     * Checks if plan can be approved.
     *
     * @return true if status is PENDING
     */
    public boolean canApprove() {
        return this == PENDING;
    }

    /**
     * Checks if plan can be rejected.
     *
     * @return true if status is PENDING
     */
    public boolean canReject() {
        return this == PENDING;
    }

    /**
     * Checks if plan can be cancelled.
     *
     * @return true if status is APPROVED or ACTIVE
     */
    public boolean canCancel() {
        return this == APPROVED || this == ACTIVE;
    }
}
