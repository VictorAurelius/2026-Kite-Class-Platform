package com.kiteclass.core.module.payment.service;

import com.kiteclass.core.module.payment.dto.CreateInstallmentPaymentRequest;
import com.kiteclass.core.module.payment.dto.CreatePaymentRequest;
import com.kiteclass.core.module.payment.dto.PaymentResponse;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for payment operations.
 *
 * @since 1.0.0
 */
public interface PaymentService {

    /**
     * UC-1: Create payment for invoice (full or partial).
     *
     * @param request create payment request
     * @param userId user ID (UUID, X-User-Id) creating payment — GAP-795
     * @return payment response with payment URL (if online payment)
     */
    @Valid PaymentResponse createPayment(
        @Valid CreatePaymentRequest request, UUID userId);

    /**
     * UC-2: Create payment for installment.
     *
     * @param request create installment payment request
     * @param userId user ID (UUID, X-User-Id) creating payment — GAP-795
     * @return payment response with payment URL (if online payment)
     */
    @Valid PaymentResponse createInstallmentPayment(
        @Valid CreateInstallmentPaymentRequest request, UUID userId);

    /**
     * UC-3: Get payment by ID.
     *
     * @param id payment ID
     * @return payment response
     */
    PaymentResponse getPaymentById(Long id);

    /**
     * UC-4: Get payments by invoice.
     *
     * @param invoiceId invoice ID
     * @return list of payment responses
     */
    List<PaymentResponse> getPaymentsByInvoice(Long invoiceId);

    /**
     * UC-5: Process webhook callback from gateway.
     *
     * @param gateway payment gateway (VNPAY, MOMO, ZALOPAY)
     * @param params webhook parameters from gateway
     */
    void processWebhookCallback(PaymentMethod gateway, Map<String, String> params);

    /**
     * UC-6: Query payment status from gateway.
     *
     * @param paymentId payment ID
     * @return current payment status from gateway
     */
    PaymentStatus queryPaymentStatus(Long paymentId);

    /**
     * UC-7: Cancel pending payment.
     *
     * @param paymentId payment ID
     */
    void cancelPayment(Long paymentId);

    /**
     * UC-8: Process refund for completed payment.
     *
     * @param paymentId payment ID
     */
    void processRefund(Long paymentId);

    /**
     * UC-9: Get pending payments (for user dashboard).
     *
     * @param pageable pagination parameters
     * @return page of pending payment responses
     */
    Page<PaymentResponse> getPendingPayments(Pageable pageable);

    /**
     * UC-10: Expire old pending payments (scheduled job).
     * Runs every 10 minutes to clean up expired payments.
     */
    @Scheduled(cron = "0 */10 * * * *")
    void expireOldPayments();
}
