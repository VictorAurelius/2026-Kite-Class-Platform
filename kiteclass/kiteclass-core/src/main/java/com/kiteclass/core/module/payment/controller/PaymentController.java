package com.kiteclass.core.module.payment.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.payment.dto.CreateInstallmentPaymentRequest;
import com.kiteclass.core.module.payment.dto.CreatePaymentRequest;
import com.kiteclass.core.module.payment.dto.PaymentResponse;
import com.kiteclass.core.module.payment.dto.PaymentStatusResponse;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import com.kiteclass.core.module.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for payment operations.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create payment for invoice.
     *
     * @param request create payment request
     * @param userDetails authenticated user
     * @return payment response with payment URL (if online payment)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
        @Valid @RequestBody CreatePaymentRequest request) {

        log.info("Creating payment for invoice {} (method: {})",
            request.getInvoiceId(), request.getPaymentMethod());

        // Note: User ID will be extracted from JWT at Gateway level
        PaymentResponse response = paymentService.createPayment(request, 1L);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Create payment for installment.
     *
     * @param request create installment payment request
     * @param userDetails authenticated user
     * @return payment response with payment URL (if online payment)
     */
    @PostMapping("/installments")
    public ResponseEntity<ApiResponse<PaymentResponse>> createInstallmentPayment(
        @Valid @RequestBody CreateInstallmentPaymentRequest request) {

        log.info("Creating installment payment for installment {} (method: {})",
            request.getInstallmentId(), request.getPaymentMethod());

        // Note: User ID will be extracted from JWT at Gateway level
        PaymentResponse response = paymentService.createInstallmentPayment(request, 1L);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Get payment by ID.
     *
     * @param id payment ID
     * @return payment response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        log.debug("Getting payment by ID: {}", id);

        PaymentResponse response = paymentService.getPaymentById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all payments for an invoice.
     *
     * @param invoiceId invoice ID
     * @return list of payment responses
     */
    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByInvoice(@PathVariable Long invoiceId) {
        log.debug("Getting payments for invoice: {}", invoiceId);

        List<PaymentResponse> responses = paymentService.getPaymentsByInvoice(invoiceId);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get pending payments with pagination.
     *
     * @param pageable pagination parameters
     * @return page of pending payments
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPendingPayments(Pageable pageable) {
        log.debug("Getting pending payments (page: {}, size: {})",
            pageable.getPageNumber(), pageable.getPageSize());

        Page<PaymentResponse> responses = paymentService.getPendingPayments(pageable);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Cancel a pending payment.
     *
     * @param id payment ID
     * @return no content
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelPayment(@PathVariable Long id) {
        log.info("Cancelling payment: {}", id);

        paymentService.cancelPayment(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Process refund for a completed payment.
     *
     * @param id payment ID
     * @return no content
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<Void> processRefund(@PathVariable Long id) {
        log.info("Processing refund for payment: {}", id);

        paymentService.processRefund(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Query payment status from gateway.
     *
     * @param id payment ID
     * @return payment status response
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> queryPaymentStatus(@PathVariable Long id) {
        log.debug("Querying payment status for: {}", id);

        PaymentStatus status = paymentService.queryPaymentStatus(id);

        return ResponseEntity.ok(ApiResponse.success(new PaymentStatusResponse(status)));
    }
}
