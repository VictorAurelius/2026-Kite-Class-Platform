package com.kiteclass.core.module.payment.record.service.impl;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.common.idempotency.IdempotencyScope;
import com.kiteclass.core.common.idempotency.IdempotencyService;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.payment.record.dto.PaymentRecordResponse;
import com.kiteclass.core.module.payment.record.dto.RecordPaymentRequest;
import com.kiteclass.core.module.payment.record.entity.PaymentRecord;
import com.kiteclass.core.module.payment.record.repository.PaymentRecordRepository;
import com.kiteclass.core.module.payment.record.service.PaymentRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final IdempotencyService idempotencyService;

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
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) invoiceId));

        if (!currentTenant.equals(invoice.getInstanceId())) {
            log.warn("Cross-tenant payment record attempt: invoiceId={} belongs to tenant={}, request from tenant={}",
                    invoiceId, invoice.getInstanceId(), currentTenant);
            throw new PermissionDeniedException("PAYMENT_RECORD_CROSS_TENANT_DENIED");
        }

        // GAP-1004: over-payment guard (BR-INV-004 balanceDue = total - amountPaid).
        // Recording more than the outstanding balance drove balance_due negative
        // + flipped the invoice to PAID for an over-charge. Reject with 400.
        BigDecimal balanceDue = invoice.getBalanceDue();
        if (request.getAmount().compareTo(balanceDue) > 0) {
            log.warn("Over-payment rejected: invoiceId={} amount={} exceeds balanceDue={}",
                    invoiceId, request.getAmount(), balanceDue);
            throw new BusinessException("PAYMENT_EXCEEDS_BALANCE", HttpStatus.BAD_REQUEST);
        }

        // GAP-1004: DB-side idempotency (BR-PAYMENT-METHOD-004). A replayed
        // Idempotency-Key previously created a 2nd payment_records row + double-counted
        // amount_paid. Persist the (tenant, key, PAYMENT_RECORD) marker; a replay short-
        // circuits with 409 instead of writing a duplicate.
        boolean idempotent = idempotencyKey != null && !idempotencyKey.isBlank();
        String requestHash = null;
        if (idempotent) {
            requestHash = IdempotencyService.hashRequest(
                    invoiceId + "|" + request.getAmount() + "|" + request.getMethod());
            if (idempotencyService.findExisting(currentTenant, idempotencyKey, IdempotencyScope.PAYMENT_RECORD)
                    .isPresent()) {
                log.info("Duplicate payment record suppressed (idempotency replay): invoiceId={} key={}",
                        invoiceId, idempotencyKey);
                throw new BusinessException("PAYMENT_RECORD_DUPLICATE", HttpStatus.CONFLICT);
            }
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

        // GAP-1004: persist the idempotency marker so a replay of the same key
        // short-circuits above. recordRequest returns false on a concurrent-race
        // loss → treat as duplicate (409).
        if (idempotent && !idempotencyService.recordRequest(
                currentTenant, idempotencyKey, IdempotencyScope.PAYMENT_RECORD,
                null, requestHash, HttpStatus.CREATED.value(), String.valueOf(saved.getId()))) {
            throw new BusinessException("PAYMENT_RECORD_DUPLICATE", HttpStatus.CONFLICT);
        }

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
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) invoiceId));
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
