package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Status values for Payment entity.
 *
 * <p>Represents payment processing status:
 * <ul>
 *   <li>PENDING: Payment initiated but not processed</li>
 *   <li>PROCESSING: Payment being processed by gateway</li>
 *   <li>COMPLETED: Payment successful</li>
 *   <li>FAILED: Payment failed</li>
 *   <li>REFUNDED: Payment refunded to customer</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Getter
public enum PaymentStatus {

    PENDING("Chờ xử lý"),
    PROCESSING("Đang xử lý"),
    COMPLETED("Thành công"),
    FAILED("Thất bại"),
    REFUNDED("Đã hoàn tiền");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }
}
