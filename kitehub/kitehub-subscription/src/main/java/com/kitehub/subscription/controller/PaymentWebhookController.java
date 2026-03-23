package com.kitehub.subscription.controller;

import com.kitehub.subscription.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

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
@Tag(name = "Payment Webhooks", description = "Payment notification webhooks from VietQR/Bank")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @Value("${webhook.payment.secret:}")
    private String webhookSecret;

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
     *   "signature": "abc123..." // HMAC-SHA256 signature for verification
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

            // Verify webhook signature to ensure it's from trusted source
            String signature = (String) payload.get("signature");
            if (signature == null || signature.isEmpty()) {
                log.warn("Missing webhook signature");
                return ResponseEntity.status(401).body(Map.of("error", "Missing signature"));
            }

            if (!verifyWebhookSignature(payload, signature)) {
                log.warn("Invalid webhook signature");
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
     * Verify webhook signature using HMAC-SHA256.
     *
     * The signature is computed by:
     * 1. Sorting payload keys alphabetically (excluding 'signature' field)
     * 2. Concatenating key=value pairs with '&' separator
     * 3. Computing HMAC-SHA256 with shared secret
     * 4. Hex-encoding the result
     *
     * @param payload Webhook payload
     * @param receivedSignature Signature from webhook
     * @return true if signature is valid
     */
    private boolean verifyWebhookSignature(Map<String, Object> payload, String receivedSignature) {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.error("Webhook secret not configured - rejecting webhook");
            return false;
        }

        try {
            // Create sorted map excluding signature field
            TreeMap<String, Object> sortedPayload = new TreeMap<>();
            payload.entrySet().stream()
                .filter(entry -> !"signature".equals(entry.getKey()))
                .forEach(entry -> sortedPayload.put(entry.getKey(), entry.getValue()));

            // Build payload string: key1=value1&key2=value2&...
            StringBuilder payloadString = new StringBuilder();
            sortedPayload.forEach((key, value) -> {
                if (payloadString.length() > 0) {
                    payloadString.append("&");
                }
                payloadString.append(key).append("=").append(value);
            });

            // Compute HMAC-SHA256
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(payloadString.toString().getBytes(StandardCharsets.UTF_8));

            // Hex encode
            String computedSignature = HexFormat.of().formatHex(hash);

            // Constant-time comparison to prevent timing attacks
            boolean isValid = constantTimeEquals(computedSignature, receivedSignature);

            if (!isValid) {
                log.warn("Signature mismatch. Expected: {}, Received: {}", computedSignature, receivedSignature);
            }

            return isValid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to verify webhook signature", e);
            return false;
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     *
     * @param a First string
     * @param b Second string
     * @return true if strings are equal
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
