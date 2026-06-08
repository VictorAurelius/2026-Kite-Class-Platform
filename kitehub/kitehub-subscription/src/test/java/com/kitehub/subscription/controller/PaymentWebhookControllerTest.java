package com.kitehub.subscription.controller;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link PaymentWebhookController} — SePay Apikey authentication +
 * payload adapter (Wave flow-kh3-2, GAP-976).
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentWebhookController — SePay webhook")
class PaymentWebhookControllerTest {

    private static final String SEPAY_API_KEY = "test-sepay-api-key";
    private static final String VALID_AUTH = "Apikey " + SEPAY_API_KEY;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentWebhookController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "sepayApiKey", SEPAY_API_KEY);
    }

    private Map<String, Object> sepayPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "92704");
        payload.put("gateway", "Vietcombank");
        payload.put("transferType", "in");
        payload.put("transferAmount", 10000);
        payload.put("description", "KH3SUB1A2B3C4D");
        payload.put("referenceCode", "FT2406001");
        return payload;
    }

    @Test
    @DisplayName("Valid Apikey + incoming transfer → 200 and forwards to service")
    void validApikey_processesPayment() {
        ResponseEntity<Map<String, Object>> response =
            controller.handlePaymentWebhook(VALID_AUTH, sepayPayload());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "success");
        verify(paymentService).processSepayWebhook(eq("92704"), eq(10000L), eq("KH3SUB1A2B3C4D"));
    }

    @Test
    @DisplayName("Missing Authorization header → 401, never reaches service")
    void missingApikey_rejected() {
        ResponseEntity<Map<String, Object>> response =
            controller.handlePaymentWebhook(null, sepayPayload());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(paymentService, never()).processSepayWebhook(anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Wrong Apikey → 401, never reaches service")
    void wrongApikey_rejected() {
        ResponseEntity<Map<String, Object>> response =
            controller.handlePaymentWebhook("Apikey wrong-key", sepayPayload());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(paymentService, never()).processSepayWebhook(anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Api-key not configured → 401")
    void apiKeyNotConfigured_rejected() {
        ReflectionTestUtils.setField(controller, "sepayApiKey", "");

        ResponseEntity<Map<String, Object>> response =
            controller.handlePaymentWebhook(VALID_AUTH, sepayPayload());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(paymentService, never()).processSepayWebhook(anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Non-incoming transfer (out) → 200 ignored, not processed")
    void outgoingTransfer_ignored() {
        Map<String, Object> payload = sepayPayload();
        payload.put("transferType", "out");

        ResponseEntity<Map<String, Object>> response =
            controller.handlePaymentWebhook(VALID_AUTH, payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ignored");
        verify(paymentService, never()).processSepayWebhook(anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Orphan txnRef (service throws IllegalArgumentException) → 400")
    void orphanTxnRef_returns400() {
        doThrow(new IllegalArgumentException("No payment found for txnRef: KH3SUB1A2B3C4D"))
            .when(paymentService).processSepayWebhook(anyString(), anyLong(), anyString());

        ResponseEntity<Map<String, Object>> response =
            controller.handlePaymentWebhook(VALID_AUTH, sepayPayload());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    @DisplayName("Unexpected service exception → 400")
    void serviceException_returns400() {
        doThrow(new RuntimeException("boom"))
            .when(paymentService).processSepayWebhook(anyString(), anyLong(), anyString());

        ResponseEntity<Map<String, Object>> response =
            controller.handlePaymentWebhook(VALID_AUTH, sepayPayload());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
