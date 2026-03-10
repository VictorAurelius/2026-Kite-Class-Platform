package com.kitehub.subscription.controller;

import com.kitehub.subscription.dto.CreatePaymentRequest;
import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for payment operations.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/platform/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a new payment.
     *
     * @param request Create payment request
     * @return Created payment response
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
        @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get payment by ID.
     *
     * @param id Payment UUID
     * @return Payment response
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments for subscription.
     *
     * @param subscriptionId Subscription UUID
     * @return List of payment responses
     */
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsBySubscription(@PathVariable UUID subscriptionId) {
        List<PaymentResponse> responses = paymentService.getPaymentsBySubscription(subscriptionId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get QR code URL for payment.
     *
     * @param id Payment UUID
     * @return QR code URL
     */
    @GetMapping("/{id}/qr-code")
    public ResponseEntity<Map<String, String>> getQRCode(@PathVariable UUID id) {
        String qrCodeUrl = paymentService.getQRCode(id);
        return ResponseEntity.ok(Map.of("qrCodeUrl", qrCodeUrl));
    }
}
