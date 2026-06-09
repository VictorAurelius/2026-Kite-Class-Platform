package com.kitehub.subscription.saleslead.controller;

import com.kitehub.subscription.saleslead.dto.CreateSalesLeadRequest;
import com.kitehub.subscription.saleslead.dto.SalesLeadResponse;
import com.kitehub.subscription.saleslead.entity.SalesLead;
import com.kitehub.subscription.saleslead.service.SalesLeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for KiteHub PLATFORM sales lead capture (GAP-1101).
 *
 * <p>Single endpoint:</p>
 * <ul>
 *   <li>{@code POST /api/platform/sales-leads} — PUBLIC unauthenticated. A
 *       prospective center owner contacting KiteHub sales about the Enterprise
 *       SaaS plan (no JWT — not logged in). Honeypot MUST be empty; rate-limit
 *       per IP enforced at gateway (2 req/sec/IP). Returns HTTP 201 + minimal
 *       receipt.</li>
 * </ul>
 *
 * <p>Distinct from the {@code kiteclass-core} tenant-marketing lead domain
 * (student → center). This is the KiteHub PLATFORM sales funnel.</p>
 *
 * <p>Gateway routes {@code /api/platform/sales-leads} → kitehub-subscription and
 * whitelists it as a public path (see {@code JwtAuthenticationGatewayFilter
 * .isPublicPath}); the subscription {@code SecurityConfig} permits it via
 * explicit {@code permitAll()} (POST only).</p>
 *
 * @since GAP-1101
 */
@RestController
@Slf4j
@Tag(name = "Sales Leads", description = "KiteHub PLATFORM sales lead capture (GAP-1101)")
public class SalesLeadController {

    private final SalesLeadService service;

    public SalesLeadController(SalesLeadService service) {
        this.service = service;
    }

    @Operation(
            summary = "Submit a KiteHub PLATFORM sales lead",
            description = "Public unauthenticated endpoint. Honeypot MUST be empty (bot trap). "
                    + "Rate-limit per IP enforced at gateway. planInterest defaults to ENTERPRISE."
    )
    @PostMapping("/api/platform/sales-leads")
    public ResponseEntity<SalesLeadResponse> submit(
            @Valid @RequestBody CreateSalesLeadRequest request,
            HttpServletRequest httpRequest) {

        SalesLead saved = service.submit(request, resolveClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SalesLeadResponse.from(saved));
    }

    /**
     * Resolve the originating client IP. Honors X-Forwarded-For (gateway
     * forwards the chain) and falls back to {@code remoteAddr}.
     */
    private static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
