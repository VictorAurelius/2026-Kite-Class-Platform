package com.kiteclass.core.module.payment.listener;

import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.payment.entity.Payment;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import com.kiteclass.core.module.payment.event.PaymentRefundedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentEventListener} refund handling.
 * Tests invoice amount update when payment is refunded.
 *
 * @author KiteClass Team
 * @since 2.8.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Refund Event Listener Tests")
class PaymentRefundEventListenerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    private Invoice testInvoice;
    private Payment testPayment;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();

        // Setup Invoice with amountPaid = 500,000
        testInvoice = Invoice.builder()
                .invoiceNumber("INV-2026-000001")
                .enrollmentId(1L)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .periodStart(LocalDate.now())
                .periodEnd(LocalDate.now().plusDays(90))
                .subtotal(new BigDecimal("1000000.00"))
                .discount(new BigDecimal("0.00"))
                .total(new BigDecimal("1000000.00"))
                .amountPaid(new BigDecimal("500000.00")) // Already paid 500k
                .build();
        testInvoice.setId(1L);
        testInvoice.setInstanceId(tenantId);

        // Setup refunded Payment
        testPayment = Payment.builder()
                .paymentNumber("PAY-2026-000001")
                .transactionId("TXN1234567890")
                .invoiceId(1L)
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(PaymentStatus.REFUNDED)
                .refundedAt(LocalDateTime.now())
                .build();
        testPayment.setId(1L);
        testPayment.setInstanceId(tenantId);
    }

    @Test
    @DisplayName("Should decrease invoice amountPaid when payment refunded")
    void shouldDecreaseInvoiceAmountPaidOnRefund() {
        // Arrange
        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);

        PaymentRefundedEvent event = new PaymentRefundedEvent(this, testPayment);

        // Act
        paymentEventListener.onPaymentRefunded(event);

        // Assert - amountPaid should decrease: 500,000 - 500,000 = 0
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    @Test
    @DisplayName("Should handle partial refund - decrease amountPaid correctly")
    void shouldHandlePartialRefund() {
        // Arrange - Refund only 200,000 out of 500,000 paid
        testPayment.setAmount(new BigDecimal("200000.00"));

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);

        PaymentRefundedEvent event = new PaymentRefundedEvent(this, testPayment);

        // Act
        paymentEventListener.onPaymentRefunded(event);

        // Assert - amountPaid should be: 500,000 - 200,000 = 300,000
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(new BigDecimal("300000.00")) == 0
        ));
    }

    @Test
    @DisplayName("Should prevent negative amountPaid on over-refund")
    void shouldPreventNegativeAmountPaidOnOverRefund() {
        // Arrange - Try to refund more than paid (edge case/error scenario)
        testPayment.setAmount(new BigDecimal("700000.00")); // More than 500k paid

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);

        PaymentRefundedEvent event = new PaymentRefundedEvent(this, testPayment);

        // Act
        paymentEventListener.onPaymentRefunded(event);

        // Assert - amountPaid should be clamped to 0 (not negative)
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    @Test
    @DisplayName("Should handle refund when invoice fully paid")
    void shouldHandleRefundWhenInvoiceFullyPaid() {
        // Arrange - Invoice fully paid
        testInvoice.setAmountPaid(new BigDecimal("1000000.00"));

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);

        PaymentRefundedEvent event = new PaymentRefundedEvent(this, testPayment);

        // Act
        paymentEventListener.onPaymentRefunded(event);

        // Assert - amountPaid should decrease: 1,000,000 - 500,000 = 500,000
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(new BigDecimal("500000.00")) == 0
        ));
    }

    @Test
    @DisplayName("Should not throw exception when invoice not found - silent failure")
    void shouldHandleMissingInvoiceSilently() {
        // Arrange
        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.empty());

        PaymentRefundedEvent event = new PaymentRefundedEvent(this, testPayment);

        // Act - Should not throw, just log error
        paymentEventListener.onPaymentRefunded(event);

        // Assert - save should never be called
        verify(invoiceRepository, never()).save(testInvoice);
    }

    @Test
    @DisplayName("Should handle multiple refunds on same invoice")
    void shouldHandleMultipleRefunds() {
        // Arrange - Invoice has 800k paid (after 2 payments)
        testInvoice.setAmountPaid(new BigDecimal("800000.00"));

        // First refund: 300k
        Payment firstRefund = Payment.builder()
                .paymentNumber("PAY-2026-000001")
                .transactionId("TXN111")
                .invoiceId(1L)
                .amount(new BigDecimal("300000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.REFUNDED)
                .build();
        firstRefund.setId(1L);
        firstRefund.setInstanceId(tenantId);

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);

        PaymentRefundedEvent firstEvent = new PaymentRefundedEvent(this, firstRefund);

        // Act - First refund
        paymentEventListener.onPaymentRefunded(firstEvent);

        // Assert - amountPaid should be: 800,000 - 300,000 = 500,000
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(new BigDecimal("500000.00")) == 0
        ));
    }
}
