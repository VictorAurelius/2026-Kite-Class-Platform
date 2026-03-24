package com.kiteclass.core.module.invoice.scheduler;

import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler that marks overdue invoices daily.
 * Runs at 3:00 AM every day to check for SENT/PARTIAL invoices past their due date.
 *
 * <p>Only invoices with non-final status (SENT, PARTIAL) are eligible.
 * Already OVERDUE, PAID, CANCELLED, or REFUNDED invoices are excluded by the query.
 *
 * @author KiteClass Team
 * @since 2026-03-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceOverdueScheduler {

    private final InvoiceRepository invoiceRepository;

    /**
     * Check and mark overdue invoices.
     * Finds all SENT/PARTIAL invoices with dueDate before today and marks them OVERDUE.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void markOverdueInvoices() {
        log.info("Starting overdue invoice check...");

        LocalDate today = LocalDate.now();
        List<Invoice> overdueInvoices = invoiceRepository.findInvoicesEligibleForOverdue(today);

        if (overdueInvoices.isEmpty()) {
            log.info("No overdue invoices found");
            return;
        }

        int count = 0;
        for (Invoice invoice : overdueInvoices) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
            count++;
            log.debug("Marked invoice {} as OVERDUE (due: {})", invoice.getId(), invoice.getDueDate());
        }

        log.info("Overdue invoice check completed: {} invoices marked as OVERDUE", count);
    }
}
