package com.kiteclass.core.module.payment.record.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.payment.record.dto.PaymentRecordResponse;
import com.kiteclass.core.module.payment.record.dto.RecordPaymentRequest;
import com.kiteclass.core.module.payment.record.entity.PaymentRecord;
import com.kiteclass.core.module.payment.record.entity.PaymentRecordMethod;
import com.kiteclass.core.module.payment.record.repository.PaymentRecordRepository;
import com.kiteclass.core.module.payment.record.service.impl.PaymentRecordServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentRecordServiceImpl} — Wave beta-readiness-4 Bucket C (GAP-292b).
 *
 * <p>Validates:
 * <ul>
 *   <li>BR-PAYMENT-METHOD-003: Cross-tenant defense (OWASP A01)</li>
 *   <li>BR-PAYMENT-METHOD-005: invoice.amount_paid running total update</li>
 *   <li>BR-PAYMENT-METHOD-006: recorded_by audit trail</li>
 *   <li>Idempotency-Key header acknowledged (logged) per BR-PAYMENT-METHOD-004</li>
 * </ul>
 *
 * <p>Note: per `postgres-specific-type-testcontainers.md` v1.0.0 mandate, PaymentRecord
 * entity does NOT use Postgres-specific types (VARCHAR + NUMERIC + TIMESTAMPTZ standard) →
 * H2/Mockito test acceptable cho service logic. Full @DataJpaTest Testcontainers IT có thể
 * add follow-up nếu Postgres-specific binding gây incident.
 */
@DisplayName("PaymentRecordServiceImpl — record payment with cross-tenant defense")
@ExtendWith(MockitoExtension.class)
class PaymentRecordServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @InjectMocks
    private PaymentRecordServiceImpl service;

    private final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(TENANT_A);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Record CASH payment for Lớp Anh ngữ 5A1 — Trung tâm Sky Education, 1.500.000đ")
    void recordPayment_cash_happyPath() {
        Long invoiceId = 42L;
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInstanceId(TENANT_A);
        invoice.setAmountPaid(BigDecimal.ZERO);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRecordRepository.save(any(PaymentRecord.class)))
                .thenAnswer(inv -> {
                    PaymentRecord pr = inv.getArgument(0);
                    pr.setId(17L);
                    return pr;
                });

        RecordPaymentRequest req = RecordPaymentRequest.builder()
                .method(PaymentRecordMethod.CASH)
                .amount(new BigDecimal("1500000"))
                .paidAt(Instant.parse("2026-05-24T08:30:00Z"))
                .note("Phụ huynh em Trần Thị Hồng thanh toán tháng 5/2026")
                .build();

        PaymentRecordResponse response = service.recordPayment(invoiceId, req, 7L, "idem-key-001");

        assertThat(response.getId()).isEqualTo(17L);
        assertThat(response.getInvoiceId()).isEqualTo(42L);
        assertThat(response.getMethod()).isEqualTo(PaymentRecordMethod.CASH);
        assertThat(response.getAmount()).isEqualByComparingTo("1500000");
        assertThat(response.getRecordedBy()).isEqualTo(7L);

        // BR-PAYMENT-METHOD-005: invoice.amount_paid updated to running total
        assertThat(invoice.getAmountPaid()).isEqualByComparingTo("1500000");
        verify(invoiceRepository).save(invoice);
    }

    @Test
    @DisplayName("BR-PAYMENT-METHOD-003 — Cross-tenant attempt rejected with PermissionDeniedException")
    void recordPayment_crossTenant_denied() {
        Long invoiceId = 42L;
        Invoice invoiceOfOtherTenant = new Invoice();
        invoiceOfOtherTenant.setId(invoiceId);
        invoiceOfOtherTenant.setInstanceId(TENANT_B);  // belongs to TENANT_B
        invoiceOfOtherTenant.setAmountPaid(BigDecimal.ZERO);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoiceOfOtherTenant));

        // Current tenant context = TENANT_A; invoice belongs to TENANT_B
        RecordPaymentRequest req = RecordPaymentRequest.builder()
                .method(PaymentRecordMethod.BANK_TRANSFER)
                .amount(new BigDecimal("1000000"))
                .build();

        assertThatThrownBy(() -> service.recordPayment(invoiceId, req, 7L, null))
                .isInstanceOf(PermissionDeniedException.class);

        // Defense verified: no PaymentRecord persisted, no invoice updated
        verify(paymentRecordRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Invoice not found returns EntityNotFoundException")
    void recordPayment_invoiceNotFound_throws() {
        Long invoiceId = 999L;
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        RecordPaymentRequest req = RecordPaymentRequest.builder()
                .method(PaymentRecordMethod.CASH)
                .amount(new BigDecimal("500000"))
                .build();

        assertThatThrownBy(() -> service.recordPayment(invoiceId, req, 7L, null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Running total: 2 payments accumulate amount_paid correctly")
    void recordPayment_runningTotal_accumulates() {
        Long invoiceId = 42L;
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInstanceId(TENANT_A);
        invoice.setAmountPaid(new BigDecimal("500000"));  // already 500k paid

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRecordRepository.save(any(PaymentRecord.class)))
                .thenAnswer(inv -> {
                    PaymentRecord pr = inv.getArgument(0);
                    pr.setId(18L);
                    return pr;
                });

        RecordPaymentRequest req = RecordPaymentRequest.builder()
                .method(PaymentRecordMethod.VIETQR)
                .amount(new BigDecimal("700000"))
                .build();

        service.recordPayment(invoiceId, req, 7L, null);

        // 500k existing + 700k new = 1.2M total
        assertThat(invoice.getAmountPaid()).isEqualByComparingTo("1200000");
    }

    @Test
    @DisplayName("Default paidAt = now() when request omits paidAt")
    void recordPayment_defaultPaidAt_now() {
        Long invoiceId = 42L;
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInstanceId(TENANT_A);
        invoice.setAmountPaid(BigDecimal.ZERO);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRecordRepository.save(any(PaymentRecord.class)))
                .thenAnswer(inv -> {
                    PaymentRecord pr = inv.getArgument(0);
                    pr.setId(19L);
                    return pr;
                });

        RecordPaymentRequest req = RecordPaymentRequest.builder()
                .method(PaymentRecordMethod.MOMO)
                .amount(new BigDecimal("300000"))
                .paidAt(null)  // explicit null
                .build();

        Instant before = Instant.now();
        PaymentRecordResponse response = service.recordPayment(invoiceId, req, 7L, null);
        Instant after = Instant.now();

        assertThat(response.getPaidAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }
}
