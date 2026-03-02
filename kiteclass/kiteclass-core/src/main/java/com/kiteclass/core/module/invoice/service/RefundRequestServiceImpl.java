package com.kiteclass.core.module.invoice.service;

import com.kiteclass.core.common.constant.InvoiceAdjustmentType;
import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.invoice.dto.CreateRefundRequestRequest;
import com.kiteclass.core.module.invoice.dto.RefundRequestResponse;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.entity.InvoiceAdjustment;
import com.kiteclass.core.module.invoice.entity.RefundRequest;
import com.kiteclass.core.module.invoice.mapper.InvoiceMapper;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.invoice.repository.RefundRequestRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Service implementation for refund request management.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class RefundRequestServiceImpl implements RefundRequestService {

    private final RefundRequestRepository refundRequestRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    @Override
    @Transactional
    public RefundRequestResponse createRefundRequest(@Valid CreateRefundRequestRequest request) {
        log.info("Creating refund request for invoice {}: amount={}",
                request.getInvoiceId(), request.getRefundAmount());

        // Validate invoice exists
        Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(request.getInvoiceId())
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) request.getInvoiceId()));

        // Validate refund amount does not exceed amount paid
        if (request.getRefundAmount().compareTo(invoice.getAmountPaid()) > 0) {
            throw new ValidationException("REFUND_AMOUNT_EXCEEDS_PAID", request.getRefundAmount());
        }

        // Create refund request
        RefundRequest refundRequest = RefundRequest.builder()
                .invoiceId(request.getInvoiceId())
                .refundAmount(request.getRefundAmount())
                .reason(request.getReason())
                .build();
        refundRequest.setInstanceId(invoice.getInstanceId());

        // Save
        RefundRequest saved = refundRequestRepository.save(refundRequest);

        log.info("Created refund request {} for invoice {}", saved.getId(), request.getInvoiceId());

        return invoiceMapper.toRefundResponse(saved);
    }

    @Override
    @Transactional
    public RefundRequestResponse approveRefund(Long id, Long approvedBy) {
        log.info("Approving refund request {}", id);

        RefundRequest refundRequest = refundRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("REFUND_REQUEST_NOT_FOUND", (Object) id));

        refundRequest.approve(approvedBy);

        RefundRequest saved = refundRequestRepository.save(refundRequest);

        log.info("Approved refund request {}", id);

        return invoiceMapper.toRefundResponse(saved);
    }

    @Override
    @Transactional
    public RefundRequestResponse rejectRefund(Long id, Long rejectedBy, String reason) {
        log.info("Rejecting refund request {}: {}", id, reason);

        RefundRequest refundRequest = refundRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("REFUND_REQUEST_NOT_FOUND", (Object) id));

        refundRequest.reject(rejectedBy, reason);

        RefundRequest saved = refundRequestRepository.save(refundRequest);

        log.info("Rejected refund request {}", id);

        return invoiceMapper.toRefundResponse(saved);
    }

    @Override
    @Transactional
    public RefundRequestResponse processRefund(Long id) {
        log.info("Processing refund request {}", id);

        RefundRequest refundRequest = refundRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("REFUND_REQUEST_NOT_FOUND", (Object) id));

        if (!refundRequest.canProcess()) {
            throw new ValidationException("REFUND_CANNOT_PROCESS", id);
        }

        // Get invoice
        Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(refundRequest.getInvoiceId())
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) refundRequest.getInvoiceId()));

        // Create refund adjustment (negative amount)
        InvoiceAdjustment adjustment = InvoiceAdjustment.builder()
                .type(InvoiceAdjustmentType.REFUND)
                .description("Hoàn tiền: " + refundRequest.getReason())
                .amount(refundRequest.getRefundAmount().negate()) // Negative
                .reason("Refund request #" + id)
                .build();

        invoice.addAdjustment(adjustment);

        // Update invoice amount paid (reduce by refund amount)
        invoice.setAmountPaid(invoice.getAmountPaid().subtract(refundRequest.getRefundAmount()));

        // Update invoice status to REFUNDED if fully refunded
        if (invoice.getAmountPaid().signum() <= 0) {
            invoice.setStatus(InvoiceStatus.REFUNDED);
        }

        invoiceRepository.save(invoice);

        // Mark refund as processed
        refundRequest.markAsProcessed();
        RefundRequest saved = refundRequestRepository.save(refundRequest);

        log.info("Processed refund request {}, invoice {} updated", id, invoice.getId());

        return invoiceMapper.toRefundResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundRequestResponse getRefundRequestById(Long id) {
        log.debug("Fetching refund request with ID: {}", id);

        RefundRequest refundRequest = refundRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("REFUND_REQUEST_NOT_FOUND", (Object) id));

        return invoiceMapper.toRefundResponse(refundRequest);
    }
}
