package com.kitehub.subscription.idempotency.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Wraps requests + responses to {@code POST /api/platform/instances} so that
 * {@link IdempotencyHandlerInterceptor} can read raw body (request) and the
 * eventual response body (response) without disturbing the Spring MVC pipeline.
 *
 * <p>The wrappers buffer bytes; only enabled on the idempotency-scoped path
 * to avoid memory cost on unrelated traffic. After the chain completes, the
 * response wrapper's {@code copyBodyToResponse()} must run so the cached bytes
 * actually reach the client.</p>
 *
 * @since Wave onboarding-polish-2 Bucket C — GAP-536
 */
@Component
public class IdempotencyCachingFilter extends OncePerRequestFilter {

    private static final String SCOPED_PATH = "/api/platform/instances";

    /** Cap body cache at 64 KB — well above any reasonable CreateInstance JSON. */
    private static final int MAX_PAYLOAD_BYTES = 64 * 1024;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only wrap POST /api/platform/instances exactly (not sub-resources).
        return !("POST".equalsIgnoreCase(request.getMethod()) && SCOPED_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedReq =
                request instanceof ContentCachingRequestWrapper req
                        ? req
                        : new ContentCachingRequestWrapper(request, MAX_PAYLOAD_BYTES);
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
