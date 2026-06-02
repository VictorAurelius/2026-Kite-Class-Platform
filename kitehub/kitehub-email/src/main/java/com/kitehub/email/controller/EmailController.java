package com.kitehub.email.controller;

import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import com.kitehub.email.service.EmailIdempotencyGuard;
import com.kitehub.email.service.EmailSender;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Email service REST controller.
 *
 * <p>SLO Tier C (write — sync SES dispatch wraps async delivery).
 * See {@code documents/05-guides/api-performance-slo.md}.
 *
 * <p><strong>GAP-840 Wave local-doable-6 Bucket H — HTTP idempotency:</strong>
 * accepts {@code Idempotency-Key} header (standard pattern per Stripe / MoMo / VietQR).
 * When provided, identical re-submits within the guard's TTL collapse to a single
 * downstream send and return the same response shape — caller-safe retry. When the
 * header is absent, the controller falls back to a content-derived key so caller
 * timeouts that resend the exact same body are still deduped. The dedup primitive
 * is the same {@link EmailIdempotencyGuard} (Redis SETNX + Caffeine fallback) that
 * GAP-580 introduced for the consumer; HTTP path uses key namespace prefix
 * {@code email:idempotency:http:*} via the request body content hash.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/emails")
@RequiredArgsConstructor
@Tag(name = "Email", description = "Internal email sending API (SMTP/SES)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-c", "controller", "email"})
public class EmailController {

    /**
     * Provider-selected email channel ({@code @Primary} {@link EmailSender} = the
     * {@code email.provider}-routing {@code EmailProviderRouter}). Injecting the
     * interface — NOT the concrete {@code SESEmailService} — is what makes
     * {@code email.provider=resend} actually reach Resend in production (GAP-788).
     */
    private final EmailSender emailSender;

    /**
     * Consumer-side idempotency guard (GAP-580). Reused here for HTTP path dedup
     * (GAP-840) — same Redis SETNX semantics, distinct caller-supplied key derived
     * from the {@code Idempotency-Key} header or hashed request body.
     */
    private final EmailIdempotencyGuard idempotencyGuard;

    /**
     * In-process response cache so a deduped HTTP retry returns the SAME response
     * shape as the original (per Stripe Idempotency contract). The guard owns the
     * "was-seen" decision; this cache stores the actual response body to replay.
     *
     * <p>Bounded via {@link ConcurrentHashMap#size()} check in {@link #cacheResponse}.</p>
     */
    private final Map<String, EmailResponse> responseCache = new ConcurrentHashMap<>();

    /** Max entries in {@link #responseCache} — bounded to limit memory at scale. */
    private static final int RESPONSE_CACHE_MAX = 10_000;

    /**
     * Send email (internal API).
     *
     * <p>Idempotency: optional {@code Idempotency-Key} header. When present, a
     * duplicate request (same key OR same body during TTL window) returns the
     * cached response without re-dispatching. When absent, the controller derives
     * a key from the request payload — protects against caller-timeout retries
     * that re-send the exact same body.</p>
     *
     * <p>Note: This API should only be called by other KiteHub services.
     * In production, use service-to-service authentication.</p>
     *
     * @param idempotencyKey optional caller-supplied key (Stripe-style); falls back
     *                       to content-derived key when null/blank
     * @param request        email request
     * @return Email response (same shape on dedup as on first send)
     */
    @PostMapping("/send")
    public ResponseEntity<EmailResponse> sendEmail(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody EmailRequest request) {

        if (request.getTemplateName() == null && request.getHtmlBody() == null) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Received email send request for: {} (Idempotency-Key={})",
                request.getTo(), idempotencyKey == null ? "<absent>" : "<present>");

        // GAP-840 — HTTP idempotency. Key precedence:
        //   1. Caller-supplied Idempotency-Key header (Stripe-style explicit dedup)
        //   2. Content hash derived from {to, templateName, emailType, sorted variables}
        // Either way, identical send within TTL window collapses to one provider call.
        Map<String, Object> derivedVariables = templateOrHtmlVariables(request);
        String derivedType = request.getTemplateName() != null ? "templated" : "html";
        String derivedKey = idempotencyGuard.computeKey(
                idempotencyKey, request.getTo(), request.getTemplateName(), derivedType, derivedVariables);
        String namespacedKey = "http:" + derivedKey;

        boolean firstSeen = idempotencyGuard.markIfFirstSeen(namespacedKey);
        if (!firstSeen) {
            // Duplicate within TTL window. Return cached response if available
            // (Stripe contract — same body on retry). If cache evicted, fall back
            // to a minimal DUPLICATE placeholder so caller's retry contract isn't
            // broken; downstream provider was already invoked by the first request.
            EmailResponse cached = responseCache.get(namespacedKey);
            if (cached != null) {
                log.info("HTTP idempotent replay — returning cached response for to={} key={}",
                        request.getTo(), idempotencyKey == null ? "<derived>" : idempotencyKey);
                return ResponseEntity.ok(cached);
            }
            log.info("HTTP idempotent skip (cache evicted) for to={} — returning DUPLICATE placeholder",
                    request.getTo());
            return ResponseEntity.ok(EmailResponse.builder()
                    .messageId("idempotent-skip")
                    .status("DUPLICATE")
                    .sentAt(LocalDateTime.now())
                    .build());
        }

        EmailResponse response;
        if (request.getTemplateName() != null) {
            response = emailSender.sendTemplatedEmail(request);
        } else {
            response = emailSender.sendEmail(
                    request.getTo(),
                    request.getSubject(),
                    request.getHtmlBody()
            );
        }

        cacheResponse(namespacedKey, response);
        return ResponseEntity.ok(response);
    }

    /**
     * Pull template variables when present; otherwise synthesize a stable map
     * from the raw HTML body so two identical HTML sends collide on the same
     * dedup key.
     */
    private static Map<String, Object> templateOrHtmlVariables(EmailRequest request) {
        if (request.getTemplateName() != null) {
            return request.getVariables() == null ? new HashMap<>() : request.getVariables();
        }
        Map<String, Object> synth = new HashMap<>();
        synth.put("__html", request.getHtmlBody() == null ? "" : request.getHtmlBody());
        synth.put("__subject", request.getSubject() == null ? "" : request.getSubject());
        return synth;
    }

    /**
     * Bounded write to {@link #responseCache} — evicts oldest-ish entry by clearing
     * a random key when cap reached. Caller retries within a few seconds are the
     * dominant pattern so absolute LRU isn't required.
     */
    private void cacheResponse(String key, EmailResponse response) {
        if (responseCache.size() >= RESPONSE_CACHE_MAX) {
            responseCache.keySet().stream().findAny().ifPresent(responseCache::remove);
        }
        responseCache.put(key, response);
    }
}
