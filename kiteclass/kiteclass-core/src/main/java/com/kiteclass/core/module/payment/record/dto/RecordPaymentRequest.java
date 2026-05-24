package com.kiteclass.core.module.payment.record.dto;

import com.kiteclass.core.module.payment.record.entity.PaymentRecordMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request DTO for recording a manual payment received from a parent/student.
 *
 * <p>Used by {@code POST /api/v1/invoices/{invoiceId}/record-payment} endpoint.
 *
 * <p>Validation rules (BR-PAYMENT-METHOD-001/002):
 * <ul>
 *   <li>method MUST be one of CASH | BANK_TRANSFER | VIETQR | MOMO</li>
 *   <li>amount MUST be > 0 (matches DB CHECK constraint in V69)</li>
 *   <li>paidAt MAY be omitted — service defaults to {@code Instant.now()}</li>
 *   <li>note MAX 500 chars (matches PaymentRecord.note column length)</li>
 * </ul>
 *
 * @see com.kiteclass.core.module.payment.record.entity.PaymentRecord
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordPaymentRequest {

    /**
     * Payment method used by the parent/student.
     * Example: CASH (most common at TT dạy thêm), BANK_TRANSFER (Vietcombank/Techcombank), VIETQR, MOMO.
     */
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentRecordMethod method;

    /**
     * Amount received in VND. MUST be > 0.
     * Example: 1500000.00 for 1.500.000đ payment.
     */
    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    /**
     * Timestamp when payment was physically received. Defaults to now() if omitted.
     * Use UTC; FE displays in user's TZ (Asia/Ho_Chi_Minh).
     */
    private Instant paidAt;

    /**
     * Optional teacher note. Max 500 chars.
     * Example: "Phụ huynh em Hồng thanh toán 2 tháng học phí + phí đồng phục".
     */
    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;
}
