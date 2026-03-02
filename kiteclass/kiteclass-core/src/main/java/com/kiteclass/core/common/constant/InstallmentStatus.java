package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status of individual installment payments.
 *
 * <p>Represents installment payment status:
 * <ul>
 *   <li>PENDING: Installment not yet due or unpaid</li>
 *   <li>PAID: Installment fully paid</li>
 *   <li>OVERDUE: Installment past due date and unpaid</li>
 *   <li>CANCELLED: Installment cancelled</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Getter
public enum InstallmentStatus {

    PENDING("Chờ thanh toán", false),
    PAID("Đã thanh toán", true),
    OVERDUE("Quá hạn", false),
    CANCELLED("Đã hủy", true);

    private final String displayNameVi;
    private final boolean isFinal;  // Cannot change after this status

    InstallmentStatus(String displayNameVi, boolean isFinal) {
        this.displayNameVi = displayNameVi;
        this.isFinal = isFinal;
    }

    /**
     * Checks if installment can accept payment.
     *
     * @return true if status is PENDING or OVERDUE
     */
    public boolean canPay() {
        return this == PENDING || this == OVERDUE;
    }
}
