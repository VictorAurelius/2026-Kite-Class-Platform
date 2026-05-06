package com.kitehub.subscription.consent.controller;

import com.kitehub.subscription.consent.dto.ConsentRequest;
import com.kitehub.subscription.consent.dto.ConsentResponse;
import com.kitehub.subscription.consent.entity.ConsentRecord;
import com.kitehub.subscription.consent.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for server-side PDPL consent records.
 *
 * <p>Endpoints (per {@code documents/01-business/kitehub/marketing/api-contract.md}):</p>
 * <ul>
 *   <li>{@code POST /api/v1/consent/record} — idempotent upsert by visitorId</li>
 *   <li>{@code GET  /api/v1/consent/{visitorId}} — query current state</li>
 *   <li>{@code POST /api/v1/consent/{visitorId}/revoke} — revoke flow</li>
 * </ul>
 *
 * <p>Auth: unauthenticated by design — visitor_id is pseudonymous and
 * banner-stage visitors aren't logged in. Rate-limit + abuse mitigation lives
 * at the gateway (out of scope for this PR).</p>
 *
 * @since Wave 25 Bucket A — GAP-353b
 */
@RestController
@RequestMapping("/api/v1/consent")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Consent", description = "Server-side PDPL consent record store (GAP-353b Wave 25)")
public class ConsentController {

    private final ConsentService consentService;

    @Operation(summary = "Record or update consent",
               description = "Idempotent upsert keyed on visitorId. Pre-populates ipAddress + userAgent from request when client omits them.")
    @PostMapping("/record")
    public ResponseEntity<ConsentResponse> recordConsent(
            @Valid @RequestBody ConsentRequest request,
            HttpServletRequest httpRequest) {

        // Auto-populate ipAddress + userAgent from headers when client omits.
        if (request.getIpAddress() == null) {
            request.setIpAddress(extractClientIp(httpRequest));
        }
        if (request.getUserAgent() == null) {
            String ua = httpRequest.getHeader("User-Agent");
            if (ua != null && ua.length() > 4096) {
                ua = ua.substring(0, 4096);
            }
            request.setUserAgent(ua);
        }

        ConsentRecord saved = consentService.recordConsent(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ConsentResponse.from(saved));
    }

    @Operation(summary = "Get current consent state for visitor")
    @GetMapping("/{visitorId}")
    public ResponseEntity<ConsentResponse> getConsent(@PathVariable("visitorId") UUID visitorId) {
        Optional<ConsentRecord> record = consentService.findLatest(visitorId);
        return record
                .map(r -> ResponseEntity.ok(ConsentResponse.from(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Revoke consent for visitor",
               description = "Sets revokedAt + flips analytics+marketing to false on the latest record. Idempotent — repeated calls return the existing revoked record.")
    @PostMapping("/{visitorId}/revoke")
    public ResponseEntity<ConsentResponse> revokeConsent(@PathVariable("visitorId") UUID visitorId) {
        try {
            ConsentRecord revoked = consentService.revoke(visitorId);
            return ResponseEntity.ok(ConsentResponse.from(revoked));
        } catch (NoSuchElementException ex) {
            log.info("Revoke requested for unknown visitor={}", visitorId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    /**
     * Extract client IP honouring {@code X-Forwarded-For} when present
     * (gateway-injected). Falls back to {@code remoteAddr}. Truncated to 45
     * chars (RFC 4291 IPv6 max).
     */
    private static String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip;
        if (forwarded != null && !forwarded.isBlank()) {
            // First entry is original client; subsequent are proxies.
            int comma = forwarded.indexOf(',');
            ip = (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        } else {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.length() > 45) {
            ip = ip.substring(0, 45);
        }
        return ip;
    }
}
