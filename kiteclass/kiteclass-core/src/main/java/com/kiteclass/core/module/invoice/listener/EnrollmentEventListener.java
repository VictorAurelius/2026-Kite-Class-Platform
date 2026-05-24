package com.kiteclass.core.module.invoice.listener;

import com.kiteclass.core.module.enrollment.event.EnrollmentCreatedEvent;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for enrollment events.
 *
 * <p>Listens to ENROLLMENT_CREATED events and auto-creates invoices.
 * This implements event-driven architecture for invoice generation.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentEventListener {

    private final InvoiceService invoiceService;

    /**
     * Handles ENROLLMENT_CREATED event by auto-creating invoice.
     *
     * <p>Event flow:
     * <ol>
     *   <li>Enrollment created → ENROLLMENT_CREATED event published</li>
     *   <li>This listener receives event</li>
     *   <li>Auto-create invoice for enrollment</li>
     *   <li>Publish INVOICE_CREATED event (for future Payment Module)</li>
     * </ol>
     *
     * <p>Error handling:
     * <ul>
     *   <li>Exceptions are logged but not re-thrown</li>
     *   <li>Enrollment should not fail if invoice creation fails</li>
     *   <li>Invoice can be created manually later if needed</li>
     * </ul>
     *
     * @param event the enrollment created event
     */
    // Wave beta-readiness-1 Bucket B — capacity-race fix (same as GradeEventListener).
    // Changed from @EventListener + @Transactional (REQUIRED) to
    // @TransactionalEventListener(AFTER_COMMIT) + @Transactional(REQUIRES_NEW).
    //
    // InvoiceService.createInvoiceForEnrollment reads the Class entity; when fired
    // inside the enrollment TX (OPTIMISTIC_FORCE_INCREMENT locked), Hibernate bumped
    // Class.version twice at commit → ObjectOptimisticLockingFailureException rolled
    // back the enrollment. Now fires AFTER enrollment commits, in its own TX.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEnrollmentCreated(EnrollmentCreatedEvent event) {
        log.info("Received ENROLLMENT_CREATED event for enrollment ID: {}",
                event.getEnrollment().getId());

        try {
            Invoice invoice = invoiceService.createInvoiceForEnrollment(
                    event.getEnrollment().getId());

            log.info("Auto-created invoice {} for enrollment {}",
                    invoice.getInvoiceNumber(), event.getEnrollment().getId());

        } catch (Exception e) {
            log.error("Failed to create invoice for enrollment {}: {}",
                    event.getEnrollment().getId(), e.getMessage(), e);

            // Don't throw - enrollment should not fail if invoice fails
            // Invoice can be created manually later
        }
    }
}
