package com.kitehub.subscription.baseline;

import com.kitehub.subscription.controller.PaymentWebhookController;
import com.kitehub.subscription.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * GAP-440 Wave 86 Bucket B baseline scaffold — webhook replay handling semantic.
 *
 * <p><strong>Spring Boot baseline version: 3.5.14.</strong> This test pins the
 * webhook signature verification + replay behavior against the current Spring
 * Boot baseline. Wave 86 Bucket B prior agent confirmed Maven Central does NOT
 * yet publish a Spring Boot 3.5.15+ patch; real dep bump is deferred to
 * {@code GAP-451}. Post-bump, re-run this test to verify the HMAC-SHA256
 * signature verification + replay semantic is preserved across the Spring
 * framework upgrade.</p>
 *
 * <p><strong>Scope rationale (re-scoped Wave 86 Bucket B):</strong> The
 * current {@link PaymentWebhookController} does NOT yet store an explicit
 * {@code idempotency_key} column — replay protection relies on
 * {@code PaymentService.processPaymentWebhook}'s downstream check via the
 * payment row's {@code transactionId} + state machine (a completed payment
 * rejects further state transitions). This baseline pins the controller-level
 * "same signed payload → same downstream call" semantic so any future
 * idempotency-key column migration (post Spring Boot bump) can be diffed
 * against a verified baseline.</p>
 *
 * @see <a href="documents/04-quality/gaps/GAP-440-spring-boot-dep-bump-before-prod.md">GAP-440</a>
 * @see <a href="documents/04-quality/gaps/GAP-451-spring-boot-3-5-x-no-newer-patch-await-upstream.md">GAP-451</a>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Spring Boot 3.5.14 baseline — webhook replay semantic (GAP-440 Wave 86 Bucket B)")
class WebhookIdempotencyReplayBaselineTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentWebhookController controller;

    private static final String WEBHOOK_SECRET = "wave-86-bucket-b-baseline-secret";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "webhookSecret", WEBHOOK_SECRET);
    }

    /**
     * Baseline contract: replaying the exact same signed payload twice produces
     * the same successful (HTTP 200) controller response both times. Downstream
     * deduplication happens in {@link PaymentService} via payment state machine
     * (a completed payment rejects further completion attempts).
     *
     * <p>Post-Spring-Boot bump (GAP-451), re-run to verify Spring's request
     * binding + HMAC verification still treats two identical payloads as
     * identically valid (no implicit nonce/timestamp rejection added by
     * upstream).</p>
     */
    @Test
    @DisplayName("Same signed payload replayed twice — both pass signature check (200 each)")
    void replaySameSignedPayloadBothPassSignatureCheck() {
        Map<String, Object> payload1 = buildSignedPayload("VCB-WAVE86-001", 500_000L,
            "KITECLASS WAVE86-IDEMPOTENCY-TEST");
        Map<String, Object> payload2 = buildSignedPayload("VCB-WAVE86-001", 500_000L,
            "KITECLASS WAVE86-IDEMPOTENCY-TEST");

        // Both payloads should compute identical signatures (deterministic HMAC)
        assertThat(payload1.get("signature"))
            .as("Identical payloads must compute identical signatures (deterministic)")
            .isEqualTo(payload2.get("signature"));

        // First delivery
        ResponseEntity<Map<String, String>> resp1 = controller.handlePaymentWebhook(payload1);
        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Replayed delivery (same payload, same signature)
        ResponseEntity<Map<String, String>> resp2 = controller.handlePaymentWebhook(payload2);
        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Baseline pin: controller forwards to PaymentService on EACH replay
        // (downstream deduplication is the PaymentService's responsibility via
        // payment state machine — NOT controller's concern in current arch).
        // Post-bump verification: this contract preserved across Spring upgrade.
        verify(paymentService, times(2)).processPaymentWebhook(
            eq("VCB-WAVE86-001"), eq(500_000L), eq("KITECLASS WAVE86-IDEMPOTENCY-TEST"));
    }

    /**
     * Baseline contract: replay with tampered amount fails signature verification
     * (returns 401), even when the original transactionId matches a prior
     * legitimate delivery. Verifies that signature integrity is per-payload, not
     * per-transactionId.
     *
     * <p>Post-Spring-Boot bump, re-run to verify Spring's request body binding
     * does not silently coerce numeric types in a way that breaks HMAC.</p>
     */
    @Test
    @DisplayName("Replay with tampered amount fails 401 (signature integrity per-payload)")
    void replayWithTamperedAmountFails401() {
        Map<String, Object> originalPayload = buildSignedPayload("VCB-WAVE86-002",
            500_000L, "KITECLASS WAVE86-TAMPER-TEST");
        String originalSignature = (String) originalPayload.get("signature");

        // Send valid original
        ResponseEntity<Map<String, String>> resp1 =
            controller.handlePaymentWebhook(originalPayload);
        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Tampered replay: same txn-id + signature, but amount changed
        Map<String, Object> tamperedPayload = new TreeMap<>(Map.of(
            "transactionId", "VCB-WAVE86-002",
            "amount", 9_999_999L, // tampered
            "content", "KITECLASS WAVE86-TAMPER-TEST",
            "bankCode", "VCB"
        ));
        tamperedPayload.put("signature", originalSignature);

        ResponseEntity<Map<String, String>> resp2 =
            controller.handlePaymentWebhook(tamperedPayload);
        assertThat(resp2.getStatusCode())
            .as("Tampered replay must fail signature check (401)")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp2.getBody()).containsEntry("error", "Invalid signature");

        // Baseline pin: tampered replay must NOT reach PaymentService
        verify(paymentService, times(1)).processPaymentWebhook(
            anyString(), anyLong(), anyString());
    }

    /**
     * Helper: build a payload with deterministic HMAC-SHA256 signature
     * mirroring {@link PaymentWebhookController}'s signing algorithm.
     */
    private Map<String, Object> buildSignedPayload(String txnId, Long amount, String content) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", txnId);
        payload.put("amount", amount);
        payload.put("content", content);
        payload.put("bankCode", "VCB");
        payload.put("signature", computeSignature(payload));
        return payload;
    }

    private String computeSignature(Map<String, Object> payload) {
        try {
            TreeMap<String, Object> sorted = new TreeMap<>();
            payload.entrySet().stream()
                .filter(entry -> !"signature".equals(entry.getKey()))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));

            StringBuilder pstr = new StringBuilder();
            sorted.forEach((k, v) -> {
                if (pstr.length() > 0) {
                    pstr.append("&");
                }
                pstr.append(k).append("=").append(v);
            });

            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(
                WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(key);
            byte[] hash = hmac.doFinal(pstr.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute signature", e);
        }
    }
}
