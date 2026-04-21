package com.kitehub.subscription.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 verifier for the migration webhook (GAP-192 Phase 4b-i).
 *
 * <p>Verification model (per api-contract.md POST /webhooks/payment):
 * <ol>
 *   <li>Caller sends raw JSON body + a header {@code X-Signature: hex(hmac_sha256(secret, body))}.</li>
 *   <li>This verifier re-computes the MAC over the exact bytes received and compares in
 *       constant time.</li>
 * </ol>
 *
 * <p>The verifier intentionally operates on the raw body (not on a parsed map) so that
 * JSON whitespace / key ordering produced by the gateway does not break the signature.
 * The existing {@code PaymentWebhookController} for VietQR uses a key-value reassembly
 * scheme; the migration endpoint follows the simpler raw-body convention because modern
 * payment gateways (Stripe / Adyen) all use the same approach.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192 Phase 4b-i)
 */
@Slf4j
@Component
public class MigrationWebhookVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${kitehub.trial-to-paid.webhook-secret:}")
    private String webhookSecret;

    /**
     * @return true if {@code signatureHex} is a valid HMAC-SHA256 of {@code rawBody}
     *     using the configured webhook secret. Logs and returns false on any error
     *     (missing secret, bad signature length, crypto failure).
     */
    public boolean verify(String rawBody, String signatureHex) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Webhook secret not configured — rejecting migration webhook");
            return false;
        }
        if (rawBody == null || signatureHex == null || signatureHex.isBlank()) {
            return false;
        }
        try {
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] computed = hmac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            boolean ok = constantTimeEquals(computedHex, signatureHex.toLowerCase());
            if (!ok) {
                log.warn("Webhook signature mismatch");
            }
            return ok;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to verify webhook signature", e);
            return false;
        }
    }

    /** Constant-time string comparison to prevent timing attacks. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
