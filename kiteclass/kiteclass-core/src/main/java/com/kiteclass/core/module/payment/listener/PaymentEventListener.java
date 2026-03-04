package com.kiteclass.core.module.payment.listener;

import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.payment.entity.Payment;
import com.kiteclass.core.module.payment.event.PaymentCompletedEvent;
import com.kiteclass.core.module.payment.event.PaymentRefundedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Event listener for payment events.
 * Updates invoice when payment is completed.
 *
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final InvoiceRepository invoiceRepository;

    /**
     * Handles PaymentCompletedEvent.
     * Updates Invoice.amountPaid which triggers status transition.
     *
     * @param event payment completed event
     */
    @EventListener
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        Payment payment = event.getPayment();

        try {
            // Update Invoice.amountPaid
            Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) payment.getInvoiceId()));

            BigDecimal newAmountPaid = invoice.getAmountPaid().add(payment.getAmount());
            invoice.setAmountPaid(newAmountPaid);

            // Save triggers @PreUpdate → calculateTotals() → updateStatus()
            invoiceRepository.save(invoice);

            log.info("Updated invoice {} amountPaid to {} (payment {})",
                invoice.getInvoiceNumber(), newAmountPaid, payment.getPaymentNumber());

            // TODO: [PR-INSTALLMENT] Implement Installment entity and update status when payment completes
            // Need to add Installment module: entity, repository, service to track payment plans
            if (payment.getInstallmentId() != null) {
                log.warn("Installment payment not supported yet (installmentId: {})",
                    payment.getInstallmentId());
            }

        } catch (Exception e) {
            log.error("Failed to update invoice for payment {}: {}",
                payment.getPaymentNumber(), e.getMessage(), e);
            // Don't throw - payment should not fail if invoice update fails
            // This is a best-effort operation that can be retried manually
        }
    }

    /**
     * Handles PaymentRefundedEvent.
     * Decreases Invoice.amountPaid when payment is refunded.
     *
     * @param event payment refunded event
     */
    @EventListener
    @Transactional
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        Payment payment = event.getPayment();

        try {
            // Decrease Invoice.amountPaid
            Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) payment.getInvoiceId()));

            BigDecimal newAmountPaid = invoice.getAmountPaid().subtract(payment.getAmount());

            // Prevent negative amountPaid
            if (newAmountPaid.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Refund amount {} exceeds invoice amountPaid {} for invoice {}",
                    payment.getAmount(), invoice.getAmountPaid(), invoice.getInvoiceNumber());
                newAmountPaid = BigDecimal.ZERO;
            }

            invoice.setAmountPaid(newAmountPaid);

            // Save triggers @PreUpdate → calculateTotals() → updateStatus()
            invoiceRepository.save(invoice);

            log.info("Updated invoice {} amountPaid to {} after refund (payment {})",
                invoice.getInvoiceNumber(), newAmountPaid, payment.getPaymentNumber());

        } catch (Exception e) {
            log.error("Failed to update invoice for refunded payment {}: {}",
                payment.getPaymentNumber(), e.getMessage(), e);
            // Don't throw - refund should not fail if invoice update fails
        }
    }
}
