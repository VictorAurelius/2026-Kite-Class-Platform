package com.kiteclass.core.module.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.payment.dto.CreatePaymentRequest;
import com.kiteclass.core.module.payment.entity.Payment;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import com.kiteclass.core.module.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Payment module.
 *
 * <p>Tests the full stack: Controller → Service → Repository → Database → Event Listeners.
 *
 * @author KiteClass Team
 * @since 2.8.1
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("Payment Integration Tests")
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private Invoice savedInvoice;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Set TenantContext for EntityPersistenceListener
        TenantContext.setCurrentTenant(tenantId);

        // Create test invoice
        savedInvoice = Invoice.builder()
                .invoiceNumber("INV-2026-000001")
                .studentId(1L) // Required field
                .classId(1L)
                .enrollmentId(1L)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .periodStart(LocalDate.now())
                .periodEnd(LocalDate.now().plusDays(90))
                .subtotal(new BigDecimal("1000000.00"))
                .discount(new BigDecimal("0.00"))
                .total(new BigDecimal("1000000.00"))
                .amountPaid(new BigDecimal("0.00"))
                .status(InvoiceStatus.SENT)
                .build();
        savedInvoice.setInstanceId(tenantId);
        savedInvoice = invoiceRepository.save(savedInvoice);
    }

    @Test
    @DisplayName("Should create CASH payment and complete immediately")
    void shouldCreateCashPaymentAndCompleteImmediately() throws Exception {
        // Arrange
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        // Act
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentNumber").exists())
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.amount").value(500000.00));

        // Assert - Payment created and completed
        List<Payment> payments = paymentRepository.findByInvoiceIdAndDeletedFalse(savedInvoice.getId());
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payments.get(0).getCompletedAt()).isNotNull();

        // Assert - Invoice amountPaid updated by event listener
        Invoice updatedInvoice = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getAmountPaid()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
    }

    @Test
    @DisplayName("Should create BANK_TRANSFER payment and complete immediately")
    void shouldCreateBankTransferPaymentAndCompleteImmediately() throws Exception {
        // Arrange
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("1000000.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        // Act
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"));

        // Assert - Full payment, invoice status = PAID
        Invoice updatedInvoice = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getAmountPaid()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("Should create VNPay payment with payment URL")
    void shouldCreateVNPayPaymentWithPaymentUrl() throws Exception {
        // Arrange
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.VNPAY)
                .ipAddress("192.168.1.1")
                .build();

        // Act
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("VNPAY"))
                .andExpect(jsonPath("$.paymentUrl").exists())
                .andExpect(jsonPath("$.transactionId").exists());

        // Assert - Payment created but NOT completed (waiting for gateway callback)
        List<Payment> payments = paymentRepository.findByInvoiceIdAndDeletedFalse(savedInvoice.getId());
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payments.get(0).getPaymentUrl()).isNotNull();
        assertThat(payments.get(0).getExpiresAt()).isNotNull();

        // Assert - Invoice NOT updated yet (no event until gateway callback)
        Invoice unchangedInvoice = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        assertThat(unchangedInvoice.getAmountPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(unchangedInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
    }

    @Test
    @DisplayName("Should handle multiple partial payments")
    void shouldHandleMultiplePartialPayments() throws Exception {
        // Arrange - First payment: 300,000
        CreatePaymentRequest firstPayment = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("300000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        // Act - Create first payment
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstPayment)))
                .andExpect(status().isCreated());

        // Assert - Invoice partially paid
        Invoice afterFirst = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        assertThat(afterFirst.getAmountPaid()).isEqualByComparingTo(new BigDecimal("300000.00"));
        assertThat(afterFirst.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);

        // Arrange - Second payment: 400,000
        CreatePaymentRequest secondPayment = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("400000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        // Act - Create second payment
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondPayment)))
                .andExpect(status().isCreated());

        // Assert - Invoice still partial (total paid: 700,000 / 1,000,000)
        Invoice afterSecond = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        assertThat(afterSecond.getAmountPaid()).isEqualByComparingTo(new BigDecimal("700000.00"));
        assertThat(afterSecond.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);

        // Arrange - Third payment: 300,000 (completes invoice)
        CreatePaymentRequest thirdPayment = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("300000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        // Act - Create third payment
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(thirdPayment)))
                .andExpect(status().isCreated());

        // Assert - Invoice fully paid
        Invoice afterThird = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        assertThat(afterThird.getAmountPaid()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(afterThird.getStatus()).isEqualTo(InvoiceStatus.PAID);

        // Assert - 3 payments created
        List<Payment> allPayments = paymentRepository.findByInvoiceIdAndDeletedFalse(savedInvoice.getId());
        assertThat(allPayments).hasSize(3);
    }

    @Test
    @DisplayName("Should reject payment when invoice already paid")
    void shouldRejectPaymentWhenInvoiceAlreadyPaid() throws Exception {
        // Arrange - Mark invoice as PAID
        savedInvoice.setAmountPaid(savedInvoice.getTotal());
        savedInvoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(savedInvoice);

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("100000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVOICE_ALREADY_PAID"));
    }

    @Test
    @DisplayName("Should reject payment when amount exceeds balance due")
    void shouldRejectPaymentWhenAmountExceedsBalance() throws Exception {
        // Arrange
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("1500000.00")) // Exceeds total
                .paymentMethod(PaymentMethod.CASH)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PAYMENT_AMOUNT_EXCEEDS_BALANCE"));
    }

    @Test
    @DisplayName("Should get payment by ID")
    void shouldGetPaymentById() throws Exception {
        // Arrange - Create payment
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        String createResponse = mockMvc.perform(post("/api/v1/payments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long paymentId = objectMapper.readTree(createResponse).get("id").asLong();

        // Act & Assert - Get payment by ID
        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.paymentNumber").exists())
                .andExpect(jsonPath("$.amount").value(500000.00));
    }

    @Test
    @DisplayName("Should get payments by invoice ID")
    void shouldGetPaymentsByInvoiceId() throws Exception {
        // Arrange - Create 2 payments
        CreatePaymentRequest payment1 = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("300000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        CreatePaymentRequest payment2 = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("400000.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        mockMvc.perform(post("/api/v1/payments")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payment1)));

        mockMvc.perform(post("/api/v1/payments")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payment2)));

        // Act & Assert - Get all payments for invoice
        mockMvc.perform(get("/api/v1/payments/invoice/" + savedInvoice.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should cancel pending payment")
    void shouldCancelPendingPayment() throws Exception {
        // Arrange - Create VNPay payment (PENDING)
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        String createResponse = mockMvc.perform(post("/api/v1/payments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long paymentId = objectMapper.readTree(createResponse).get("id").asLong();

        // Act - Cancel payment
        mockMvc.perform(put("/api/v1/payments/" + paymentId + "/cancel")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());

        // Assert - Payment cancelled
        Payment cancelledPayment = paymentRepository.findByIdAndDeletedFalse(paymentId).orElseThrow();
        assertThat(cancelledPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(cancelledPayment.getFailureReason()).contains("Cancelled by user");
    }

    @Test
    @DisplayName("Should process refund and update invoice amountPaid")
    void shouldProcessRefundAndUpdateInvoiceAmountPaid() throws Exception {
        // Arrange - Create and complete a CASH payment
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        String createResponse = mockMvc.perform(post("/api/v1/payments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long paymentId = objectMapper.readTree(createResponse).get("id").asLong();

        // Verify invoice updated
        Invoice afterPayment = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        assertThat(afterPayment.getAmountPaid()).isEqualByComparingTo(new BigDecimal("500000.00"));

        // Act - Process refund
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());

        // Assert - Payment refunded
        Payment refundedPayment = paymentRepository.findByIdAndDeletedFalse(paymentId).orElseThrow();
        assertThat(refundedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refundedPayment.getRefundedAt()).isNotNull();

        // Assert - Invoice amountPaid decreased back to 0
        Invoice afterRefund = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        assertThat(afterRefund.getAmountPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(afterRefund.getStatus()).isEqualTo(InvoiceStatus.SENT); // Back to SENT from PARTIAL
    }

    @Test
    @DisplayName("Should handle partial refund correctly")
    void shouldHandlePartialRefund() throws Exception {
        // Arrange - Make 2 payments totaling 800k
        CreatePaymentRequest payment1 = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        CreatePaymentRequest payment2 = CreatePaymentRequest.builder()
                .invoiceId(savedInvoice.getId())
                .amount(new BigDecimal("300000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        String response1 = mockMvc.perform(post("/api/v1/payments")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payment1)))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/v1/payments")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payment2)));

        Long payment1Id = objectMapper.readTree(response1).get("id").asLong();

        // Verify total paid = 800k
        Invoice afterPayments = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        assertThat(afterPayments.getAmountPaid()).isEqualByComparingTo(new BigDecimal("800000.00"));

        // Act - Refund first payment (500k)
        mockMvc.perform(post("/api/v1/payments/" + payment1Id + "/refund")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());

        // Assert - Invoice amountPaid decreased: 800k - 500k = 300k
        Invoice afterRefund = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        assertThat(afterRefund.getAmountPaid()).isEqualByComparingTo(new BigDecimal("300000.00"));
        assertThat(afterRefund.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
    }
}
