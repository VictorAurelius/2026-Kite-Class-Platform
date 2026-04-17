package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Type of invoice adjustments.
 *
 * <p>Defines types of adjustments applied to invoices:
 * <ul>
 *   <li>DISCOUNT: Reduces invoice total (negative amount)</li>
 *   <li>LATE_FEE: Penalty for late payment (positive amount)</li>
 *   <li>ADDITIONAL_CHARGE: Additional fees (positive amount)</li>
 *   <li>REFUND: Refund amount (negative amount)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Getter
public enum InvoiceAdjustmentType {

    DISCOUNT("Giảm giá", true),
    LATE_FEE("Phí trễ hạn", false),
    ADDITIONAL_CHARGE("Phí bổ sung", false),
    REFUND("Hoàn tiền", true);

    private final String displayNameVi;
    private final boolean isNegative;  // true if reduces invoice total

    InvoiceAdjustmentType(String displayNameVi, boolean isNegative) {
        this.displayNameVi = displayNameVi;
        this.isNegative = isNegative;
    }
}
