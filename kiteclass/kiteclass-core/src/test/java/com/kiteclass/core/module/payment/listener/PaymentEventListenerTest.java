package com.kiteclass.core.module.payment.listener;

import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.invoice.dto.InstallmentPlanResponse;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.invoice.service.InstallmentPlanService;
import com.kiteclass.core.module.payment.entity.Payment;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import com.kiteclass.core.module.payment.event.PaymentCompletedEvent;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentEventListener}.
 * Tests invoice amount update when payment completes.
 *
 * @author KiteClass Team
 * @since 2.8.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventListener Tests")
class PaymentEventListenerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InstallmentPlanService installmentPlanService;

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    private Invoice testInvoice;
    private Payment testPayment;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();

        // Setup Invoice with 0 amountPaid
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
                .amountPaid(new BigDecimal("0.00"))
                .build();
        testInvoice.setId(1L);
        testInvoice.setInstanceId(tenantId);

        // Setup completed Payment
        testPayment = Payment.builder()
                .paymentNumber("PAY-2026-000001")
                .transactionId("TXN1234567890")
                .invoiceId(1L)
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(PaymentStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
        testPayment.setId(1L);
        testPayment.setInstanceId(tenantId);
    }

    @Test
    @DisplayName("Should update invoice amountPaid when payment completes")
    void shouldUpdateInvoiceAmountPaidOnPaymentCompleted() {
        // Arrange
        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);

        PaymentCompletedEvent event = new PaymentCompletedEvent(this, testPayment);

        // Act
        paymentEventListener.onPaymentCompleted(event);

        // Assert
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(new BigDecimal("500000.00")) == 0
        ));
    }

    @Test
    @DisplayName("Should handle partial payment - update amountPaid correctly")
    void shouldHandlePartialPayment() {
        // Arrange - Invoice already has 300000 paid
        testInvoice.setAmountPaid(new BigDecimal("300000.00"));

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);

        PaymentCompletedEvent event = new PaymentCompletedEvent(this, testPayment);

        // Act
        paymentEventListener.onPaymentCompleted(event);

        // Assert - amountPaid should be 300000 + 500000 = 800000
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(new BigDecimal("800000.00")) == 0
        ));
    }

    @Test
    @DisplayName("Should handle full payment - update amountPaid to total")
    void shouldHandleFullPayment() {
        // Arrange - Payment covers full invoice
        testPayment.setAmount(new BigDecimal("1000000.00"));

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);

        PaymentCompletedEvent event = new PaymentCompletedEvent(this, testPayment);

        // Act
        paymentEventListener.onPaymentCompleted(event);

        // Assert - amountPaid should equal total
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(new BigDecimal("1000000.00")) == 0
        ));
    }

    @Test
    @DisplayName("Should not throw exception when invoice not found - silent failure")
    void shouldHandleMissingInvoiceSilently() {
        // Arrange
        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.empty());

        PaymentCompletedEvent event = new PaymentCompletedEvent(this, testPayment);

        // Act - Should not throw, just log error
        paymentEventListener.onPaymentCompleted(event);

        // Assert - save should never be called
        verify(invoiceRepository, never()).save(testInvoice);
    }

    @Test
    @DisplayName("Should handle multiple payments on same invoice")
    void shouldHandleMultiplePayments() {
        // Arrange - First payment already processed
        testInvoice.setAmountPaid(new BigDecimal("500000.00"));

        // Second payment
        Payment secondPayment = Payment.builder()
                .paymentNumber("PAY-2026-000002")
                .transactionId("TXN0987654321")
                .invoiceId(1L)
                .amount(new BigDecimal("300000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();
        secondPayment.setId(2L);
        secondPayment.setInstanceId(tenantId);

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);

        PaymentCompletedEvent event = new PaymentCompletedEvent(this, secondPayment);

        // Act
        paymentEventListener.onPaymentCompleted(event);

        // Assert - amountPaid should be 500000 + 300000 = 800000
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(new BigDecimal("800000.00")) == 0
        ));
    }

    @Test
    @DisplayName("Should record installment payment when installmentId present")
    void shouldRecordInstallmentPaymentWhenInstallmentIdPresent() {
        // Arrange - Payment linked to installment
        Long installmentId = 1000L;
        testPayment.setInstallmentId(installmentId);

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);
        when(installmentPlanService.recordInstallmentPayment(eq(installmentId), eq(testPayment.getAmount())))
                .thenReturn(mock(InstallmentPlanResponse.class));

        PaymentCompletedEvent event = new PaymentCompletedEvent(this, testPayment);

        // Act
        paymentEventListener.onPaymentCompleted(event);

        // Assert
        verify(installmentPlanService).recordInstallmentPayment(installmentId, testPayment.getAmount());
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(new BigDecimal("500000.00")) == 0
        ));
    }

    @Test
    @DisplayName("Should continue when installment not found")
    void shouldContinueWhenInstallmentNotFound() {
        // Arrange
        Long installmentId = 9999L;
        testPayment.setInstallmentId(installmentId);

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);
        when(installmentPlanService.recordInstallmentPayment(eq(installmentId), any(BigDecimal.class)))
                .thenThrow(new EntityNotFoundException("INSTALLMENT_NOT_FOUND", installmentId));

        PaymentCompletedEvent event = new PaymentCompletedEvent(this, testPayment);

        // Act - Should NOT throw exception
        paymentEventListener.onPaymentCompleted(event);

        // Assert - Invoice still updated despite installment failure
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(new BigDecimal("500000.00")) == 0
        ));
    }

    @Test
    @DisplayName("Should continue when installment already paid")
    void shouldContinueWhenInstallmentAlreadyPaid() {
        // Arrange
        Long installmentId = 1000L;
        testPayment.setInstallmentId(installmentId);

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);
        when(installmentPlanService.recordInstallmentPayment(eq(installmentId), any(BigDecimal.class)))
                .thenThrow(new IllegalStateException("Cannot pay installment with status: PAID"));

        PaymentCompletedEvent event = new PaymentCompletedEvent(this, testPayment);

        // Act - Should NOT throw exception
        paymentEventListener.onPaymentCompleted(event);

        // Assert - Invoice still updated despite installment failure
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(new BigDecimal("500000.00")) == 0
        ));
    }

    @Test
    @DisplayName("Should skip installment when installmentId is null")
    void shouldSkipInstallmentWhenInstallmentIdIsNull() {
        // Arrange - Payment without installment
        testPayment.setInstallmentId(null);

        when(invoiceRepository.findById(1L))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(testInvoice))
                .thenReturn(testInvoice);

        PaymentCompletedEvent event = new PaymentCompletedEvent(this, testPayment);

        // Act
        paymentEventListener.onPaymentCompleted(event);

        // Assert - Installment service NOT called
        verify(installmentPlanService, never()).recordInstallmentPayment(any(), any());
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getAmountPaid().compareTo(new BigDecimal("500000.00")) == 0
        ));
    }
}
