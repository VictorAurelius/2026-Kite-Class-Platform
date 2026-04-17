package com.kiteclass.core.module.invoice.service;

import com.kiteclass.core.common.constant.InstallmentPlanStatus;
import com.kiteclass.core.common.constant.InstallmentStatus;
import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.invoice.dto.CreateInstallmentPlanRequest;
import com.kiteclass.core.module.invoice.dto.InstallmentPlanResponse;
import com.kiteclass.core.module.invoice.entity.Installment;
import com.kiteclass.core.module.invoice.entity.InstallmentPlan;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.mapper.InvoiceMapper;
import com.kiteclass.core.module.invoice.repository.InstallmentPlanRepository;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InstallmentPlanServiceImpl}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>requestInstallmentPlan — success, invoice not found, plan already exists, invoice in final status</li>
 *   <li>approveInstallmentPlan — success</li>
 *   <li>rejectInstallmentPlan — success</li>
 *   <li>getInstallmentPlanById — success and not found</li>
 *   <li>getInstallmentPlanByInvoiceId — success and not found</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2026-03-24
 */
@ExtendWith(MockitoExtension.class)
class InstallmentPlanServiceTest {

    @Mock
    private InstallmentPlanRepository installmentPlanRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceMapper invoiceMapper;

    @InjectMocks
    private InstallmentPlanServiceImpl installmentPlanService;

    @Captor
    private ArgumentCaptor<InstallmentPlan> planCaptor;

    private static final Long INVOICE_ID = 100L;
    private static final Long PLAN_ID = 200L;
    private static final UUID INSTANCE_ID = UUID.randomUUID();

