package com.kitehub.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Populates SLF4J MDC with the contextual fields required by {@code logs-format-standard.md} §2.2:
 * {@code tenantId}, {@code traceId}, {@code spanId}, {@code userId}, {@code requestId}.
 *
 * <p>Reads from (in order of precedence):</p>
 * <ol>
 *   <li>HTTP headers — {@code X-Tenant-Id}, {@code X-User-Id}, {@code X-Trace-Id},
 *       {@code traceparent} (W3C), {@code X-Request-Id}</li>
 *   <li>Spring Security {@link Authentication} — username + JWT claims if available</li>
 * </ol>
 *
 * <p>Always clears MDC in {@code finally} to prevent leakage across pooled threads.</p>
 *
 * <p>Filter order: register near the top of the chain (after authentication so
 * SecurityContext is populated, before request dispatch).</p>
 */
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String MDC_REQUEST_ID = "requestId";

    public static final String HDR_TENANT_ID = "X-Tenant-Id";
    public static final String HDR_USER_ID = "X-User-Id";
    public static final String HDR_TRACE_ID = "X-Trace-Id";
    public static final String HDR_REQUEST_ID = "X-Request-Id";
    public static final String HDR_TRACEPARENT = "traceparent";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String traceId = headerOr(request, HDR_TRACE_ID, null);
            if (traceId == null) {
                traceId = parseTraceparent(request.getHeader(HDR_TRACEPARENT));
            }
            if (traceId == null) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
            put(MDC_TRACE_ID, traceId);

            String requestId = headerOr(request, HDR_REQUEST_ID,
                "req-" + traceId.substring(0, Math.min(8, traceId.length())));
            put(MDC_REQUEST_ID, requestId);

            String tenantId = headerOr(request, HDR_TENANT_ID, null);
            String userId = headerOr(request, HDR_USER_ID, null);

            // Fall back to SecurityContext if header missing
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                if (userId == null && auth.getName() != null) {
                    userId = auth.getName();
                }
                // tenantId can come from JWT claims via custom principal — leave to per-service
                // override; default behavior is header-driven.
            }

            if (tenantId != null) {
                put(MDC_TENANT_ID, tenantId);
            }
            if (userId != null) {
                put(MDC_USER_ID, userId);
            }

            // Propagate traceId to downstream services via response header for symmetry.
            response.setHeader(HDR_TRACE_ID, traceId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_SPAN_ID);
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private static void put(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    private static String headerOr(HttpServletRequest request, String name, String fallback) {
        String v = request.getHeader(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    /**
     * W3C {@code traceparent: 00-<trace-id>-<parent-id>-<flags>}. Returns the trace-id
     * portion or {@code null} if header malformed.
     */
    private static String parseTraceparent(String tp) {
        if (tp == null) {
            return null;
        }
        String[] parts = tp.split("-");
        if (parts.length < 3 || parts[1].length() != 32) {
            return null;
        }
        return parts[1];
    }
}
