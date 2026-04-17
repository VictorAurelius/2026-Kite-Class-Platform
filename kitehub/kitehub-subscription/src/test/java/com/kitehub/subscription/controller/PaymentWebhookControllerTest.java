package com.kitehub.subscription.controller;

import com.kitehub.subscription.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PaymentWebhookController.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PaymentWebhookControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentWebhookController controller;

    private static final String WEBHOOK_SECRET = "test-webhook-secret-key";

    @BeforeEach
    void setUp() {
        // Set webhook secret using reflection (since it's @Value injected)
        ReflectionTestUtils.setField(controller, "webhookSecret", WEBHOOK_SECRET);
    }

    @Test
    void shouldAcceptValidWebhookSignature() {
        // Given
        Map<String, Object> payload = createTestPayload();
        String validSignature = computeSignature(payload, WEBHOOK_SECRET);
        payload.put("signature", validSignature);

        // When
        ResponseEntity<Map<String, String>> response = controller.handlePaymentWebhook(payload);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "success");

        verify(paymentService).processPaymentWebhook(
            eq("VCB123456789"),
            eq(500000L),
            eq("KITECLASS ABC123DE")
        );
    }

    @Test
    void shouldRejectInvalidSignature() {
        // Given
        Map<String, Object> payload = createTestPayload();
        payload.put("signature", "invalid-signature");

        // When
        ResponseEntity<Map<String, String>> response = controller.handlePaymentWebhook(payload);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "Invalid signature");

        verify(paymentService, never()).processPaymentWebhook(anyString(), anyLong(), anyString());
    }

    @Test
    void shouldRejectMissingSignature() {
        // Given
        Map<String, Object> payload = createTestPayload();
        // No signature field

        // When
        ResponseEntity<Map<String, String>> response = controller.handlePaymentWebhook(payload);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "Missing signature");

        verify(paymentService, never()).processPaymentWebhook(anyString(), anyLong(), anyString());
    }

    @Test
    void shouldRejectEmptySignature() {
        // Given
        Map<String, Object> payload = createTestPayload();
        payload.put("signature", "");

        // When
        ResponseEntity<Map<String, String>> response = controller.handlePaymentWebhook(payload);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "Missing signature");

        verify(paymentService, never()).processPaymentWebhook(anyString(), anyLong(), anyString());
    }

    @Test
    void shouldRejectWebhookWhenSecretNotConfigured() {
        // Given
        ReflectionTestUtils.setField(controller, "webhookSecret", "");
        Map<String, Object> payload = createTestPayload();
        payload.put("signature", "any-signature");

        // When
        ResponseEntity<Map<String, String>> response = controller.handlePaymentWebhook(payload);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "Invalid signature");

        verify(paymentService, never()).processPaymentWebhook(anyString(), anyLong(), anyString());
    }

    @Test
    void shouldHandlePaymentServiceException() {
        // Given
        Map<String, Object> payload = createTestPayload();
        String validSignature = computeSignature(payload, WEBHOOK_SECRET);
        payload.put("signature", validSignature);

        doThrow(new RuntimeException("Payment processing failed"))
            .when(paymentService).processPaymentWebhook(anyString(), anyLong(), anyString());

        // When
        ResponseEntity<Map<String, String>> response = controller.handlePaymentWebhook(payload);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void shouldRejectTamperedPayload() {
        // Given
        Map<String, Object> payload = createTestPayload();
        String validSignature = computeSignature(payload, WEBHOOK_SECRET);

        // Tamper with payload after signature computation
        payload.put("amount", 1000000); // Changed from 500000
        payload.put("signature", validSignature);

        // When
        ResponseEntity<Map<String, String>> response = controller.handlePaymentWebhook(payload);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "Invalid signature");

        verify(paymentService, never()).processPaymentWebhook(anyString(), anyLong(), anyString());
    }

    @Test
    void shouldComputeConsistentSignature() {
        // Given
        Map<String, Object> payload = createTestPayload();

        // When - compute signature twice
        String signature1 = computeSignature(payload, WEBHOOK_SECRET);
        String signature2 = computeSignature(payload, WEBHOOK_SECRET);

        // Then - signatures should be identical
        assertThat(signature1).isEqualTo(signature2);
    }

    @Test
    void shouldHandlePayloadWithDifferentFieldOrder() {
        // Given - same data in different order
        Map<String, Object> payload1 = Map.of(
            "transactionId", "VCB123456789",
            "amount", 500000,
            "content", "KITECLASS ABC123DE",
            "bankCode", "VCB"
        );

        Map<String, Object> payload2 = Map.of(
            "bankCode", "VCB",
            "content", "KITECLASS ABC123DE",
            "amount", 500000,
            "transactionId", "VCB123456789"
        );

        // When
        String signature1 = computeSignature(payload1, WEBHOOK_SECRET);
        String signature2 = computeSignature(payload2, WEBHOOK_SECRET);

        // Then - signatures should be identical (sorted internally)
        assertThat(signature1).isEqualTo(signature2);
    }

    /**
     * Helper method to create test payload.
     */
    private Map<String, Object> createTestPayload() {
        return new TreeMap<>(Map.of(
            "transactionId", "VCB123456789",
            "amount", 500000,
            "content", "KITECLASS ABC123DE",
            "bankCode", "VCB"
        ));
    }

    /**
     * Helper method to compute HMAC-SHA256 signature.
     * Mirrors the implementation in PaymentWebhookController.
     */
    private String computeSignature(Map<String, Object> payload, String secret) {
        try {
            // Sort payload and exclude signature field
            TreeMap<String, Object> sortedPayload = new TreeMap<>();
            payload.entrySet().stream()
                .filter(entry -> !"signature".equals(entry.getKey()))
                .forEach(entry -> sortedPayload.put(entry.getKey(), entry.getValue()));

            // Build payload string
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
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(payloadString.toString().getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new RuntimeException("Failed to compute signature", e);
        }
    }
}
