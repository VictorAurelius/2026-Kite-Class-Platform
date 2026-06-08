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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * GAP-440 / GAP-451 baseline scaffold — SePay webhook replay handling semantic
 * (rewritten Wave flow-kh3-2 when the webhook migrated from HMAC body-signature
 * to SePay {@code Authorization: Apikey} auth).
 *
 * <p><strong>Spring Boot baseline version: 3.5.14.</strong> Pins the request
 * binding + Apikey verification + replay forwarding behavior against the current
 * Spring Boot baseline. Post dep-bump (GAP-451), re-run to verify Spring's
 * request body binding + header binding still treat two identical SePay payloads
 * as identically valid (no implicit nonce/timestamp rejection from upstream).</p>
 *
 * <p><strong>Idempotency note:</strong> the controller forwards to
 * {@link PaymentService#processSepayWebhook} on EACH delivery; deduplication is
 * the service's responsibility (it early-returns when the SePay transaction id is
 * already stamped on a completed payment — see V64 unique index on
 * {@code payments.transaction_id}). This baseline pins the controller-level
 * "same valid payload → same downstream call" contract.</p>
 *
 * @see <a href="documents/04-quality/gaps/GAP-440-spring-boot-dep-bump-before-prod.md">GAP-440</a>
 * @see <a href="documents/04-quality/gaps/GAP-451-spring-boot-3-5-x-no-newer-patch-await-upstream.md">GAP-451</a>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Spring Boot 3.5.14 baseline — SePay webhook replay semantic (GAP-440/451)")
class WebhookIdempotencyReplayBaselineTest {

    private static final String SEPAY_API_KEY = "wave-flow-kh3-2-baseline-key";
    private static final String VALID_AUTH = "Apikey " + SEPAY_API_KEY;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentWebhookController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "sepayApiKey", SEPAY_API_KEY);
    }

    private Map<String, Object> sepayPayload(String id, long amount, String description) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id);
        payload.put("gateway", "Vietcombank");
        payload.put("transferType", "in");
        payload.put("transferAmount", amount);
        payload.put("description", description);
        return payload;
    }

    @Test
    @DisplayName("Same SePay payload replayed twice — both pass Apikey check (200 each)")
    void replaySamePayloadBothForwardToService() {
        Map<String, Object> payload1 = sepayPayload("SEPAY-KH3-001", 10_000L, "KH3SUB1A2B3C4D");
        Map<String, Object> payload2 = sepayPayload("SEPAY-KH3-001", 10_000L, "KH3SUB1A2B3C4D");

        ResponseEntity<Map<String, Object>> resp1 =
            controller.handlePaymentWebhook(VALID_AUTH, payload1);
        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map<String, Object>> resp2 =
            controller.handlePaymentWebhook(VALID_AUTH, payload2);
        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Controller forwards on each delivery; service dedups via transaction id.
        verify(paymentService, times(2))
            .processSepayWebhook(eq("SEPAY-KH3-001"), eq(10_000L), eq("KH3SUB1A2B3C4D"));
    }

    @Test
    @DisplayName("Replay with a wrong Apikey fails 401 — auth is per-request")
    void replayWithWrongApikeyFails401() {
        Map<String, Object> payload = sepayPayload("SEPAY-KH3-002", 10_000L, "KH3SUB0A1B2C3D");

        // Valid original
        ResponseEntity<Map<String, Object>> resp1 =
            controller.handlePaymentWebhook(VALID_AUTH, payload);
        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Replay with a tampered Apikey
        ResponseEntity<Map<String, Object>> resp2 =
            controller.handlePaymentWebhook("Apikey tampered-key", payload);
        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Only the first (authenticated) delivery reached the service.
        verify(paymentService, times(1)).processSepayWebhook(anyString(), anyLong(), anyString());
        verify(paymentService, never()).processSepayWebhook(eq("SEPAY-KH3-002"), eq(9_999_999L), anyString());
    }
}
