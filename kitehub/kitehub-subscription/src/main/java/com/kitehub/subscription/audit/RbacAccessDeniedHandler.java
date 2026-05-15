package com.kitehub.subscription.audit;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Logs every RBAC 403 (privilege-escalation attempt) to {@link AdminAuditLog}
 * so security audit can review unauthorized access patterns.
 *
 * <p>GAP-562b (Wave 80 Bucket C): when a STAFF user hits an OWNER-only
 * endpoint, Spring Security throws {@link AccessDeniedException}. This
 * handler intercepts the exception and persists a row capturing:</p>
 * <ul>
 *   <li>{@code admin_user_id}: caller user ID from {@code X-User-Id} header
 *     (best-effort; falls back to a zero UUID when absent).</li>
 *   <li>{@code action}: {@code RBAC_DENIED:<HTTP_METHOD>:<endpoint>}.</li>
 *   <li>{@code request_ip}, {@code user_agent}: request provenance.</li>
 *   <li>{@code success=false} + {@code error_message}: denial reason.</li>
 * </ul>
 *
 * <p>Wired in {@link com.kitehub.subscription.config.SecurityConfig}'s
 * {@code exceptionHandling} so it fires on EVERY {@code @PreAuthorize} 403.
 * The audit write is best-effort: write failure is logged but never
 * propagates (we always still return 403 to the caller).</p>
 *
 * @since Wave 80 — GAP-562b
 */
@Component
@Profile("!test")
public class RbacAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RbacAccessDeniedHandler.class);
    private static final UUID UNKNOWN_USER_ID = new UUID(0L, 0L);
    private static final int MAX_PAYLOAD_LEN = 1024;

    private final AdminAuditLogRepository auditRepo;

    public RbacAccessDeniedHandler(AdminAuditLogRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        try {
            persistAuditEntry(request, accessDeniedException);
        } catch (Exception ex) {
            // Best-effort: never propagate audit failures.
            log.warn("Failed to write RBAC denial audit row: {}", ex.getMessage());
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"FORBIDDEN\",\"message\":\"Insufficient role for this resource\"}");
    }

    private void persistAuditEntry(HttpServletRequest request,
                                   AccessDeniedException ex) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = resolveUserId(auth, request);

        String action = String.format("RBAC_DENIED:%s:%s",
                safe(request.getMethod()),
                safe(request.getRequestURI()));

        String payload = String.format(
                "{\"deniedRoles\":\"%s\",\"reason\":\"%s\"}",
                jsonEscape(authoritiesString(auth)),
                jsonEscape(truncate(ex.getMessage(), 256)));

        AdminAuditLog entry = AdminAuditLog.builder()
                .adminUserId(userId)
                .action(truncate(action, 64))
                .targetEntityType("HTTP_ENDPOINT")
                .targetEntityId(truncate(request.getRequestURI(), 128))
                .requestIp(truncate(extractIp(request), 64))
                .userAgent(truncate(request.getHeader("User-Agent"), 512))
                .payloadJson(truncate(payload, MAX_PAYLOAD_LEN))
                .success(false)
                .errorMessage(truncate(ex.getMessage(), 1024))
                .build();

        auditRepo.save(entry);
    }

    private static UUID resolveUserId(Authentication auth, HttpServletRequest req) {
        if (auth != null && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignore) {
                // fall through
            }
        }
        String header = req.getHeader("X-User-Id");
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignore) {
                // fall through
            }
        }
        return UNKNOWN_USER_ID;
    }

    private static String authoritiesString(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return "";
        return auth.getAuthorities().toString();
    }

    private static String extractIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
