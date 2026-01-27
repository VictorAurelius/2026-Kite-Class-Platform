package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status values for Invoice entity.
 *
 * <p>Represents invoice lifecycle with business rules:
 * <ul>
 *   <li>DRAFT: Can be edited, not sent to customer</li>
 *   <li>SENT: Sent to customer, awaiting payment</li>
 *   <li>PAID: Fully paid</li>
 *   <li>PARTIAL: Partially paid</li>
 *   <li>OVERDUE: Past due date without payment</li>
 *   <li>CANCELLED: Invoice cancelled</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Getter
public enum InvoiceStatus {

    DRAFT("Nháp", false),
    SENT("Đã gửi", false),
    PAID("Đã thanh toán", true),
    PARTIAL("Thanh toán một phần", false),
    OVERDUE("Quá hạn", false),
    CANCELLED("Đã hủy", true);

    private final String displayNameVi;
    private final boolean isFinal;  // Cannot change after this status

    InvoiceStatus(String displayNameVi, boolean isFinal) {
        this.displayNameVi = displayNameVi;
        this.isFinal = isFinal;
    }

    /**
     * Checks if invoice can be edited.
     *
     * @return true if status is DRAFT
     */
    public boolean canEdit() {
        return this == DRAFT;
    }

    /**
     * Checks if invoice can accept payment.
     *
     * @return true if status is SENT, PARTIAL, or OVERDUE
     */
    public boolean canPay() {
        return this == SENT || this == PARTIAL || this == OVERDUE;
    }

    /**
     * Checks if invoice can be cancelled.
     *
     * @return true if status is DRAFT or SENT
     */
    public boolean canCancel() {
        return this == DRAFT || this == SENT;
    }
}
