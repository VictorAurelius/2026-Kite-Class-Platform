package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.platform.domain.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for payment information.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
public class PaymentResponse {

    private UUID id;
    private UUID subscriptionId;
    private Long amountVnd;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String qrCodeUrl;
    private String transactionId;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String paymentContent;
    private String txnRef;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Payment entity to Response DTO.
     *
     * @param payment Payment entity
     * @return PaymentResponse DTO
     */
    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
            .id(payment.getId())
            .subscriptionId(payment.getSubscriptionId())
            .amountVnd(payment.getAmountVnd())
            .currency(payment.getCurrency())
            .paymentMethod(payment.getPaymentMethod())
            .status(payment.getStatus())
            .qrCodeUrl(payment.getQrCodeUrl())
            .transactionId(payment.getTransactionId())
            .bankCode(payment.getBankCode())
            .accountNumber(payment.getAccountNumber())
            .accountName(payment.getAccountName())
            .paymentContent(payment.getPaymentContent())
            .txnRef(payment.getTxnRef())
            .paidAt(payment.getPaidAt())
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .build();
    }
}
