package com.kiteclass.core.module.payment.dto;

import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment operations.
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String paymentNumber;
    private String transactionId;
    private Long invoiceId;
    private Long installmentId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String paymentUrl;
    private String qrCodeUrl;
    private String receiptNumber;
    private String receiptUrl;
    private LocalDateTime initiatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;
    private String failureReason;
}
