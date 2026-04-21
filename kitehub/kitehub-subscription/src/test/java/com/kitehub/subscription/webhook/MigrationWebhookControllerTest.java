package com.kitehub.subscription.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.service.TrialToPaidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MigrationWebhookController} (GAP-192 Phase 4b-i).
 *
 * <p>Full MVC with @WebMvcTest would exercise the Jackson stack — these tests
 * call the controller method directly with a real {@link ObjectMapper} so we
 * cover the JSON parsing + signature-verification paths without spinning up
 * the dispatcher.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MigrationWebhookController")
class MigrationWebhookControllerTest {

    @Mock
    private TrialToPaidService trialToPaidService;

    @Mock
    private MigrationWebhookVerifier verifier;

    private MigrationWebhookController controller;

    private UUID instanceId;

    @BeforeEach
    void setUp() {
        controller = new MigrationWebhookController(trialToPaidService, verifier, new ObjectMapper());
        instanceId = UUID.randomUUID();
    }

    private String captureBody() {
        return "{\"eventType\":\"payment.captured\"," +
            "\"paymentIntentId\":\"pi_abc\"," +
            "\"metadata\":{\"instanceId\":\"" + instanceId + "\"}," +
            "\"amount\":49900,\"currency\":\"VND\"}";
    }

    private String reversalBody() {
        return "{\"eventType\":\"payment.reversed\"," +
            "\"paymentIntentId\":\"pi_abc\"," +
            "\"metadata\":{\"instanceId\":\"" + instanceId + "\"}," +
            "\"reason\":\"chargeback\"}";
    }

    @Test
    @DisplayName("valid HMAC + payment.captured → advances phase + ack true")
    void captureHappyPath() {
        when(verifier.verify(any(), any())).thenReturn(true);
        ResponseEntity<Map<String, Object>> resp = controller.receive(captureBody(), "signature");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("ack", true);
        verify(trialToPaidService).handlePaymentCaptured(instanceId, "pi_abc");
    }

    @Test
    @DisplayName("valid HMAC + payment.reversed → calls handlePaymentReversed")
    void reversalHappyPath() {
        when(verifier.verify(any(), any())).thenReturn(true);

        ResponseEntity<Map<String, Object>> resp = controller.receive(reversalBody(), "signature");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(trialToPaidService).handlePaymentReversed(instanceId, "chargeback");
    }

    @Test
    @DisplayName("invalid HMAC → 401 and no service call")
    void invalidSignature() {
        when(verifier.verify(any(), any())).thenReturn(false);

        ResponseEntity<Map<String, Object>> resp = controller.receive(captureBody(), "bad");

        assertThat(resp.getStatusCode().value()).isEqualTo(401);
        verify(trialToPaidService, never()).handlePaymentCaptured(any(), any());
    }

    @Test
    @DisplayName("malformed JSON → 400")
    void malformedJson() {
        when(verifier.verify(any(), any())).thenReturn(true);

        ResponseEntity<Map<String, Object>> resp = controller.receive("not-json", "sig");

        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        verify(trialToPaidService, never()).handlePaymentCaptured(any(), any());
    }

    @Test
    @DisplayName("unknown eventType → 400")
    void unknownEvent() {
        when(verifier.verify(any(), any())).thenReturn(true);
        String body = "{\"eventType\":\"nope\",\"metadata\":{\"instanceId\":\"" + instanceId + "\"}}";

        ResponseEntity<Map<String, Object>> resp = controller.receive(body, "sig");

        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("missing instanceId → 400")
    void missingInstanceId() {
        when(verifier.verify(any(), any())).thenReturn(true);
        String body = "{\"eventType\":\"payment.captured\",\"metadata\":{}}";

        ResponseEntity<Map<String, Object>> resp = controller.receive(body, "sig");

        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("duplicate webhook with same paymentIntentId is tolerated by downstream service idempotency")
    void idempotentDuplicate() {
        // This test just asserts the controller doesn't crash on repeated deliveries —
        // real idempotency lives in the service (existing Phase 4a coverage).
        when(verifier.verify(any(), any())).thenReturn(true);
        controller.receive(captureBody(), "sig");
        controller.receive(captureBody(), "sig");
        verify(trialToPaidService, org.mockito.Mockito.times(2))
            .handlePaymentCaptured(eq(instanceId), eq("pi_abc"));
    }
}
