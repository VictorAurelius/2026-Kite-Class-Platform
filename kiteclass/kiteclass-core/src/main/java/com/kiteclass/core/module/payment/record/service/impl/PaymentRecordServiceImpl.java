package com.kiteclass.core.module.payment.record.service.impl;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.payment.record.dto.PaymentRecordResponse;
import com.kiteclass.core.module.payment.record.dto.RecordPaymentRequest;
import com.kiteclass.core.module.payment.record.entity.PaymentRecord;
import com.kiteclass.core.module.payment.record.repository.PaymentRecordRepository;
import com.kiteclass.core.module.payment.record.service.PaymentRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link PaymentRecordService}.
 *
 * <p>Implements OWASP A01 cross-tenant defense via instanceId verification
 * BEFORE PaymentRecord persistence. Idempotency-Key support delegated to
 * caller's controller layer (Bucket scope minimization — full
 * idempotency_keys table integration via {@code PaymentIdempotencyService}
 * pattern deferred to follow-up if duplicate-record incident surfaces).
 *
 * @see PaymentRecordService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRecordServiceImpl implements PaymentRecordService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    @Override
    @Transactional
    public PaymentRecordResponse recordPayment(
            Long invoiceId,
            RecordPaymentRequest request,
            Long recordedByUserId,
            String idempotencyKey
    ) {
        UUID currentTenant = TenantContext.getCurrentTenant();

        // Fetch invoice + cross-tenant defense (BR-PAYMENT-METHOD-003 / OWASP A01)
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", invoiceId));

        if (!currentTenant.equals(invoice.getInstanceId())) {
            log.warn("Cross-tenant payment record attempt: invoiceId={} belongs to tenant={}, request from tenant={}",
                    invoiceId, invoice.getInstanceId(), currentTenant);
            throw new PermissionDeniedException("PAYMENT_RECORD_CROSS_TENANT_DENIED");
        }

        // Idempotency note (BR-PAYMENT-METHOD-004):
        // If idempotencyKey provided, check for existing record with same (invoice, key) tuple.
        // Phase 1 BETA simplification: rely on FE deduplication + controller-level idempotency-key header logging.
        // Full DB-backed dedup deferred until duplicate-record incident class surfaces in production.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            log.debug("Idempotency-Key received for payment record: invoiceId={} key={}", invoiceId, idempotencyKey);
        }

        Instant paidAt = request.getPaidAt() != null ? request.getPaidAt() : Instant.now();

        PaymentRecord entity = PaymentRecord.builder()
                .invoiceId(invoiceId)
                .method(request.getMethod())
                .amount(request.getAmount())
                .paidAt(paidAt)
                .note(request.getNote())
                .recordedBy(recordedByUserId)
                .build();
        entity.setInstanceId(currentTenant);

        PaymentRecord saved = paymentRecordRepository.save(entity);

        // Update invoice.amount_paid (running total) - business rule: payment_records sum should match
        BigDecimal currentPaid = invoice.getAmountPaid() != null ? invoice.getAmountPaid() : BigDecimal.ZERO;
        invoice.setAmountPaid(currentPaid.add(request.getAmount()));
        invoiceRepository.save(invoice);

        log.info("Payment recorded: invoiceId={} method={} amount={} recordedBy={} tenant={}",
                invoiceId, request.getMethod(), request.getAmount(), recordedByUserId, currentTenant);

        return PaymentRecordResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentRecordResponse> getPaymentRecordsByInvoice(Long invoiceId) {
        UUID currentTenant = TenantContext.getCurrentTenant();

        // Defense: verify invoice belongs to tenant before listing payments (OWASP A01)
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", invoiceId));
        if (!currentTenant.equals(invoice.getInstanceId())) {
            throw new PermissionDeniedException("PAYMENT_RECORD_CROSS_TENANT_DENIED");
        }

        return paymentRecordRepository
                .findByInvoiceIdAndInstanceId(invoiceId, currentTenant)
                .stream()
                .map(PaymentRecordResponse::fromEntity)
                .toList();
    }
}
