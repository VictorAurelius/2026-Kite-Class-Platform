package com.kiteclass.core.module.payment.record.service;

import com.kiteclass.core.module.payment.record.dto.PaymentRecordResponse;
import com.kiteclass.core.module.payment.record.dto.RecordPaymentRequest;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Service interface for recording manual payments received at trung tâm.
 *
 * <p>Distinct from gateway payment service (VNPAY/MoMo redirect flow handled by
 * {@code com.kiteclass.core.module.payment} package). This service handles offline
 * payments — cash, bank transfer, VietQR scan, MoMo wallet — recorded by
 * teacher/admin after physically receiving tuition fee.
 *
 * <p>Multi-tenant + cross-tenant defense per OWASP A01:
 * <ul>
 *   <li>Invoice lookup verifies instanceId matches current tenant</li>
 *   <li>Idempotency key prevents double-recording (BR-PAYMENT-METHOD-004)</li>
 *   <li>RecordedBy auto-populated from authenticated principal</li>
 * </ul>
 *
 * @see com.kiteclass.core.module.payment.record.entity.PaymentRecord
 * @see com.kiteclass.core.module.parent.payment.PaymentIdempotencyService
 */
public interface PaymentRecordService {

    /**
     * Records a manual payment against an invoice.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>BR-PAYMENT-METHOD-001: method ∈ {CASH, BANK_TRANSFER, VIETQR, MOMO}</li>
     *   <li>BR-PAYMENT-METHOD-002: amount > 0</li>
     *   <li>BR-PAYMENT-METHOD-003: invoice MUST belong to current tenant (OWASP A01)</li>
     *   <li>BR-PAYMENT-METHOD-004: same idempotencyKey returns existing record (no duplicate)</li>
     * </ul>
     *
     * @param invoiceId the invoice being paid
     * @param request payment details (method, amount, paidAt, note)
     * @param recordedByUserId authenticated user recording the payment
     * @param idempotencyKey optional Idempotency-Key header value (BR-PAYMENT-METHOD-004)
     * @return response with persisted PaymentRecord
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if invoice not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if invoice belongs to other tenant
     */
    PaymentRecordResponse recordPayment(
            Long invoiceId,
            @Valid RecordPaymentRequest request,
            Long recordedByUserId,
            String idempotencyKey
    );

    /**
     * Lists all payment records for an invoice within current tenant.
     *
     * @param invoiceId the invoice ID
     * @return list of payment records (may be empty)
     */
    List<PaymentRecordResponse> getPaymentRecordsByInvoice(Long invoiceId);
}
