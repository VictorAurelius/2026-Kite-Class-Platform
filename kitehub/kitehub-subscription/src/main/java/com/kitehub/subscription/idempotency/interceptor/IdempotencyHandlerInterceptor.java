package com.kitehub.subscription.idempotency.interceptor;

import com.kitehub.subscription.idempotency.entity.IdempotencyKey;
import com.kitehub.subscription.idempotency.exception.IdempotencyConflictException;
import com.kitehub.subscription.idempotency.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;

/**
 * Stripe-style idempotency interceptor (GAP-536 Wave onboarding-polish-2 Bucket C).
 *
 * <p>Reads the {@code Idempotency-Key} header on mutation endpoints (currently
 * POST {@code /api/platform/instances}). Behavior:
 * <ul>
 *   <li>Header absent → proceeds normally (no caching, no replay)</li>
 *   <li>Header present + cache hit + body hash match → replay cached response,
 *       short-circuit handler</li>
 *   <li>Header present + cache hit + body hash mismatch → 422 idempotency
 *       conflict (Stripe semantics)</li>
 *   <li>Header present + cache miss → run handler, then cache the response in
 *       {@code afterCompletion}</li>
 * </ul></p>
 *
 * <p><strong>Implementation note:</strong> request body is consumed by handler,
 * so we rely on {@code ContentCachingRequestWrapper} (registered upstream via
 * a Spring filter) to read the raw body for hashing without disturbing the
 * downstream {@code @RequestBody} parse. Response body capture similarly uses
 * {@code ContentCachingResponseWrapper}.</p>
 *
 * @since Wave onboarding-polish-2 Bucket C — GAP-536
 */
@Slf4j
@Component
public class IdempotencyHandlerInterceptor implements HandlerInterceptor {

    /** Header name per Stripe convention. */
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    /** Logical endpoint identifier passed to {@link IdempotencyService}. */
    public static final String ENDPOINT_POST_INSTANCES = "POST_instances";

    /** Request attribute carrying the resolved key for {@code afterCompletion}. */
    private static final String ATTR_KEY = "idempotencyKey";

    /** Request attribute carrying the request-body SHA-256 hash. */
    private static final String ATTR_HASH = "idempotencyHash";

    private final IdempotencyService idempotencyService;

    public IdempotencyHandlerInterceptor(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String key = request.getHeader(HEADER_IDEMPOTENCY_KEY);
        if (key == null || key.isBlank()) {
            return true;
        }

        // Read raw body for hashing. Requires ContentCachingRequestWrapper upstream.
        String body = extractBody(request);
        String requestHash = IdempotencyService.hashRequest(body);

        try {
            var replay = idempotencyService.findValidReplay(key, ENDPOINT_POST_INSTANCES, requestHash);
            if (replay.isPresent()) {
                writeCachedResponse(response, replay.get());
                return false;
            }
        } catch (IdempotencyConflictException ex) {
            log.warn("Idempotency conflict on POST /api/platform/instances: key={} reason={}",
                    key, ex.getMessage());
            writeConflictResponse(response, ex.getMessage());
            return false;
        }

        // Cache miss — store key + hash for afterCompletion to persist response.
        request.setAttribute(ATTR_KEY, key);
        request.setAttribute(ATTR_HASH, requestHash);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) throws Exception {
        String key = (String) request.getAttribute(ATTR_KEY);
        String hash = (String) request.getAttribute(ATTR_HASH);
        if (key == null || hash == null) {
            return;
        }

        int status = response.getStatus();
        // Only cache successful (2xx) responses; errors should remain retryable.
        if (status < 200 || status >= 300) {
            return;
        }
        if (ex != null) {
            // Handler raised — don't poison the cache.
            return;
        }

        String responseBody = extractResponseBody(response);
        try {
            idempotencyService.cacheResponse(key, ENDPOINT_POST_INSTANCES, hash, status, responseBody);
        } catch (Exception cacheEx) {
            // Cache best-effort — never break the response Spring already wrote.
            log.warn("Failed to cache idempotency response: key={} reason={}", key, cacheEx.getMessage());
        }
    }

    private String extractBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            return new String(buf, StandardCharsets.UTF_8);
        }
        // Fallback: caller didn't wrap. Hash empty payload — still allows key
        // tracking but loses body-mismatch protection. Log so ops can wire the
        // wrapper if this endpoint surfaces a real conflict scenario.
        log.debug("Request not ContentCachingRequestWrapper — body hash will be empty");
        return "";
    }

    private String extractResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            return new String(buf, StandardCharsets.UTF_8);
        }
        return "";
    }

    private void writeCachedResponse(HttpServletResponse response, IdempotencyKey cached) throws java.io.IOException {
        response.setStatus(cached.getResponseStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("X-Idempotency-Replay", "true");
        response.getWriter().write(cached.getResponseBody());
    }

    private void writeConflictResponse(HttpServletResponse response, String message) throws java.io.IOException {
        response.setStatus(422);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String escaped = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write(
                "{\"error\":\"idempotency_conflict\",\"message\":\"" + escaped + "\"}");
    }
}
