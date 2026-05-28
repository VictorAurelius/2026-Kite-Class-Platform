package com.kiteclass.core.module.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.payment.dto.CreatePaymentRequest;
import com.kiteclass.core.module.payment.dto.PaymentResponse;
import com.kiteclass.core.module.payment.dto.gateway.PaymentGatewayRequest;
import com.kiteclass.core.module.payment.dto.gateway.PaymentInitiationResponse;
import com.kiteclass.core.module.payment.entity.Payment;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import com.kiteclass.core.module.payment.event.PaymentCompletedEvent;
import com.kiteclass.core.module.payment.gateway.PaymentGatewayClient;
import com.kiteclass.core.module.payment.mapper.PaymentMapper;
import com.kiteclass.core.module.payment.repository.PaymentRepository;
import com.kiteclass.core.module.payment.repository.PaymentWebhookLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentServiceImpl}.
 *
 * @author KiteClass Team
 * @since 2.8.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentWebhookLogRepository webhookLogRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentNumberGenerator paymentNumberGenerator;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private PaymentGatewayClient vnpayGatewayClient;

    @Mock
    private PaymentGatewayClient momoGatewayClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    // GAP-795: payment actor is the X-User-Id UUID, not a numeric id.
    private static final UUID ACTOR_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Invoice testInvoice;
    private CreatePaymentRequest createRequest;
    private Payment testPayment;
    private PaymentResponse testResponse;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();

        // Setup Invoice
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
                .status(InvoiceStatus.SENT)
                .build();
        testInvoice.setId(1L);
        testInvoice.setInstanceId(tenantId);

        // Setup CreatePaymentRequest
        createRequest = CreatePaymentRequest.builder()
                .invoiceId(1L)
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.VNPAY)
                .ipAddress("192.168.1.1")
                .build();

        // Setup Payment
        testPayment = Payment.builder()
                .paymentNumber("PAY-2026-000001")
                .transactionId("TXN1234567890abcdefgh")
                .invoiceId(1L)
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentUrl("https://vnpay.vn/pay/12345")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        testPayment.setId(1L);
        testPayment.setInstanceId(tenantId);

        // Setup PaymentResponse
        testResponse = PaymentResponse.builder()
                .id(1L)
                .paymentNumber("PAY-2026-000001")
                .transactionId("TXN1234567890abcdefgh")
                .invoiceId(1L)
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentUrl("https://vnpay.vn/pay/12345")
                .build();

        // Setup gateway clients map
        Map<PaymentMethod, PaymentGatewayClient> gatewayClients = new HashMap<>();
        gatewayClients.put(PaymentMethod.VNPAY, vnpayGatewayClient);
        gatewayClients.put(PaymentMethod.MOMO, momoGatewayClient);
        ReflectionTestUtils.setField(paymentService, "gatewayClients", gatewayClients);
        ReflectionTestUtils.setField(paymentService, "returnUrl", "http://localhost:3000/payment/return");
        ReflectionTestUtils.setField(paymentService, "notifyUrl", "http://localhost:8081/api/v1/payments/webhook");
    }

    @Test
    @DisplayName("Should not throw when notify URL is properly configured")
    void shouldInitSuccessfullyWithConfiguredNotifyUrl() {
        // Arrange
        ReflectionTestUtils.setField(paymentService, "notifyUrl",
            "https://api.myschool.kiteclass.com/api/v1/payments/webhook");
        when(applicationContext.getBean("vnpayGatewayClient", PaymentGatewayClient.class))
                .thenReturn(vnpayGatewayClient);
        when(applicationContext.getBean("momoGatewayClient", PaymentGatewayClient.class))
                .thenReturn(momoGatewayClient);
        when(applicationContext.getBean("zalopayGatewayClient", PaymentGatewayClient.class))
                .thenReturn(momoGatewayClient);

        // Act & Assert - should not throw
        paymentService.init();
    }

    @Test
    @DisplayName("Should create payment successfully with VNPay gateway")
    void shouldCreatePaymentSuccessfully() {
        // Arrange
        when(invoiceRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testInvoice));
        when(paymentNumberGenerator.generate(tenantId))
                .thenReturn("PAY-2026-000001");

        PaymentInitiationResponse gatewayResponse = PaymentInitiationResponse.builder()
                .paymentUrl("https://vnpay.vn/pay/12345")
                .qrCodeUrl("https://vnpay.vn/qr/12345")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        when(vnpayGatewayClient.initiatePayment(any(PaymentGatewayRequest.class)))
                .thenReturn(gatewayResponse);

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(testPayment);
        when(paymentMapper.toResponse(any(Payment.class)))
                .thenReturn(testResponse);

        // Act
        PaymentResponse result = paymentService.createPayment(createRequest, ACTOR_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPaymentUrl()).isNotNull();
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.VNPAY);

        verify(paymentRepository).save(argThat(payment ->
                payment.getPaymentMethod() == PaymentMethod.VNPAY &&
                payment.getPaymentStatus() == PaymentStatus.PENDING &&
                payment.getPaymentUrl() != null
        ));
        // Note: Event publishing tested in integration tests
        // verify(eventPublisher).publishEvent(any(PaymentCreatedEvent.class));
    }

    @Test
    @DisplayName("Should create offline payment and complete immediately")
    void shouldCreateOfflinePaymentAndCompleteImmediately() {
        // Arrange
        createRequest = CreatePaymentRequest.builder()
                .invoiceId(1L)
                .amount(new BigDecimal("500000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        testPayment.setPaymentMethod(PaymentMethod.CASH);
        testPayment.setPaymentStatus(PaymentStatus.COMPLETED);

        when(invoiceRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testInvoice));
        when(paymentNumberGenerator.generate(tenantId))
                .thenReturn("PAY-2026-000001");
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(testPayment);
        when(paymentMapper.toResponse(any(Payment.class)))
                .thenReturn(testResponse);

        // Act
        PaymentResponse result = paymentService.createPayment(createRequest, ACTOR_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        verify(paymentRepository).save(argThat(payment ->
                payment.getPaymentMethod() == PaymentMethod.CASH
        ));
        // Note: Event publishing tested in integration tests
        // verify(eventPublisher).publishEvent(any(PaymentCreatedEvent.class));
        // verify(eventPublisher).publishEvent(any(PaymentCompletedEvent.class));
        verify(vnpayGatewayClient, never()).initiatePayment(any());
    }

    @Test
    @DisplayName("Should throw exception when invoice not found")
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        when(invoiceRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(createRequest, ACTOR_USER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("INVOICE_NOT_FOUND"));

        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should throw exception when invoice already paid")
    void shouldThrowExceptionWhenInvoiceAlreadyPaid() {
        // Arrange
        testInvoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testInvoice));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(createRequest, ACTOR_USER_ID))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("INVOICE_ALREADY_PAID"));

        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should throw exception when payment amount exceeds balance due")
    void shouldThrowExceptionWhenAmountExceedsBalance() {
        // Arrange
        createRequest = CreatePaymentRequest.builder()
                .invoiceId(1L)
                .amount(new BigDecimal("1500000.00")) // Exceeds balance
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        when(invoiceRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testInvoice));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(createRequest, ACTOR_USER_ID))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("PAYMENT_AMOUNT_EXCEEDS_BALANCE"));

        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should process webhook callback successfully")
    void shouldProcessWebhookCallbackSuccessfully() throws Exception {
        // Arrange
        Map<String, String> webhookParams = new HashMap<>();
        webhookParams.put("vnp_TxnRef", "TXN1234567890abcdefgh");
        webhookParams.put("vnp_ResponseCode", "00");
        webhookParams.put("vnp_TransactionNo", "VNP123456");
        webhookParams.put("vnp_SecureHash", "valid_signature");

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"vnp_TxnRef\":\"TXN1234567890abcdefgh\"}");
        when(vnpayGatewayClient.verifySignature(any(), anyString()))
                .thenReturn(true);
        when(paymentRepository.findByTransactionIdAndDeletedFalse("TXN1234567890abcdefgh"))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(testPayment);
        when(webhookLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        paymentService.processWebhookCallback(PaymentMethod.VNPAY, webhookParams);

        // Assert
        verify(vnpayGatewayClient).verifySignature(eq(webhookParams), eq("valid_signature"));
        verify(paymentRepository).save(argThat(payment ->
                payment.getPaymentStatus() == PaymentStatus.COMPLETED &&
                payment.getGatewayTransactionId() != null
        ));
        // Note: Event publishing tested in integration tests
        // verify(eventPublisher).publishEvent(any(PaymentCompletedEvent.class));
        verify(webhookLogRepository).save(argThat(log ->
                log.getSignatureValid() != null &&
                log.getSignatureValid() &&
                log.getProcessed()
        ));
    }

    @Test
    @DisplayName("Should reject webhook with invalid signature")
    void shouldRejectWebhookWithInvalidSignature() throws Exception {
        // Arrange
        Map<String, String> webhookParams = new HashMap<>();
        webhookParams.put("vnp_TxnRef", "TXN1234567890abcdefgh");
        webhookParams.put("vnp_SecureHash", "invalid_signature");

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"vnp_TxnRef\":\"TXN1234567890abcdefgh\"}");
        when(vnpayGatewayClient.verifySignature(any(), anyString()))
                .thenReturn(false);
        when(webhookLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        paymentService.processWebhookCallback(PaymentMethod.VNPAY, webhookParams);

        // Assert
        verify(vnpayGatewayClient).verifySignature(eq(webhookParams), eq("invalid_signature"));
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(webhookLogRepository).save(argThat(log ->
                log.getSignatureValid() != null &&
                !log.getSignatureValid() &&
                !log.getProcessed()
        ));
    }

    @Test
    @DisplayName("Should handle idempotency - skip already completed payment")
    void shouldHandleIdempotencyForCompletedPayment() throws Exception {
        // Arrange
        testPayment.setPaymentStatus(PaymentStatus.COMPLETED);
        testPayment.setCompletedAt(LocalDateTime.now());

        Map<String, String> webhookParams = new HashMap<>();
        webhookParams.put("vnp_TxnRef", "TXN1234567890abcdefgh");
        webhookParams.put("vnp_ResponseCode", "00");
        webhookParams.put("vnp_SecureHash", "valid_signature");

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"vnp_TxnRef\":\"TXN1234567890abcdefgh\"}");
        when(vnpayGatewayClient.verifySignature(any(), anyString()))
                .thenReturn(true);
        when(paymentRepository.findByTransactionIdAndDeletedFalse("TXN1234567890abcdefgh"))
                .thenReturn(Optional.of(testPayment));
        when(webhookLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        paymentService.processWebhookCallback(PaymentMethod.VNPAY, webhookParams);

        // Assert
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(webhookLogRepository).save(argThat(log ->
                log.getProcessed()
        ));
    }

    @Test
    @DisplayName("Should handle failed payment from gateway")
    void shouldHandleFailedPaymentFromGateway() throws Exception {
        // Arrange
        Map<String, String> webhookParams = new HashMap<>();
        webhookParams.put("vnp_TxnRef", "TXN1234567890abcdefgh");
        webhookParams.put("vnp_ResponseCode", "07"); // Transaction error
        webhookParams.put("vnp_SecureHash", "valid_signature");

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"vnp_TxnRef\":\"TXN1234567890abcdefgh\"}");
        when(vnpayGatewayClient.verifySignature(any(), anyString()))
                .thenReturn(true);
        when(paymentRepository.findByTransactionIdAndDeletedFalse("TXN1234567890abcdefgh"))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(testPayment);
        when(webhookLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        paymentService.processWebhookCallback(PaymentMethod.VNPAY, webhookParams);

        // Assert
        verify(paymentRepository).save(argThat(payment ->
                payment.getPaymentStatus() == PaymentStatus.FAILED &&
                payment.getFailureReason() != null
        ));
        verify(eventPublisher, never()).publishEvent(any(PaymentCompletedEvent.class));
    }

    @Test
    @DisplayName("Should cancel pending payment successfully")
    void shouldCancelPendingPaymentSuccessfully() {
        // Arrange
        when(paymentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(testPayment);

        // Act
        paymentService.cancelPayment(1L);

        // Assert
        verify(paymentRepository).save(argThat(payment ->
                payment.getPaymentStatus() == PaymentStatus.FAILED &&
                payment.getFailureReason() != null &&
                payment.getFailureReason().contains("Cancelled by user")
        ));
    }

    @Test
    @DisplayName("Should throw exception when payment not found for cancellation")
    void shouldThrowExceptionWhenPaymentNotFoundForCancellation() {
        // Arrange
        when(paymentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.cancelPayment(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("PAYMENT_NOT_FOUND"));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should process refund successfully")
    void shouldProcessRefundSuccessfully() {
        // Arrange
        testPayment.setPaymentStatus(PaymentStatus.COMPLETED);
        testPayment.setCompletedAt(LocalDateTime.now());
        testPayment.setGatewayTransactionId("VNP123456");

        when(paymentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(testPayment);

        // Act
        paymentService.processRefund(1L);

        // Assert
        verify(vnpayGatewayClient).processRefund(
                eq("TXN1234567890abcdefgh"),
                eq(new BigDecimal("500000.00"))
        );
        verify(paymentRepository).save(argThat(payment ->
                payment.getPaymentStatus() == PaymentStatus.REFUNDED &&
                payment.getRefundedAt() != null
        ));
    }

    @Test
    @DisplayName("Should query payment status from gateway")
    void shouldQueryPaymentStatusFromGateway() {
        // Arrange
        testPayment.setPaymentMethod(PaymentMethod.VNPAY);
        when(paymentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testPayment));
        when(vnpayGatewayClient.queryPaymentStatus("TXN1234567890abcdefgh"))
                .thenReturn(PaymentStatus.COMPLETED);

        // Act
        PaymentStatus status = paymentService.queryPaymentStatus(1L);

        // Assert
        assertThat(status).isEqualTo(PaymentStatus.COMPLETED);
        verify(vnpayGatewayClient).queryPaymentStatus("TXN1234567890abcdefgh");
    }

    @Test
    @DisplayName("Should use configured notify URL in gateway request")
    void shouldUseConfiguredNotifyUrlInGatewayRequest() {
        // Arrange
        String customNotifyUrl = "https://api.example.com/api/v1/payments/webhook";
        ReflectionTestUtils.setField(paymentService, "notifyUrl", customNotifyUrl);

        when(invoiceRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testInvoice));
        when(paymentNumberGenerator.generate(tenantId))
                .thenReturn("PAY-2026-000001");

        PaymentInitiationResponse gatewayResponse = PaymentInitiationResponse.builder()
                .paymentUrl("https://vnpay.vn/pay/12345")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        when(vnpayGatewayClient.initiatePayment(any(PaymentGatewayRequest.class)))
                .thenReturn(gatewayResponse);

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(testPayment);
        when(paymentMapper.toResponse(any(Payment.class)))
                .thenReturn(testResponse);

        // Act
        paymentService.createPayment(createRequest, ACTOR_USER_ID);

        // Assert - verify the gateway request uses the configured URL
        verify(vnpayGatewayClient).initiatePayment(argThat(request ->
                request.getNotifyUrl().startsWith(customNotifyUrl)
        ));
    }

    @Test
    @DisplayName("Should return current status for offline payment")
    void shouldReturnCurrentStatusForOfflinePayment() {
        // Arrange
        testPayment.setPaymentMethod(PaymentMethod.CASH);
        testPayment.setPaymentStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testPayment));

        // Act
        PaymentStatus status = paymentService.queryPaymentStatus(1L);

        // Assert
        assertThat(status).isEqualTo(PaymentStatus.COMPLETED);
        verify(vnpayGatewayClient, never()).queryPaymentStatus(anyString());
    }
}
