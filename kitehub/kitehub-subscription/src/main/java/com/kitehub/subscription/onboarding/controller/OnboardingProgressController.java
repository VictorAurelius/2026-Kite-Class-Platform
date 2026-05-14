package com.kitehub.subscription.onboarding.controller;

import com.kitehub.subscription.onboarding.dto.OnboardingProgressResponse;
import com.kitehub.subscription.onboarding.dto.OnboardingProgressUpdateCommand;
import com.kitehub.subscription.onboarding.exception.TenantContextMissingException;
import com.kitehub.subscription.onboarding.service.OnboardingProgressService;
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

    @GetMapping
    @Operation(summary = "Get onboarding progress for current tenant (lazy-init)")
    public ResponseEntity<OnboardingProgressResponse> getProgress(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        UUID tenantId = resolveTenant(tenantHeader);
        return ResponseEntity.ok(service.getProgress(tenantId));
    }

    @PutMapping
    @Operation(summary = "Update one step completion (idempotent on equal value)")
    public ResponseEntity<OnboardingProgressResponse> updateStep(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @Valid @RequestBody OnboardingProgressUpdateCommand command
    ) {
        UUID tenantId = resolveTenant(tenantHeader);
        return ResponseEntity.ok(service.updateStep(tenantId, command));
    }

    private UUID resolveTenant(String tenantHeader) {
        if (tenantHeader == null || tenantHeader.isBlank()) {
            throw new TenantContextMissingException("X-Tenant-Id header missing");
        }
        try {
            return UUID.fromString(tenantHeader);
        } catch (IllegalArgumentException ex) {
            throw new TenantContextMissingException("X-Tenant-Id header malformed (not a UUID)");
        }
    }

    // ── Exception handlers (problem+json error envelope) ──

    @ExceptionHandler(TenantContextMissingException.class)
    public ResponseEntity<ProblemDetail> handleTenantMissing(TenantContextMissingException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        body.setProperty("error", "TENANT_CONTEXT_MISSING");
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
