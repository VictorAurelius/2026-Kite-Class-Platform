package com.kitehub.subscription.impersonation;

import com.kitehub.subscription.impersonation.dto.ImpersonationAuditEntryDto;
import com.kitehub.subscription.impersonation.dto.ImpersonationEndResponse;
import com.kitehub.subscription.impersonation.dto.ImpersonationStartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for admin "View as tenant" impersonation (GAP-040 Wave 79
 * Bucket F-bis).
 *
 * <p>3 endpoints, all guarded by {@code hasRole('PLATFORM_ADMIN')}:</p>
 * <ul>
 *   <li>{@code POST /api/v1/admin/impersonate/{tenantSlug}} — start session,
 *       returns scoped JWT bearing tenant claim + impersonated_by claim with
 *       30-second TTL.</li>
 *   <li>{@code POST /api/v1/admin/impersonate/end} — manual exit, marks audit
 *       row {@code MANUAL_EXIT}.</li>
 *   <li>{@code GET  /api/v1/admin/impersonate/audit-log} — paginated history
 *       for the admin audit panel UI.</li>
 * </ul>
 *
 * <p>Authentication: gateway forwards {@code X-User-Id} + {@code X-User-Roles}
 * which {@link com.kitehub.subscription.config.SecurityConfig.XUserRolesHeaderFilter}
 * translates to {@code ROLE_PLATFORM_ADMIN} authority — same pattern as
 * {@link com.kitehub.subscription.beta.controller.BetaAccessController}.</p>
 *
 * @since Wave 79 (GAP-040)
 */
@RestController
@RequestMapping("/api/v1/admin/impersonate")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Impersonation", description = "GAP-040: admin 'View as tenant' support tool (30s sessions + audit log)")
public class ImpersonationController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ImpersonationService service;

    @Operation(summary = "Start an impersonation session",
               description = "Mints a scoped JWT bearing tenant_id + impersonated_by claims with a 30-second TTL. "
                       + "Audit-log row inserted in the same transaction as the token mint — failure to persist "
                       + "the row prevents the token from being returned. Caller MUST have PLATFORM_ADMIN role.")
    @PostMapping("/{tenantSlug}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ImpersonationStartResponse> start(
            @PathVariable String tenantSlug,
            Authentication authentication,
            HttpServletRequest request) {

        UUID adminUserId = resolveAdminUserId(authentication);
        ImpersonationStartResponse resp = service.startImpersonation(
                adminUserId,
                tenantSlug,
                resolveClientIp(request),
                request.getHeader("User-Agent"));
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "End the admin's current impersonation session",
               description = "Marks the active audit row with ended_at + ended_reason=MANUAL_EXIT. "
                       + "Returns 404 if no active session exists for the caller.")
    @PostMapping("/end")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ImpersonationEndResponse> end(Authentication authentication) {
        UUID adminUserId = resolveAdminUserId(authentication);
        return ResponseEntity.ok(service.endImpersonation(adminUserId));
    }

    @Operation(summary = "List impersonation audit-log entries",
               description = "Paginated, newest-first. Visible only to PLATFORM_ADMIN role.")
    @GetMapping("/audit-log")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Page<ImpersonationAuditEntryDto>> auditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<ImpersonationAuditEntryDto> dtoPage = service.listAuditLog(pageable)
                .map(ImpersonationAuditEntryDto::from);
        return ResponseEntity.ok(dtoPage);
    }

    private UUID resolveAdminUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Missing authenticated principal — expected gateway-forwarded X-User-Id");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Authentication principal is not a UUID: " + authentication.getName(), ex);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Take first IP if X-Forwarded-For has comma-separated list
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
