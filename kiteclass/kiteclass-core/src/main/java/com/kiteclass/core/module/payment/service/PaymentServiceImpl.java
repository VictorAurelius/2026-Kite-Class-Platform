package com.kiteclass.core.module.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.payment.dto.CreateInstallmentPaymentRequest;
import com.kiteclass.core.module.payment.dto.CreatePaymentRequest;
import com.kiteclass.core.module.payment.dto.PaymentResponse;
import com.kiteclass.core.module.payment.dto.gateway.PaymentGatewayRequest;
import com.kiteclass.core.module.payment.dto.gateway.PaymentInitiationResponse;
import com.kiteclass.core.module.payment.entity.Payment;
import com.kiteclass.core.module.payment.entity.PaymentWebhookLog;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import com.kiteclass.core.module.payment.event.PaymentCompletedEvent;
import com.kiteclass.core.module.payment.event.PaymentCreatedEvent;
import com.kiteclass.core.module.payment.event.PaymentRefundedEvent;
import com.kiteclass.core.module.payment.gateway.PaymentGatewayClient;
import com.kiteclass.core.module.payment.mapper.PaymentMapper;
import com.kiteclass.core.module.payment.repository.PaymentRepository;
import com.kiteclass.core.module.payment.repository.PaymentWebhookLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Implementation of PaymentService.
 *
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class PaymentServiceImpl implements PaymentService {

    private static final String DEFAULT_NOTIFY_URL = "http://localhost:8081/api/v1/payments/webhook";

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookLogRepository webhookLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentNumberGenerator paymentNumberGenerator;
    private final PaymentMapper paymentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    @Value("${payment.return-url:http://localhost:3000/payment/return}")
    private String returnUrl;

    @Value("${payment.notify-url:" + DEFAULT_NOTIFY_URL + "}")
    private String notifyUrl;

    private Map<PaymentMethod, PaymentGatewayClient> gatewayClients;

    @PostConstruct
    public void init() {
        gatewayClients = new HashMap<>();
        gatewayClients.put(PaymentMethod.VNPAY,
            applicationContext.getBean("vnpayGatewayClient", PaymentGatewayClient.class));
        gatewayClients.put(PaymentMethod.MOMO,
            applicationContext.getBean("momoGatewayClient", PaymentGatewayClient.class));
        gatewayClients.put(PaymentMethod.ZALOPAY,
            applicationContext.getBean("zalopayGatewayClient", PaymentGatewayClient.class));

        // Fail-safe: warn if notify URL is not explicitly configured or uses default
        if (notifyUrl == null || notifyUrl.isBlank()) {
            log.warn("SECURITY: payment.notify-url is not configured! "
                + "Payment webhooks will not work correctly. "
                + "Set PAYMENT_NOTIFY_URL environment variable for production.");
        } else if (DEFAULT_NOTIFY_URL.equals(notifyUrl)) {
            log.warn("SECURITY: payment.notify-url is using default localhost value '{}'. "
                + "This MUST be changed for production deployment. "
                + "Set PAYMENT_NOTIFY_URL environment variable.", notifyUrl);
        } else {
            log.info("Payment notify URL configured: {}", notifyUrl);
        }
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, UUID userId) {
        // 1. Validate invoice exists and can accept payment
        Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(request.getInvoiceId())
            .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ValidationException("INVOICE_ALREADY_PAID", (Object) null);
        }

        // 2. Validate amount <= balance due
        BigDecimal balanceDue = invoice.getBalanceDue();
        if (request.getAmount().compareTo(balanceDue) > 0) {
            throw new ValidationException("PAYMENT_AMOUNT_EXCEEDS_BALANCE",
                request.getAmount(), balanceDue);
        }

        // 3. Generate unique transaction ID (idempotency)
        String transactionId = generateTransactionId();

        // 4. Create payment record (PENDING status)
        String paymentNumber = paymentNumberGenerator.generate(invoice.getInstanceId());

        Payment payment = Payment.builder()
            .paymentNumber(paymentNumber)
            .transactionId(transactionId)
            .invoiceId(invoice.getId())
            .amount(request.getAmount())
            .paymentMethod(request.getPaymentMethod())
            .paymentStatus(PaymentStatus.PENDING)
            .createdBy(userId)
            .build();
        payment.setInstanceId(invoice.getInstanceId());

        // 5. For online payment methods, initiate gateway payment
        if (request.getPaymentMethod().isOnline()) {
            PaymentGatewayClient gatewayClient = gatewayClients.get(request.getPaymentMethod());

            PaymentGatewayRequest gatewayRequest = PaymentGatewayRequest.builder()
                .transactionId(transactionId)
                .amount(request.getAmount())
                .orderInfo("Thanh toán hóa đơn " + invoice.getInvoiceNumber())
                .returnUrl(returnUrl)
                .notifyUrl(notifyUrl + "/" + request.getPaymentMethod().name().toLowerCase())
                .ipAddress(request.getIpAddress() != null ? request.getIpAddress() : "127.0.0.1")
                .build();

            PaymentInitiationResponse gatewayResponse = gatewayClient.initiatePayment(gatewayRequest);

            payment.setPaymentUrl(gatewayResponse.getPaymentUrl());
            payment.setQrCodeUrl(gatewayResponse.getQrCodeUrl());
            payment.setExpiresAt(gatewayResponse.getExpiresAt());
        } else {
            // For offline payments (CASH, BANK_TRANSFER), mark as completed immediately
            payment.complete(null, null);
        }

        // 6. Save payment
        Payment savedPayment = paymentRepository.save(payment);

        // 7. Publish PaymentCreatedEvent
        eventPublisher.publishEvent(new PaymentCreatedEvent(this, savedPayment));

        // 8. If offline payment, publish PaymentCompletedEvent immediately
        if (request.getPaymentMethod().isOffline()) {
            eventPublisher.publishEvent(new PaymentCompletedEvent(this, savedPayment));
        }

        log.info("Created payment {} for invoice {} (amount: {}, method: {})",
            savedPayment.getPaymentNumber(), invoice.getInvoiceNumber(),
            request.getAmount(), request.getPaymentMethod());

        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse createInstallmentPayment(CreateInstallmentPaymentRequest request, UUID userId) {
        // To be implemented when Installment module is ready
        log.warn("Installment payment not implemented yet");
        throw new UnsupportedOperationException("Installment payment not implemented yet");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new EntityNotFoundException("PAYMENT_NOT_FOUND", (Object) id));

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByInvoice(Long invoiceId) {
        List<Payment> payments = paymentRepository.findByInvoiceIdAndDeletedFalse(invoiceId);

        return payments.stream()
            .map(paymentMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public void processWebhookCallback(PaymentMethod gateway, Map<String, String> params) {
        UUID tenantId = null;
        try {
            // 1. Log webhook for audit
            String requestPayloadJson;
            try {
                requestPayloadJson = objectMapper.writeValueAsString(params);
            } catch (Exception e) {
                requestPayloadJson = params.toString();
            }

            PaymentWebhookLog webhookLog = PaymentWebhookLog.builder()
                .gateway(gateway.name())
                .requestPayload(requestPayloadJson)
                .signature(params.getOrDefault("vnp_SecureHash",
                    params.getOrDefault("signature", "")))
                .build();

            // 2. Verify signature (CRITICAL for security)
            PaymentGatewayClient gatewayClient = gatewayClients.get(gateway);
            String signature = params.getOrDefault("vnp_SecureHash",
                params.getOrDefault("signature", ""));
            boolean signatureValid = gatewayClient.verifySignature(params, signature);

            webhookLog.setSignatureValid(signatureValid);

            if (!signatureValid) {
                webhookLog.setErrorMessage("Invalid signature");
                webhookLog.setProcessed(false);
                webhookLogRepository.save(webhookLog);
                log.error("Invalid webhook signature from {}: {}", gateway, params);
                return;
            }

            // 3. Find payment by transaction ID
            // Support multiple formats: VNPay (vnp_TxnRef), MoMo (orderId), Generic (transactionId)
            String transactionId = params.getOrDefault("vnp_TxnRef",
                params.getOrDefault("orderId",
                    params.getOrDefault("transactionId", "")));
            Payment payment = paymentRepository.findByTransactionIdAndDeletedFalse(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("PAYMENT_NOT_FOUND_BY_TRANSACTION", (Object) transactionId));

            webhookLog.setPaymentId(payment.getId());
            webhookLog.setInstanceId(payment.getInstanceId());
            tenantId = payment.getInstanceId();

            // 4. Check idempotency (already processed?)
            if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
                log.info("Payment {} already completed, skipping webhook", payment.getPaymentNumber());
                webhookLog.setProcessed(true);
                webhookLogRepository.save(webhookLog);
                return;
            }

            // 5. Update payment status based on gateway response
            // Support multiple formats: VNPay (vnp_ResponseCode), MoMo (resultCode), Generic (status)
            String responseCode = params.getOrDefault("vnp_ResponseCode",
                params.getOrDefault("resultCode",
                    params.getOrDefault("status", "")));

            if (isSuccessResponseCode(responseCode)) {
                // Success
                String gatewayTxnId = params.getOrDefault("vnp_TransactionNo",
                    params.getOrDefault("transId", ""));

                String gatewayResponseJson;
                try {
                    gatewayResponseJson = objectMapper.writeValueAsString(params);
                } catch (Exception e) {
                    gatewayResponseJson = params.toString();
                }

                payment.complete(gatewayTxnId, gatewayResponseJson);

                // 6. Publish PaymentCompletedEvent (will trigger invoice update)
                eventPublisher.publishEvent(new PaymentCompletedEvent(this, payment));

                log.info("Payment {} completed successfully (gateway: {})",
                    payment.getPaymentNumber(), gateway);
            } else {
                // Failure
                payment.fail("Gateway error: " + responseCode);
                log.warn("Payment {} failed with code: {} (gateway: {})",
                    payment.getPaymentNumber(), responseCode, gateway);
            }

            paymentRepository.save(payment);
            webhookLog.setProcessed(true);
            webhookLogRepository.save(webhookLog);

        } catch (Exception e) {
            log.error("Failed to process webhook callback from {}: {}",
                gateway, e.getMessage(), e);

            // Save error to webhook log
            PaymentWebhookLog errorLog = PaymentWebhookLog.builder()
                .gateway(gateway.name())
                .requestPayload(params.toString())
                .signatureValid(false)
                .processed(false)
                .errorMessage(e.getMessage())
                .instanceId(tenantId)
                .build();
            webhookLogRepository.save(errorLog);

            throw e;
        }
    }

    @Override
    @Transactional
    public PaymentStatus queryPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findByIdAndDeletedFalse(paymentId)
            .orElseThrow(() -> new EntityNotFoundException("PAYMENT_NOT_FOUND", (Object) paymentId));

        if (payment.getPaymentMethod().isOnline()) {
            PaymentGatewayClient gatewayClient = gatewayClients.get(payment.getPaymentMethod());
            return gatewayClient.queryPaymentStatus(payment.getTransactionId());
        }

        return payment.getPaymentStatus();
    }

    @Override
    @Transactional
    public void cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findByIdAndDeletedFalse(paymentId)
            .orElseThrow(() -> new EntityNotFoundException("PAYMENT_NOT_FOUND", (Object) paymentId));

        payment.cancel();
        paymentRepository.save(payment);

        log.info("Cancelled payment {}", payment.getPaymentNumber());
    }

    @Override
    @Transactional
    public void processRefund(Long paymentId) {
        Payment payment = paymentRepository.findByIdAndDeletedFalse(paymentId)
            .orElseThrow(() -> new EntityNotFoundException("PAYMENT_NOT_FOUND", (Object) paymentId));

        // 1. Call gateway refund API (for online payments)
        if (payment.getPaymentMethod().isOnline()) {
            PaymentGatewayClient gatewayClient = gatewayClients.get(payment.getPaymentMethod());
            gatewayClient.processRefund(payment.getTransactionId(), payment.getAmount());
        }

        // 2. Mark payment as refunded
        payment.refund();
        paymentRepository.save(payment);

        // 3. Publish PaymentRefundedEvent (triggers invoice amount update)
        eventPublisher.publishEvent(new PaymentRefundedEvent(this, payment));

        log.info("Processed refund for payment {} (amount: {})",
            payment.getPaymentNumber(), payment.getAmount());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPendingPayments(Pageable pageable) {
        Page<Payment> payments = paymentRepository.findByPaymentStatusAndDeletedFalse(
            PaymentStatus.PENDING, pageable);

        return payments.map(paymentMapper::toResponse);
    }

    @Override
    @Transactional
    public void expireOldPayments() {
        int expiredCount = paymentRepository.expirePendingPayments(
            LocalDateTime.now(), PaymentStatus.FAILED);

        if (expiredCount > 0) {
            log.info("Expired {} pending payments", expiredCount);
        }
    }

    /**
     * Generates unique transaction ID.
     * Pattern: TXN{timestamp}{random}
     *
     * @return transaction ID
     */
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Checks if gateway response code indicates success.
     * Supports VNPay ("00"), MoMo ("0"), and generic ("COMPLETED").
     */
    private boolean isSuccessResponseCode(String responseCode) {
        return "00".equals(responseCode)
            || "0".equals(responseCode)
            || "COMPLETED".equalsIgnoreCase(responseCode);
    }
}
