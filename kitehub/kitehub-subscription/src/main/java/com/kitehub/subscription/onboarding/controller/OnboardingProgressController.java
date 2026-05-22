package com.kitehub.subscription.onboarding.controller;

import com.kitehub.subscription.onboarding.dto.OnboardingProgressResponse;
import com.kitehub.subscription.onboarding.dto.OnboardingProgressUpdateCommand;
import com.kitehub.subscription.onboarding.exception.TenantContextMissingException;
import com.kitehub.subscription.onboarding.exception.TenantHeaderJwtMismatchException;
import com.kitehub.subscription.onboarding.service.OnboardingProgressService;
import com.kitehub.subscription.service.JwtKeyService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.UUID;

/**
 * REST controller for Day-1 onboarding checklist (Wave 78 GAP-538).
 *
 * <p>Schema source-of-truth:
 * {@code documents/01-business/kitehub/onboarding/api-contract.md}.</p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code GET  /api/v1/onboarding-progress} — fetch current tenant checklist (lazy-init)</li>
 *   <li>{@code PUT  /api/v1/onboarding-progress} — update single step completion</li>
 * </ul>
 *
 * <p>Tenant scoping uses gateway-forwarded {@code X-Tenant-Id} header.
 * Authentication is enforced by the gateway plus
 * {@code SecurityConfig.XUserRolesHeaderFilter}; missing tenant header
 * yields HTTP 403 {@code TENANT_CONTEXT_MISSING}.</p>
 *
 * @since Wave 78 — GAP-538
 */
@RestController
@RequestMapping("/api/v1/onboarding-progress")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "Per-tenant Day-1 onboarding checklist")
@Slf4j
public class OnboardingProgressController {

    private final OnboardingProgressService service;
    private final JwtKeyService jwtKeyService;

    @GetMapping
    @Operation(summary = "Get onboarding progress for current tenant (lazy-init)")
    public ResponseEntity<OnboardingProgressResponse> getProgress(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        UUID tenantId = resolveTenant(tenantHeader, authorizationHeader);
        return ResponseEntity.ok(service.getProgress(tenantId));
    }

    @PutMapping
    @Operation(summary = "Update one step completion (idempotent on equal value)")
    public ResponseEntity<OnboardingProgressResponse> updateStep(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody OnboardingProgressUpdateCommand command
    ) {
        UUID tenantId = resolveTenant(tenantHeader, authorizationHeader);
        return ResponseEntity.ok(service.updateStep(tenantId, command));
    }

    /**
     * Resolve tenant context with two-path priority (GAP-712 — Wave 105 Bucket E fix):
     * <ol>
     *   <li>If {@code X-Tenant-Id} header present → use header value, cross-check
     *       against JWT {@code tenantId} claim (GAP-554 defense-in-depth).</li>
     *   <li>If header missing AND JWT contains {@code tenantId} claim → derive
     *       tenant from JWT claim (Wave 104 Bucket A enrichment makes this the
     *       primary resolution path for first-party callers).</li>
     *   <li>If neither → {@code 403 TENANT_CONTEXT_MISSING}.</li>
     * </ol>
     *
     * <p>Cross-check policy (when both present):</p>
     * <ul>
     *   <li>Header present, JWT absent OR has no {@code tenantId} claim → ALLOW
     *       (Phase 1 BETA — gateway is trusted boundary, JWT may pre-date claim).</li>
     *   <li>Both present + mismatch → {@code 403 TENANT_HEADER_JWT_MISMATCH}.</li>
     *   <li>JWT unparseable / invalid signature → ALLOW header value (auth filter
     *       is the canonical signature gate; this controller does not duplicate it).</li>
     * </ul>
     */
    private UUID resolveTenant(String tenantHeader, String authorizationHeader) {
        String jwtTenant = extractJwtTenantClaim(authorizationHeader);

        if (tenantHeader == null || tenantHeader.isBlank()) {
            // Fallback to JWT-derived tenant (Wave 104 Bucket A enrichment).
            if (jwtTenant == null || jwtTenant.isBlank()) {
                throw new TenantContextMissingException(
                    "X-Tenant-Id header missing and JWT tenantId claim absent");
            }
            try {
                return UUID.fromString(jwtTenant);
            } catch (IllegalArgumentException ex) {
                throw new TenantContextMissingException(
                    "JWT tenantId claim malformed (not a UUID)");
            }
        }

        // Header path with cross-check.
        final UUID tenantId;
        try {
            tenantId = UUID.fromString(tenantHeader);
        } catch (IllegalArgumentException ex) {
            throw new TenantContextMissingException("X-Tenant-Id header malformed (not a UUID)");
        }
        if (jwtTenant != null && !jwtTenant.isBlank() && !jwtTenant.equals(tenantHeader)) {
            log.warn("Tenant header/JWT mismatch: header={} jwt={}",
                tenantHeader, jwtTenant);
            throw new TenantHeaderJwtMismatchException(
                "X-Tenant-Id header does not match JWT tenantId claim");
        }
        return tenantId;
    }

    /**
     * Best-effort extraction of {@code tenantId} claim from a Bearer token.
     * Returns {@code null} for any failure path (no token, unparseable, no claim).
     * Authentication itself is enforced by {@code XUserRolesHeaderFilter}; this
     * helper is only concerned with the cross-check value.
     */
    private String extractJwtTenantClaim(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return null;
        }
        try {
            Claims claims = jwtKeyService.parse(token).getPayload();
            Object claim = claims.get("tenantId");
            return claim == null ? null : claim.toString();
        } catch (Exception ex) {
            // JWT invalid / expired / wrong signature → cross-check is no-op.
            return null;
        }
    }

    // ── Exception handlers (problem+json error envelope) ──

    @ExceptionHandler(TenantContextMissingException.class)
    public ResponseEntity<ProblemDetail> handleTenantMissing(TenantContextMissingException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        body.setProperty("error", "TENANT_CONTEXT_MISSING");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(TenantHeaderJwtMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTenantMismatch(TenantHeaderJwtMismatchException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        body.setProperty("error", "TENANT_HEADER_JWT_MISMATCH");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("invalid payload");
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        body.setProperty("error", "ONBOARDING_INVALID_PAYLOAD");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String errorCode = (message != null && message.contains("OnboardingStepId"))
                ? "ONBOARDING_INVALID_STEP_ID"
                : "ONBOARDING_INVALID_PAYLOAD";
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid payload: " + (message == null ? "malformed" : message));
        body.setProperty("error", errorCode);
        return ResponseEntity.badRequest().body(body);
    }
}
