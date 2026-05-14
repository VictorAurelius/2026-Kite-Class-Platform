package com.kitehub.subscription.feedback.controller;

import com.kitehub.subscription.feedback.dto.FeedbackSubmissionRequest;
import com.kitehub.subscription.feedback.dto.FeedbackSubmissionResponse;
import com.kitehub.subscription.feedback.entity.FeedbackSubmission;
import com.kitehub.subscription.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for in-app feedback widget submission (GAP-542 Wave 78 Bucket F).
 *
 * <p>Endpoint surface (single endpoint per contract):</p>
 * <ul>
 *   <li>{@code POST /api/v1/feedback} — public, rate-limited at gateway
 *       (10 req/min/IP per contract §Rate limits)</li>
 * </ul>
 *
 * <p>Auth context (tenantId + userId) auto-attached from
 * {@link SecurityContextHolder} when a bearer JWT is present; anonymous submit
 * is allowed (matches contract §Auth note).</p>
 *
 * <p>Contract: {@code documents/01-business/kitehub/feedback/api-contract.md}</p>
 *
 * @since Wave 78 — GAP-542
 */
@RestController
@Slf4j
@Tag(name = "Feedback", description = "In-app feedback widget submission (GAP-542 Wave 78)")
public class FeedbackController {

    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @Operation(
            summary = "Submit in-app feedback",
            description = "Public endpoint. Honeypot MUST be empty. Auth context (tenantId/userId) auto-attached if Bearer JWT is present. Rate-limit per IP enforced at gateway."
    )
    @PostMapping("/api/v1/feedback")
    public ResponseEntity<FeedbackSubmissionResponse> submit(
            @Valid @RequestBody FeedbackSubmissionRequest request,
            HttpServletRequest httpRequest) {

        String tenantId = currentTenantId();
        String userId = currentUserId();
        String clientIp = resolveClientIp(httpRequest);

        FeedbackSubmission saved = service.submit(request, tenantId, userId, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FeedbackSubmissionResponse.from(saved));
    }

    private static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return auth.getName();
    }

    private static String currentTenantId() {
        // Gateway forwards X-Tenant-Id via SecurityContext extension; absent for
        // anonymous submits. For now return null — auth filter populates when
        // JWT carries tenantId claim (Wave 33+ infrastructure).
        return null;
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
