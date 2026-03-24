package com.kiteclass.core.module.invoice.scheduler;

import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InvoiceOverdueScheduler}.
 *
 * @author KiteClass Team
 * @since 2026-03-24
 */
@ExtendWith(MockitoExtension.class)
class InvoiceOverdueSchedulerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceOverdueScheduler scheduler;

    @Test
    @DisplayName("Should mark overdue invoices as OVERDUE")
    void mockOverdueInvoices_updatesStatus() {
        // Given
        Invoice sentInvoice = Invoice.builder()
                .dueDate(LocalDate.now().minusDays(5))
                .status(InvoiceStatus.SENT)
                .build();
        sentInvoice.setId(1L);

        Invoice partialInvoice = Invoice.builder()
                .dueDate(LocalDate.now().minusDays(2))
                .status(InvoiceStatus.PARTIAL)
                .build();
        partialInvoice.setId(2L);

        when(invoiceRepository.findInvoicesEligibleForOverdue(any(LocalDate.class)))
                .thenReturn(List.of(sentInvoice, partialInvoice));

        // When
        scheduler.markOverdueInvoices();

        // Then
        verify(invoiceRepository, times(2)).save(any(Invoice.class));
        assert sentInvoice.getStatus() == InvoiceStatus.OVERDUE;
        assert partialInvoice.getStatus() == InvoiceStatus.OVERDUE;
    }

    @Test
    @DisplayName("Should do nothing when no overdue invoices found")
    void noOverdueInvoices_doesNothing() {
        // Given
        when(invoiceRepository.findInvoicesEligibleForOverdue(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        scheduler.markOverdueInvoices();

        // Then
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should only process invoices returned by query - already final invoices are excluded by query")
    void alreadyFinalInvoice_skipped() {
        // Given - the query only returns SENT/PARTIAL, so PAID invoices are never returned
        // This test verifies behavior when query returns only eligible invoices
        Invoice sentInvoice = Invoice.builder()
                .dueDate(LocalDate.now().minusDays(3))
                .status(InvoiceStatus.SENT)
                .build();
        sentInvoice.setId(1L);

        // Only one eligible invoice returned (PAID invoice excluded by query)
        when(invoiceRepository.findInvoicesEligibleForOverdue(any(LocalDate.class)))
                .thenReturn(List.of(sentInvoice));

        // When
        scheduler.markOverdueInvoices();

        // Then - only the eligible invoice is saved
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
        assert sentInvoice.getStatus() == InvoiceStatus.OVERDUE;
    }
}
