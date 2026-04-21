package com.kitehub.subscription.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.service.TrialToPaidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Webhook endpoint for trial-to-paid migration gateway events (GAP-192 Phase 4b-i).
 *
 * <p>Contract per {@code documents/01-business/kitehub/trial-to-paid-migration/api-contract.md}
 * (POST /webhooks/payment). The VietQR webhook at {@code /webhooks/payment} already
 * owns that path in {@link com.kitehub.subscription.controller.PaymentWebhookController}
 * so the migration-specific webhook lives at {@code /webhooks/trial-migration}. Both
 * endpoints use HMAC-SHA256 verification; see {@link MigrationWebhookVerifier}.</p>
 *
 * <h3>Event handling</h3>
 * <ul>
 *   <li>{@code payment.captured} — advances PAYMENT_PENDING → PAYMENT_CAPTURED; the
 *       async {@code MigrationScheduler} picks it up for the actual MIGRATING work.</li>
 *   <li>{@code payment.reversed} — triggers the rollback flow (ACTIVE → TRIAL) if
 *       still inside the T2P-04 24h window.</li>
 * </ul>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192 Phase 4b-i)
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/webhooks/trial-migration")
@RequiredArgsConstructor
@Tag(name = "Trial Migration Webhooks",
    description = "Gateway webhooks that drive the trial-to-paid migration state machine")
public class MigrationWebhookController {

    private final TrialToPaidService trialToPaidService;
    private final MigrationWebhookVerifier verifier;
    private final ObjectMapper objectMapper;

    /**
     * Entry point for {@code payment.captured} / {@code payment.reversed} gateway events.
     *
     * <p>Body is received as a String to preserve raw bytes for HMAC verification —
     * re-serialising a Map would produce different whitespace / key ordering and break
     * the signature.</p>
     */
    @Operation(summary = "Migration webhook (HMAC-verified)",
        description = "Accepts payment.captured + payment.reversed events from the gateway")
    @PostMapping
    public ResponseEntity<Map<String, Object>> receive(
        @RequestBody String rawBody,
        @RequestHeader(value = "X-Signature", required = false) String signature) {

        if (!verifier.verify(rawBody, signature)) {
            log.warn("Rejecting migration webhook — bad or missing HMAC signature");
            return ResponseEntity.status(401).body(Map.of("error", "invalid signature"));
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            log.warn("Migration webhook rejected — malformed JSON: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "malformed json"));
        }

        String eventType = textOrNull(root, "eventType");
        String paymentIntentId = textOrNull(root, "paymentIntentId");
        JsonNode metadata = root.get("metadata");
        if (eventType == null || metadata == null || !metadata.has("instanceId")) {
            log.warn("Migration webhook rejected — missing required fields");
            return ResponseEntity.badRequest().body(Map.of("error", "missing required fields"));
        }
        UUID instanceId;
        try {
            instanceId = UUID.fromString(metadata.get("instanceId").asText());
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid instanceId"));
        }

        MigrationWebhookEventType type = MigrationWebhookEventType.fromWireName(eventType);
        if (type == null) {
            log.warn("Migration webhook rejected — unknown eventType: {}", eventType);
            return ResponseEntity.badRequest().body(Map.of("error", "unknown eventType"));
        }

        log.info("Processing migration webhook: event={} instance={} paymentIntent={}",
            type, instanceId, paymentIntentId);

        switch (type) {
            case PAYMENT_CAPTURED -> trialToPaidService.handlePaymentCaptured(instanceId, paymentIntentId);
            case PAYMENT_REVERSED -> {
                String reason = textOrDefault(root, "reason", "gateway.reversal");
                trialToPaidService.handlePaymentReversed(instanceId, reason);
            }
        }

        return ResponseEntity.ok(Map.of("ack", true));
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String textOrDefault(JsonNode node, String field, String fallback) {
        String v = textOrNull(node, field);
        return v == null ? fallback : v;
    }
}