    private Invoice createTestInvoice() {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-2026-000001")
                .studentId(1L)
                .classId(1L)
                .enrollmentId(1L)
                .status(InvoiceStatus.SENT)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.of(2026, 4, 1))
                .periodStart(LocalDate.now())
                .periodEnd(LocalDate.now().plusMonths(3))
                .total(new BigDecimal("1000000.00"))
                .amountPaid(BigDecimal.ZERO)
                .build();
        invoice.setId(INVOICE_ID);
        invoice.setInstanceId(INSTANCE_ID);
        invoice.setDeleted(false);
        return invoice;
    }

    private CreateInstallmentPlanRequest createRequest(int numberOfInstallments) {
        return CreateInstallmentPlanRequest.builder()
                .invoiceId(INVOICE_ID)
                .numberOfInstallments(numberOfInstallments)
                .build();
    }

    private InstallmentPlanResponse createMockResponse() {
        return InstallmentPlanResponse.builder()
                .id(PLAN_ID)
                .invoiceId(INVOICE_ID)
                .numberOfInstallments(3)
                .status(InstallmentPlanStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("requestInstallmentPlan")
    class RequestInstallmentPlan {

        @Test
        @DisplayName("should create plan with correct installment amounts for 3 installments")
        void requestInstallmentPlan_success_threeInstallments() {
            // Given
            Invoice invoice = createTestInvoice();
            CreateInstallmentPlanRequest request = createRequest(3);

            when(invoiceRepository.findByIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(Optional.of(invoice));
            when(installmentPlanRepository.existsByInvoiceIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(false);
            when(installmentPlanRepository.save(any(InstallmentPlan.class)))
                    .thenAnswer(invocation -> {
                        InstallmentPlan saved = invocation.getArgument(0);
                        saved.setId(PLAN_ID);
                        return saved;
                    });
            when(invoiceMapper.toPlanResponse(any(InstallmentPlan.class)))
                    .thenReturn(createMockResponse());

            // When
            InstallmentPlanResponse response = installmentPlanService.requestInstallmentPlan(request);

            // Then
            assertThat(response).isNotNull();

            verify(installmentPlanRepository).save(planCaptor.capture());
            InstallmentPlan capturedPlan = planCaptor.getValue();

            assertThat(capturedPlan.getInvoiceId()).isEqualTo(INVOICE_ID);
            assertThat(capturedPlan.getNumberOfInstallments()).isEqualTo(3);
            assertThat(capturedPlan.getStatus()).isEqualTo(InstallmentPlanStatus.PENDING);
            assertThat(capturedPlan.getInstanceId()).isEqualTo(INSTANCE_ID);
            assertThat(capturedPlan.getInstallments()).hasSize(3);

            // Verify installment amounts: 1,000,000 / 3 = 333,333.33 each, last gets remainder
            BigDecimal expectedPerInstallment = new BigDecimal("1000000.00")
                    .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            BigDecimal expectedLastAmount = new BigDecimal("1000000.00")
                    .subtract(expectedPerInstallment.multiply(BigDecimal.valueOf(2)))
                    .setScale(2, RoundingMode.HALF_UP);

            Installment first = capturedPlan.getInstallments().get(0);
            assertThat(first.getInstallmentNumber()).isEqualTo(1);
            assertThat(first.getAmount()).isEqualByComparingTo(expectedPerInstallment);
            assertThat(first.getStatus()).isEqualTo(InstallmentStatus.PENDING);

            Installment second = capturedPlan.getInstallments().get(1);
            assertThat(second.getInstallmentNumber()).isEqualTo(2);
            assertThat(second.getAmount()).isEqualByComparingTo(expectedPerInstallment);

            Installment third = capturedPlan.getInstallments().get(2);
            assertThat(third.getInstallmentNumber()).isEqualTo(3);
            assertThat(third.getAmount()).isEqualByComparingTo(expectedLastAmount);

            // Verify total matches
            BigDecimal totalInstallments = first.getAmount()
                    .add(second.getAmount())
                    .add(third.getAmount());
            assertThat(totalInstallments).isEqualByComparingTo(new BigDecimal("1000000.00"));
        }

        @Test
        @DisplayName("should calculate due dates 1 month apart starting from invoice due date")
        void requestInstallmentPlan_success_dueDatesCalculation() {
            // Given
            Invoice invoice = createTestInvoice();
            LocalDate invoiceDueDate = invoice.getDueDate();
            CreateInstallmentPlanRequest request = createRequest(3);

            when(invoiceRepository.findByIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(Optional.of(invoice));
            when(installmentPlanRepository.existsByInvoiceIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(false);
            when(installmentPlanRepository.save(any(InstallmentPlan.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(invoiceMapper.toPlanResponse(any(InstallmentPlan.class)))
                    .thenReturn(createMockResponse());

            // When
            installmentPlanService.requestInstallmentPlan(request);

            // Then
            verify(installmentPlanRepository).save(planCaptor.capture());
            InstallmentPlan capturedPlan = planCaptor.getValue();

            assertThat(capturedPlan.getInstallments().get(0).getDueDate())
                    .isEqualTo(invoiceDueDate);
            assertThat(capturedPlan.getInstallments().get(1).getDueDate())
                    .isEqualTo(invoiceDueDate.plusMonths(1));
            assertThat(capturedPlan.getInstallments().get(2).getDueDate())
                    .isEqualTo(invoiceDueDate.plusMonths(2));
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when invoice not found")
        void requestInstallmentPlan_invoiceNotFound_throwsException() {
            // Given
            CreateInstallmentPlanRequest request = createRequest(3);

            when(invoiceRepository.findByIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> installmentPlanService.requestInstallmentPlan(request))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(installmentPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ValidationException when plan already exists for invoice")
        void requestInstallmentPlan_planAlreadyExists_throwsException() {
            // Given
            Invoice invoice = createTestInvoice();
            CreateInstallmentPlanRequest request = createRequest(3);

            when(invoiceRepository.findByIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(Optional.of(invoice));
            when(installmentPlanRepository.existsByInvoiceIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> installmentPlanService.requestInstallmentPlan(request))
                    .isInstanceOf(ValidationException.class);

            verify(installmentPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ValidationException when invoice is in final status (PAID)")
        void requestInstallmentPlan_invoiceFinalStatus_throwsException() {
            // Given
            Invoice invoice = createTestInvoice();
            invoice.setStatus(InvoiceStatus.PAID);
            CreateInstallmentPlanRequest request = createRequest(3);

            when(invoiceRepository.findByIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(Optional.of(invoice));
            when(installmentPlanRepository.existsByInvoiceIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> installmentPlanService.requestInstallmentPlan(request))
                    .isInstanceOf(ValidationException.class);

            verify(installmentPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle even division with 2 installments")
        void requestInstallmentPlan_success_evenDivision() {
            // Given: 1,000,000 / 2 = 500,000 each (no remainder)
            Invoice invoice = createTestInvoice();
            CreateInstallmentPlanRequest request = createRequest(2);

            when(invoiceRepository.findByIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(Optional.of(invoice));
            when(installmentPlanRepository.existsByInvoiceIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(false);
            when(installmentPlanRepository.save(any(InstallmentPlan.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(invoiceMapper.toPlanResponse(any(InstallmentPlan.class)))
                    .thenReturn(createMockResponse());

            // When
            installmentPlanService.requestInstallmentPlan(request);

            // Then
            verify(installmentPlanRepository).save(planCaptor.capture());
            InstallmentPlan capturedPlan = planCaptor.getValue();

            assertThat(capturedPlan.getInstallments()).hasSize(2);
            assertThat(capturedPlan.getInstallments().get(0).getAmount())
                    .isEqualByComparingTo(new BigDecimal("500000.00"));
            assertThat(capturedPlan.getInstallments().get(1).getAmount())
                    .isEqualByComparingTo(new BigDecimal("500000.00"));
        }
    }

    @Nested
    @DisplayName("approveInstallmentPlan")
    class ApproveInstallmentPlan {

        @Test
        @DisplayName("should approve and activate plan")
        void approveInstallmentPlan_success() {
            // Given
            Long approvedBy = 42L;
            InstallmentPlan plan = InstallmentPlan.builder()
                    .invoiceId(INVOICE_ID)
                    .numberOfInstallments(3)
                    .status(InstallmentPlanStatus.PENDING)
                    .build();
            plan.setId(PLAN_ID);
            plan.setInstanceId(INSTANCE_ID);

            when(installmentPlanRepository.findByIdAndDeletedFalse(PLAN_ID))
                    .thenReturn(Optional.of(plan));
            when(installmentPlanRepository.save(any(InstallmentPlan.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(invoiceMapper.toPlanResponse(any(InstallmentPlan.class)))
                    .thenReturn(createMockResponse());

            // When
            installmentPlanService.approveInstallmentPlan(PLAN_ID, approvedBy);

            // Then
            verify(installmentPlanRepository).save(planCaptor.capture());
            InstallmentPlan capturedPlan = planCaptor.getValue();

            // After approve() + activate(), status should be ACTIVE
            assertThat(capturedPlan.getStatus()).isEqualTo(InstallmentPlanStatus.ACTIVE);
            assertThat(capturedPlan.getApprovedBy()).isEqualTo(approvedBy);
            assertThat(capturedPlan.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when plan not found")
        void approveInstallmentPlan_notFound_throwsException() {
            // Given
            when(installmentPlanRepository.findByIdAndDeletedFalse(PLAN_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> installmentPlanService.approveInstallmentPlan(PLAN_ID, 42L))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(installmentPlanRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("rejectInstallmentPlan")
    class RejectInstallmentPlan {

        @Test
        @DisplayName("should reject plan with reason")
        void rejectInstallmentPlan_success() {
            // Given
            String reason = "Student has outstanding balance";
            InstallmentPlan plan = InstallmentPlan.builder()
                    .invoiceId(INVOICE_ID)
                    .numberOfInstallments(3)
                    .status(InstallmentPlanStatus.PENDING)
                    .build();
            plan.setId(PLAN_ID);
            plan.setInstanceId(INSTANCE_ID);

            when(installmentPlanRepository.findByIdAndDeletedFalse(PLAN_ID))
                    .thenReturn(Optional.of(plan));
            when(installmentPlanRepository.save(any(InstallmentPlan.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(invoiceMapper.toPlanResponse(any(InstallmentPlan.class)))
                    .thenReturn(createMockResponse());

            // When
            installmentPlanService.rejectInstallmentPlan(PLAN_ID, reason);

            // Then
            verify(installmentPlanRepository).save(planCaptor.capture());
            InstallmentPlan capturedPlan = planCaptor.getValue();

            assertThat(capturedPlan.getStatus()).isEqualTo(InstallmentPlanStatus.REJECTED);
            assertThat(capturedPlan.getRejectionReason()).isEqualTo(reason);
            assertThat(capturedPlan.getRejectedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when plan not found")
        void rejectInstallmentPlan_notFound_throwsException() {
            // Given
            when(installmentPlanRepository.findByIdAndDeletedFalse(PLAN_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> installmentPlanService.rejectInstallmentPlan(PLAN_ID, "reason"))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(installmentPlanRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getInstallmentPlanById")
    class GetInstallmentPlanById {

        @Test
        @DisplayName("should return plan response when found")
        void getInstallmentPlanById_success() {
            // Given
            InstallmentPlan plan = InstallmentPlan.builder()
                    .invoiceId(INVOICE_ID)
                    .numberOfInstallments(3)
                    .status(InstallmentPlanStatus.ACTIVE)
                    .build();
            plan.setId(PLAN_ID);

            InstallmentPlanResponse expectedResponse = createMockResponse();

            when(installmentPlanRepository.findByIdAndDeletedFalse(PLAN_ID))
                    .thenReturn(Optional.of(plan));
            when(invoiceMapper.toPlanResponse(plan))
                    .thenReturn(expectedResponse);

            // When
            InstallmentPlanResponse response = installmentPlanService.getInstallmentPlanById(PLAN_ID);

            // Then
            assertThat(response).isEqualTo(expectedResponse);
            verify(installmentPlanRepository).findByIdAndDeletedFalse(PLAN_ID);
            verify(invoiceMapper).toPlanResponse(plan);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when plan not found")
        void getInstallmentPlanById_notFound_throwsException() {
            // Given
            when(installmentPlanRepository.findByIdAndDeletedFalse(PLAN_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> installmentPlanService.getInstallmentPlanById(PLAN_ID))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(invoiceMapper, never()).toPlanResponse(any(InstallmentPlan.class));
        }
    }

    @Nested
    @DisplayName("getInstallmentPlanByInvoiceId")
    class GetInstallmentPlanByInvoiceId {

        @Test
        @DisplayName("should return plan response when found by invoice ID")
        void getInstallmentPlanByInvoiceId_success() {
            // Given
            InstallmentPlan plan = InstallmentPlan.builder()
                    .invoiceId(INVOICE_ID)
                    .numberOfInstallments(3)
                    .status(InstallmentPlanStatus.ACTIVE)
                    .build();
            plan.setId(PLAN_ID);

            InstallmentPlanResponse expectedResponse = createMockResponse();

            when(installmentPlanRepository.findByInvoiceIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(Optional.of(plan));
            when(invoiceMapper.toPlanResponse(plan))
                    .thenReturn(expectedResponse);

            // When
            InstallmentPlanResponse response = installmentPlanService.getInstallmentPlanByInvoiceId(INVOICE_ID);

            // Then
            assertThat(response).isEqualTo(expectedResponse);
            verify(installmentPlanRepository).findByInvoiceIdAndDeletedFalse(INVOICE_ID);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when plan not found by invoice ID")
        void getInstallmentPlanByInvoiceId_notFound_throwsException() {
            // Given
            when(installmentPlanRepository.findByInvoiceIdAndDeletedFalse(INVOICE_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> installmentPlanService.getInstallmentPlanByInvoiceId(INVOICE_ID))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(invoiceMapper, never()).toPlanResponse(any(InstallmentPlan.class));
        }
    }
}
