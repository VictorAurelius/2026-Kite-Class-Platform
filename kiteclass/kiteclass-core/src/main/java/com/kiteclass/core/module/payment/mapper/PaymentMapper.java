package com.kiteclass.core.module.payment.mapper;

import com.kiteclass.core.module.payment.dto.PaymentResponse;
import com.kiteclass.core.module.payment.entity.Payment;
import org.springframework.stereotype.Component;

/**
 * Mapper for Payment entity to DTO conversions.
 *
 * @since 1.0.0
 */
@Component
public class PaymentMapper {

    /**
     * Converts Payment entity to PaymentResponse DTO.
     *
     * @param payment payment entity
     * @return payment response DTO
     */
    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentResponse.builder()
            .id(payment.getId())
            .paymentNumber(payment.getPaymentNumber())
            .transactionId(payment.getTransactionId())
            .invoiceId(payment.getInvoiceId())
            .installmentId(payment.getInstallmentId())
            .amount(payment.getAmount())
            .paymentMethod(payment.getPaymentMethod())
            .paymentStatus(payment.getPaymentStatus())
            .paymentUrl(payment.getPaymentUrl())
            .qrCodeUrl(payment.getQrCodeUrl())
            .receiptNumber(payment.getReceiptNumber())
            .receiptUrl(payment.getReceiptUrl())
            .initiatedAt(payment.getInitiatedAt())
            .expiresAt(payment.getExpiresAt())
            .completedAt(payment.getCompletedAt())
            .failureReason(payment.getFailureReason())
            .build();
    }
}
