package com.kitehub.subscription.idempotency.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Set;

/**
 * Wraps requests + responses to idempotency-scoped POST endpoints so that
 * {@link IdempotencyHandlerInterceptor} can read raw body (request) and the
 * eventual response body (response) without disturbing the Spring MVC pipeline.
 *
 * <p>Scoped paths (extended Wave local-doable-10 Bucket A — GAP-730):
 * <ul>
 *   <li>{@code POST /api/platform/instances} (GAP-536)</li>
 *   <li>{@code POST /api/auth/register} (GAP-730 signup)</li>
 *   <li>{@code POST /api/v1/auth/request-beta-access} (GAP-730 beta-request)</li>
 * </ul></p>
 *
 * <p>The request is wrapped in {@link CachedBodyHttpServletRequest} — a
 * fully-buffered re-readable wrapper — so the interceptor's {@code preHandle}
 * body hash and the downstream {@code @RequestBody} parse both read the SAME
 * bytes. (We previously used {@code ContentCachingRequestWrapper}; its byte
 * cache is empty at {@code preHandle} time → every payload hashed identically →
 * same-key/different-body 422 conflict never fired. GAP-536 live verify
 * 2026-06-02.) The response wrapper buffers bytes; only enabled on the
 * idempotency-scoped paths to avoid memory cost on unrelated traffic. After the
 * chain completes, {@code copyBodyToResponse()} must run so the cached bytes
 * actually reach the client.</p>
 *
 * @since Wave onboarding-polish-2 Bucket C — GAP-536
 */
@Component
public class IdempotencyCachingFilter extends OncePerRequestFilter {

    private static final Set<String> SCOPED_PATHS = Set.of(
            "/api/platform/instances",
            "/api/auth/register",
            "/api/v1/auth/request-beta-access"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only wrap POST + exact-match scoped paths (not sub-resources).
        return !("POST".equalsIgnoreCase(request.getMethod())
                && SCOPED_PATHS.contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest wrappedReq =
                request instanceof CachedBodyHttpServletRequest
                        ? request
                        : new CachedBodyHttpServletRequest(request);
        ContentCachingResponseWrapper wrappedRes =
                response instanceof ContentCachingResponseWrapper res ? res : new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(wrappedReq, wrappedRes);
        } finally {
            // Must copy buffered response back to actual response stream.
            wrappedRes.copyBodyToResponse();
        }
    }
}
