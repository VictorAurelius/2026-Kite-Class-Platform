package com.kitehub.subscription.beta.controller;

import com.kitehub.subscription.beta.dto.BetaApproveCommand;
import com.kitehub.subscription.beta.dto.BetaClaimCodeExchangeCommand;
import com.kitehub.subscription.beta.dto.BetaClaimCodeExchangeResponse;
import com.kitehub.subscription.beta.dto.BetaRejectCommand;
import com.kitehub.subscription.beta.dto.BetaRequestDto;
import com.kitehub.subscription.beta.dto.BetaRequestPage;
import com.kitehub.subscription.beta.dto.BetaRequestResponse;
import com.kitehub.subscription.beta.dto.BetaSignupCommand;
import com.kitehub.subscription.beta.dto.BetaSignupErrorResponse;
import com.kitehub.subscription.beta.dto.BetaTokenValidationResponse;
import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.audit.Auditable;
import com.kitehub.subscription.beta.service.BetaAccessService;
import com.kitehub.subscription.beta.service.BetaRateLimitExceededException;
import com.kitehub.subscription.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the beta tenant invite mechanism (GAP-372 Wave 33).
 *
 * <p>5 endpoints across 3 audiences (per {@code release-1-deploy-plan.md} §2.3):</p>
 * <ul>
 *   <li>{@code POST /api/v1/auth/request-beta-access} — public, rate-limited at gateway</li>
 *   <li>{@code GET  /api/v1/auth/beta-signup/validate?token=...} — public token pre-fill</li>
 *   <li>{@code POST /api/v1/auth/beta-signup} — public token-redemption signup</li>
 *   <li>{@code GET  /api/v1/admin/beta-requests?status=PENDING&page=0&size=20} — coordinator</li>
 *   <li>{@code POST /api/v1/admin/beta-requests/{id}/approve} — coordinator</li>
 *   <li>{@code POST /api/v1/admin/beta-requests/{id}/reject} — coordinator</li>
 * </ul>
 *
 * <p>Admin endpoints are guarded at the controller via {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")}
 * (closes GAP-384, Wave 35). Authentication comes from the gateway-forwarded
 * {@code X-User-Id} + {@code X-User-Roles} headers translated into Spring
 * {@code SecurityContext} authorities by
 * {@link com.kitehub.subscription.config.SecurityConfig.XUserRolesHeaderFilter}.</p>
 *
 * @since Wave 33 — GAP-372
 */
@RestController
@Slf4j
@Tag(name = "Beta Access", description = "Beta tenant invite mechanism (GAP-372 Wave 33 Phase 1 BETA)")
public class BetaAccessController {

    private final BetaAccessService service;
    private final AuthService authService;

    public BetaAccessController(BetaAccessService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    // ── Public endpoints ──────────────────────────────────────────────

    @Operation(summary = "Submit a beta access request",
               description = "Public unauthenticated endpoint. Honeypot field MUST be empty. Rate-limit per IP enforced at gateway + per-email 24h rate limit (GAP-388 388-C).")
    @PostMapping("/api/v1/auth/request-beta-access")
    public ResponseEntity<BetaRequestResponse> submitRequest(
            @Valid @RequestBody BetaRequestDto dto,
            HttpServletRequest request) {
        BetaAccessRequest saved = service.submitRequest(dto, resolveClientIp(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(BetaRequestResponse.from(saved));
    }

    @Operation(summary = "Exchange a 6-digit claim code for the invite token (GAP-388 388-B 2FA)",
               description = "Public unauthenticated endpoint. Submits the claim code from the invite email and returns the underlying invite_token UUID + pre-fill data.")
    @PostMapping("/api/v1/auth/beta-signup/exchange-claim-code")
    public ResponseEntity<BetaClaimCodeExchangeResponse> exchangeClaimCode(
            @Valid @RequestBody BetaClaimCodeExchangeCommand cmd) {
        BetaClaimCodeExchangeResponse resp = service.exchangeClaimCode(cmd.claimCode());
        if (!resp.valid()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Validate an invite token (signup pre-fill)",
               description = "Public unauthenticated endpoint. Returns email + name + persona pre-fill if token is valid + APPROVED + not expired.")
    @GetMapping("/api/v1/auth/beta-signup/validate")
    public ResponseEntity<BetaTokenValidationResponse> validateToken(@RequestParam UUID token) {
        BetaTokenValidationResponse resp = service.validateToken(token);
        if (!resp.valid()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Complete beta signup with invite token",
               description = "Public unauthenticated endpoint. Marks the request SIGNED_UP, then provisions the tenant via the standard registration pipeline (subdomain reservation, owner-user creation, password hashing). On registration failure the beta request is rolled back to APPROVED with a fresh invite token so the invitee can retry (GAP-372 closure follow-up #1, Wave 45 Bucket A). Error responses include JSON body with {@code errorCode} field (INVALID_TOKEN/TOKEN_EXPIRED/WRONG_STATE) per GAP-611 (Wave beta-readiness-5 Bucket D) so FE + observability can differentiate application-layer 404 from infrastructure 404.")
    @PostMapping("/api/v1/auth/beta-signup")
    public ResponseEntity<?> completeBetaSignup(@Valid @RequestBody BetaSignupCommand cmd) {
        BetaAccessRequest saved;
        try {
            saved = service.completeBetaSignup(cmd);
        } catch (IllegalArgumentException ex) {
            // Token not found in DB (never issued / already redeemed + nulled out) →
            // 404 with JSON body so caller can distinguish from infrastructure 404
            // (gateway misroute, controller absent). Closes GAP-611.
            log.warn("Beta signup rejected: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BetaSignupErrorResponse.invalidToken());
        } catch (IllegalStateException ex) {
            // Token found but request in non-APPROVED state OR token expired →
            // 409 with JSON body so FE can show specific guidance.
            log.warn("Beta signup invalid state: {}", ex.getMessage());
            String msg = ex.getMessage() == null ? "" : ex.getMessage();
            BetaSignupErrorResponse body = msg.contains("expired")
                    ? BetaSignupErrorResponse.tokenExpired()
                    : BetaSignupErrorResponse.wrongState(extractStateFromMessage(msg));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        // Token redeemed + status flipped to SIGNED_UP. Now provision the tenant
        // via the standard registration pipeline. If it fails, roll the beta
        // request back to APPROVED so the invitee can retry with the same email.
        try {
            authService.registerFromBetaInvite(
                    saved.getOrgName(),
                    cmd.subdomain(),
                    saved.getEmail(),
                    cmd.ownerPassword());
        } catch (IllegalArgumentException ex) {
            // Conflicting subdomain / email — surface 409 + rollback.
            log.warn("Beta signup tenant provisioning conflict (rolling back): {}", ex.getMessage());
            service.rollbackSignup(saved.getId());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException ex) {
            // Generic provisioning failure — rollback + surface 500 so the FE
            // can show a retry message; invitee keeps a usable token.
            log.error("Beta signup tenant provisioning failed (rolling back): {}", ex.getMessage(), ex);
            service.rollbackSignup(saved.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok(BetaRequestResponse.from(saved));
    }

    // ── Coordinator endpoints ─────────────────────────────────────────

    @Operation(summary = "List beta access requests by status (coordinator)",
               description = "Coordinator-only. Defaults to PENDING-first ordering by createdAt desc.")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping("/api/v1/admin/beta-requests")
    public ResponseEntity<BetaRequestPage> listRequests(
            @RequestParam(defaultValue = "PENDING") BetaAccessRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
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
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Auditable(
        action = "BETA_REQUEST_APPROVE",
        entityType = "beta_access_request",
        resourceType = "beta_access_request",
        resourceIdSource = "arg0")
    @PostMapping("/api/v1/admin/beta-requests/{id}/approve")
    public ResponseEntity<BetaRequestResponse> approve(
            @PathVariable Long id,
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
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Auditable(
        action = "BETA_REQUEST_REJECT",
        entityType = "beta_access_request",
        resourceType = "beta_access_request",
        resourceIdSource = "arg0")
    @PostMapping("/api/v1/admin/beta-requests/{id}/reject")
    public ResponseEntity<BetaRequestResponse> reject(
            @PathVariable Long id,
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

    // ── Exception handlers (controller-scoped — take precedence over GlobalExceptionHandler) ─

    /**
     * Bean-Validation handler that wires the honeypot detection into
     * {@link BetaAccessService#recordHoneypotRejection(String, String)}
     * (GAP-388 388-A — closes GAP-387 dead-wire).
     *
     * <p>Spring's {@code @Valid @RequestBody} raises
     * {@link MethodArgumentNotValidException} when {@code BetaRequestDto.honeypot}
     * violates {@code @Size(max = 0)}. The global handler maps this to a generic
     * 400 — but we need the side-effect of incrementing
     * {@code beta_honeypot_rejections_total} so the {@code BetaHoneypotSpike}
     * alert rule has data to fire on.</p>
     *
     * <p>Only the {@code BetaRequestDto} surface ({@code POST /request-beta-access})
     * is in scope — other validation failures fall through to the global 400
     * handler unchanged (still surfaced as 400 from this method).</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<String> violatedFields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField())
                .toList();
        boolean honeypotTripped = violatedFields.contains("honeypot");
        if (honeypotTripped) {
            String email = extractTargetField(ex, "email");
            service.recordHoneypotRejection(email, resolveClientIp(request));
        }
        StringBuilder errors = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
            errors.append(fe.getField()).append(": ").append(fe.getDefaultMessage()).append("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errors.toString());
        pd.setTitle("Validation Error");
        return ResponseEntity.badRequest().body(pd);
    }

    /**
     * Maps {@link BetaRateLimitExceededException} to HTTP 429 (GAP-388 388-C).
     */
    @ExceptionHandler(BetaRateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(BetaRateLimitExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        pd.setTitle("Too Many Requests");
        pd.setProperty("errorCode", "BETA_EMAIL_RATE_LIMIT");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(pd);
    }

    /**
     * Best-effort parse của {@code BetaAccessService.completeBetaSignup}
     * {@code IllegalStateException} message ("Cannot complete signup in state X
     * (must be APPROVED)") để extract state name cho error response. Fallback
     * "UNKNOWN" nếu message format không match.
     *
     * @since Wave beta-readiness-5 — GAP-611
     */
    private static String extractStateFromMessage(String msg) {
        if (msg == null) {
            return "UNKNOWN";
        }
        int start = msg.indexOf("in state ");
        if (start < 0) {
            return "UNKNOWN";
        }
        start += "in state ".length();
        int end = msg.indexOf(' ', start);
        if (end < 0) {
            end = msg.length();
        }
        return msg.substring(start, end);
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
            // First entry is the closest-to-client IP.
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Pull the raw value of a field on the rejected payload from the binding
     * result so the audit log can record the attempted email even when the
     * payload failed validation. Returns {@code null} if not bound.
     */
    private static String extractTargetField(MethodArgumentNotValidException ex, String fieldName) {
        Object target = ex.getBindingResult().getTarget();
        if (target instanceof BetaRequestDto dto) {
            return switch (fieldName) {
                case "email" -> dto.email();
                default -> null;
            };
        }
        return null;
    }
}
