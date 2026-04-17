package com.kiteclass.core.module.payment.dto;

import com.kiteclass.core.module.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for payment status query.
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {
    private PaymentStatus status;
}
