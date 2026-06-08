package com.kitehub.subscription.controller;

import com.kitehub.subscription.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * REST controller for SePay payment webhook notifications (Wave flow-kh3-2).
 *
 * <p>SePay (https://sepay.vn) calls this endpoint when a bank transfer credits
 * the merchant account. Authentication is via the {@code Authorization: Apikey
 * <key>} header (NOT a JWT, NOT an HMAC body signature). The payload carries the
 * SePay transaction shape; the {@code txnRef} (KH3SUB&lt;8 hex&gt;) is embedded in
 * the transfer {@code description} and used for exact-match payment lookup.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/webhooks/payment")
@RequiredArgsConstructor
@Tag(name = "Payment Webhooks", description = "SePay payment notification webhook")
public class PaymentWebhookController {

    private static final String APIKEY_PREFIX = "Apikey ";

    private final PaymentService paymentService;

    @Value("${kitehub.payment.sepay.api-key:}")
    private String sepayApiKey;

    /**
     * Receive a SePay payment notification.
     *
     * <p>SePay payload example:</p>
     * <pre>
     * {
     *   "id": "92704",
     *   "gateway": "Vietcombank",
     *   "transactionDate": "2026-06-04 15:30:00",
     *   "accountNumber": "1234567890",
     *   "transferType": "in",
     *   "transferAmount": 10000,
     *   "description": "KH3SUB1A2B3C4D",
     *   "referenceCode": "FT2406..."
     * }
     * </pre>
     *
     * @param authHeader {@code Authorization: Apikey <key>} header
     * @param payload    SePay transaction payload
     * @return HTTP 200 for valid Apikey (success/ignored/idempotent), 401 for a
     *         missing/wrong Apikey, 400 for an unmatched txnRef (orphan notify)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> handlePaymentWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload) {
        log.info("Received SePay webhook: {}", payload);

        if (!verifyApiKey(authHeader)) {
            log.warn("SePay webhook rejected — missing or invalid Apikey");
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Invalid API key"));
        }

        try {
            String transferType = (String) payload.get("transferType");
            if (!"in".equalsIgnoreCase(transferType)) {
                log.info("Ignoring non-incoming SePay transfer (type={})", transferType);
                // SePay ACK contract: body MUST contain {"success": true} or SePay
                // marks the delivery failed + retries (up to 7×). Ignored = received
                // OK, nothing to do → acknowledge so SePay stops retrying.
                return ResponseEntity.ok(Map.of("success", true, "status", "ignored"));
            }

            String sepayId = payload.get("id") == null ? null : String.valueOf(payload.get("id"));
            long transferAmount = ((Number) payload.get("transferAmount")).longValue();
            String description = (String) payload.get("description");

            paymentService.processSepayWebhook(sepayId, transferAmount, description);
            // SePay ACK contract (verified 2026-06-08 Test Mode): the 200 body MUST
            // contain {"success": true}, otherwise SePay marks the webhook delivery
            // failed ("Response không đúng quy cách") + retries up to 7× even though
            // the payment was processed. GAP-1063.
            return ResponseEntity.ok(Map.of("success", true, "status", "success"));

        } catch (IllegalArgumentException e) {
            // Orphan notify — txnRef present but no matching payment.
            log.warn("SePay webhook unprocessable: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to process SePay webhook", e);
            return ResponseEntity.status(400).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Verify the {@code Authorization: Apikey <key>} header against the configured
     * SePay api-key using a constant-time comparison.
     *
     * @param authHeader raw Authorization header value
     * @return true when the header carries the correct Apikey
     */
    private boolean verifyApiKey(String authHeader) {
        if (sepayApiKey == null || sepayApiKey.isEmpty()) {
            log.error("SePay api-key not configured — rejecting webhook");
            return false;
        }
        if (authHeader == null || !authHeader.startsWith(APIKEY_PREFIX)) {
            return false;
        }
        String provided = authHeader.substring(APIKEY_PREFIX.length()).trim();
        return MessageDigest.isEqual(
            provided.getBytes(StandardCharsets.UTF_8),
            sepayApiKey.getBytes(StandardCharsets.UTF_8));
    }
}
