package com.kitehub.admin.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Adds RFC 8594 deprecation headers to legacy {@code /api/platform/admin/*} responses (GAP-654).
 *
 * <p>The legacy {@link com.kitehub.admin.controller.AdminController} surface is {@code @Deprecated}
 * (forRemoval) in favour of the canonical {@code /api/v1/admin/*} controllers. This interceptor
 * advertises that deprecation to clients per RFC 8594 (Sunset HTTP Header) so consumers can detect
 * the migration window programmatically:</p>
 *
 * <ul>
 *   <li>{@code Sunset: Sat, 30 Sep 2026 23:59:59 GMT} — the date after which the legacy path may be removed.</li>
 *   <li>{@code Link: </api/v1/admin/...>; rel="successor-version"} — the canonical replacement path,
 *       derived by mapping the {@code /api/platform/admin} prefix to {@code /api/v1/admin}.</li>
 *   <li>{@code Deprecation: true} — RFC 8594 companion signal that the resource is deprecated.</li>
 * </ul>
 *
 * <p>Registered for the {@code /api/platform/admin/**} path pattern only (see
 * {@link AdminWebMvcConfig}); v1 paths are unaffected.</p>
 *
 * @since 1.0
 */
public class SunsetHeaderInterceptor implements HandlerInterceptor {

    /** RFC 8594 Sunset date for the legacy {@code /api/platform/admin} surface. */
    static final String SUNSET_DATE = "Sat, 30 Sep 2026 23:59:59 GMT";

    private static final String LEGACY_PREFIX = "/api/platform/admin";
    private static final String SUCCESSOR_PREFIX = "/api/v1/admin";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", SUNSET_DATE);
        response.setHeader("Link", buildSuccessorLink(request.getRequestURI()));
        return true;
    }

    /**
     * Build the {@code Link: rel="successor-version"} header value by remapping the legacy admin
     * prefix to the canonical v1 prefix; falls back to the v1 root when the URI does not match.
     */
    private String buildSuccessorLink(String requestUri) {
        String successorPath = SUCCESSOR_PREFIX;
        if (requestUri != null && requestUri.startsWith(LEGACY_PREFIX)) {
            successorPath = SUCCESSOR_PREFIX + requestUri.substring(LEGACY_PREFIX.length());
        }
        return "<" + successorPath + ">; rel=\"successor-version\"";
    }
}
