package com.kiteclass.core.module.payment.record.dto;

import com.kiteclass.core.module.payment.record.entity.PaymentRecord;
import com.kiteclass.core.module.payment.record.entity.PaymentRecordMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO returned after recording a payment.
 *
 * @see com.kiteclass.core.module.payment.record.entity.PaymentRecord
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordResponse {

    private Long id;
    private Long invoiceId;
    private PaymentRecordMethod method;
    private BigDecimal amount;
    private Instant paidAt;
    private String note;
    private Long recordedBy;
    private Instant createdAt;

    /**
     * Maps PaymentRecord entity → response DTO.
     */
    public static PaymentRecordResponse fromEntity(PaymentRecord entity) {
        return PaymentRecordResponse.builder()
                .id(entity.getId())
                .invoiceId(entity.getInvoiceId())
                .method(entity.getMethod())
                .amount(entity.getAmount())
                .paidAt(entity.getPaidAt())
                .note(entity.getNote())
                .recordedBy(entity.getRecordedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
