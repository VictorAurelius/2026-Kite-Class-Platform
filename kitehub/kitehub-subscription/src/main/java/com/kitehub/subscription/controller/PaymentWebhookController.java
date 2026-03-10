package com.kitehub.subscription.controller;

import com.kitehub.subscription.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for payment webhook notifications.
 * Receives payment confirmations from VietQR/Bank.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/webhooks/payment")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    /**
     * Receive payment notification from VietQR/Bank.
     * Called when customer completes bank transfer.
     *
     * Webhook payload example:
     * {
     *   "transactionId": "VCB123456789",
     *   "amount": 500000,
     *   "content": "KITECLASS ABC123DE",
     *   "bankCode": "VCB",
     *   "signature": "abc123..." // TODO: Verify signature
     * }
     *
     * @param payload Webhook payload from payment gateway
     * @return Success response
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> handlePaymentWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received payment webhook: {}", payload);

        try {
            // Extract payment information
            String transactionId = (String) payload.get("transactionId");
            Long amountVnd = ((Number) payload.get("amount")).longValue();
            String paymentContent = (String) payload.get("content");

            // TODO: Verify webhook signature to ensure it's from trusted source
            String signature = (String) payload.get("signature");
            if (!verifyWebhookSignature(payload, signature)) {
                log.warn("Invalid webhook signature: {}", signature);
                return ResponseEntity.status(401).body(Map.of("error", "Invalid signature"));
            }

            // Process payment
            paymentService.processPaymentWebhook(transactionId, amountVnd, paymentContent);

            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (Exception e) {
            log.error("Failed to process payment webhook", e);
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verify webhook signature.
     * TODO: Implement signature verification using shared secret.
     *
     * @param payload Webhook payload
     * @param signature Signature from webhook
     * @return true if signature is valid
     */
    private boolean verifyWebhookSignature(Map<String, Object> payload, String signature) {
        // TODO: Implement HMAC signature verification
        // For MVP: Skip signature verification (security risk - fix in production)
        log.warn("Webhook signature verification not implemented - accepting all webhooks (INSECURE)");
        return true;
    }
}
