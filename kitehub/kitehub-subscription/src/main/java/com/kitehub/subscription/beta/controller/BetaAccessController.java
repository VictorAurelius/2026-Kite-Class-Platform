package com.kitehub.subscription.beta.controller;

import com.kitehub.subscription.beta.dto.BetaApproveCommand;
import com.kitehub.subscription.beta.dto.BetaRejectCommand;
import com.kitehub.subscription.beta.dto.BetaRequestDto;
import com.kitehub.subscription.beta.dto.BetaRequestPage;
import com.kitehub.subscription.beta.dto.BetaRequestResponse;
import com.kitehub.subscription.beta.dto.BetaSignupCommand;
import com.kitehub.subscription.beta.dto.BetaTokenValidationResponse;
import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.beta.service.BetaAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for the beta tenant invite mechanism (GAP-372 Wave 33).
 *
 * <p>5 endpoints across 3 audiences (per {@code release-1-deploy-plan.md} §2.3):</p>
 * <ul>
 *   <li>{@code POST /api/v1/auth/request-beta-access} — public, rate-limited at gateway</li>
 *   <li>{@code GET  /api/v1/auth/beta-signup/validate?token=XXX} — public token pre-fill</li>
 *   <li>{@code POST /api/v1/auth/beta-signup} — public token-redemption signup</li>
 *   <li>{@code GET  /api/v1/admin/beta-requests?status=PENDING&page=0&size=20} — coordinator</li>
 *   <li>{@code POST /api/v1/admin/beta-requests/{id}/approve} — coordinator</li>
 *   <li>{@code POST /api/v1/admin/beta-requests/{id}/reject} — coordinator</li>
 * </ul>
 *
 * <p>Admin endpoints expect a coordinator role guarded at the gateway / Spring
 * Security configuration level (out of scope for this controller; mirrors how
 * {@code DsarController} keeps the security config separate).</p>
 *
 * @since Wave 33 — GAP-372
 */
@RestController
@Slf4j
@Tag(name = "Beta Access", description = "Beta tenant invite mechanism (GAP-372 Wave 33 Phase 1 BETA)")
public class BetaAccessController {

    private final BetaAccessService service;

    public BetaAccessController(BetaAccessService service) {
        this.service = service;
    }

    // ── Public endpoints ──────────────────────────────────────────────

    @Operation(summary = "Submit a beta access request",
               description = "Public unauthenticated endpoint. Honeypot field MUST be empty. Rate-limit per IP enforced at gateway.")
    @PostMapping("/api/v1/auth/request-beta-access")
    public ResponseEntity<BetaRequestResponse> submitRequest(@Valid @RequestBody BetaRequestDto dto) {
        BetaAccessRequest saved = service.submitRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(BetaRequestResponse.from(saved));
    }

    @Operation(summary = "Validate an invite token (signup pre-fill)",
               description = "Public unauthenticated endpoint. Returns email + name + persona pre-fill if token is valid + APPROVED + not expired.")
    @GetMapping("/api/v1/auth/beta-signup/validate")
    public ResponseEntity<BetaTokenValidationResponse> validateToken(@RequestParam("token") UUID token) {
        BetaTokenValidationResponse resp = service.validateToken(token);
        if (!resp.valid()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Complete beta signup with invite token",
               description = "Public unauthenticated endpoint. Marks the request SIGNED_UP and clears the token. Tenant provisioning is delegated to the standard registration pipeline.")
    @PostMapping("/api/v1/auth/beta-signup")
    public ResponseEntity<BetaRequestResponse> completeBetaSignup(@Valid @RequestBody BetaSignupCommand cmd) {
        try {
            BetaAccessRequest saved = service.completeBetaSignup(cmd);
            return ResponseEntity.ok(BetaRequestResponse.from(saved));
        } catch (IllegalArgumentException ex) {
            log.warn("Beta signup rejected: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException ex) {
            log.warn("Beta signup invalid state: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // ── Coordinator endpoints ─────────────────────────────────────────

    @Operation(summary = "List beta access requests by status (coordinator)",
               description = "Coordinator-only. Defaults to PENDING-first ordering by createdAt desc.")
    @GetMapping("/api/v1/admin/beta-requests")
    public ResponseEntity<BetaRequestPage> listRequests(
            @RequestParam(value = "status", defaultValue = "PENDING") BetaAccessRequestStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<BetaAccessRequest> result = service.listByStatus(status, PageRequest.of(safePage, safeSize));
        BetaRequestPage body = new BetaRequestPage(
                result.getContent().stream().map(BetaRequestResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Approve a beta access request (coordinator)",
               description = "Issues invite token, +24h expiry, publishes invite-sent event via Outbox.")
    @PostMapping("/api/v1/admin/beta-requests/{id}/approve")
    public ResponseEntity<BetaRequestResponse> approve(
            @PathVariable("id") Long id,
            @Valid @RequestBody BetaApproveCommand cmd) {
        try {
            BetaAccessRequest saved = service.approveRequest(id, cmd);
            return ResponseEntity.ok(BetaRequestResponse.from(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException ex) {
            log.warn("Approve invalid state: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(summary = "Reject a beta access request (coordinator)")
    @PostMapping("/api/v1/admin/beta-requests/{id}/reject")
    public ResponseEntity<BetaRequestResponse> reject(
            @PathVariable("id") Long id,
            @Valid @RequestBody BetaRejectCommand cmd) {
        try {
            BetaAccessRequest saved = service.rejectRequest(id, cmd);
            return ResponseEntity.ok(BetaRequestResponse.from(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException ex) {
            log.warn("Reject invalid state: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
