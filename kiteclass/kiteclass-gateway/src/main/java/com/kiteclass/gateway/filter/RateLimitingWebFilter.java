package com.kiteclass.gateway.filter;

import com.kiteclass.gateway.config.RateLimitingProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web filter for rate limiting using Bucket4j.
 * <p>
 * Applies to ALL HTTP requests (including local controller endpoints and proxied routes).
 * <p>
 * Implements token bucket algorithm with different limits:
 * <ul>
 *   <li>Unauthenticated users (by IP): 100 requests/minute</li>
 *   <li>Authenticated users (by user ID): 1000 requests/minute</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.6.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Run early in the filter chain, but after CORS
public class RateLimitingWebFilter implements WebFilter {

    private final RateLimitingProperties properties;

    // In-memory cache for buckets (consider Redis for distributed systems)
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String X_RATE_LIMIT_RETRY_AFTER = "X-Rate-Limit-Retry-After-Seconds";

    /**
     * Apply rate limiting to the request.
     * <p>
     * Checks if the request exceeds the configured rate limit based on IP address or user ID.
     * Returns 429 Too Many Requests if rate limit is exceeded, otherwise continues the filter chain.
     *
     * @param exchange the current server web exchange
     * @param chain the web filter chain to continue
     * @return a Mono that completes when the filter processing is done
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String key = getRateLimitKey(request);
        boolean isAuthenticated = isAuthenticatedRequest(request);

        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(isAuthenticated));

        long availableTokens = bucket.getAvailableTokens();

        if (bucket.tryConsume(1)) {
            // Request allowed
            addRateLimitHeaders(exchange.getResponse(), availableTokens - 1);
            log.debug("Rate limit check passed for key: {} (remaining: {})", key, availableTokens - 1);
            return chain.filter(exchange);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for key: {}", key);
            return rateLimitExceeded(exchange);
        }
    }

    /**
     * Get rate limit key based on user ID or IP address.
     *
     * @param request HTTP request
     * @return rate limit key
     */
    private String getRateLimitKey(ServerHttpRequest request) {
        // Check for authenticated user
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null) {
            return "user:" + userId;
        }

        // Fall back to IP address
        String ipAddress = getClientIpAddress(request);
        return "ip:" + ipAddress;
    }

    /**
     * Check if request is authenticated.
     *
     * @param request HTTP request
     * @return true if request has valid Authorization header
     */
    private boolean isAuthenticatedRequest(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return authHeader != null && authHeader.startsWith(BEARER_PREFIX);
    }

    /**
     * Get client IP address from request.
     *
     * @param request HTTP request
     * @return client IP address
     */
    private String getClientIpAddress(ServerHttpRequest request) {
        // Check X-Forwarded-For header (for proxies)
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        // Fall back to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    /**
     * Create bucket with appropriate capacity based on authentication status.
     *
     * @param isAuthenticated true if user is authenticated
     * @return configured bucket
     */
    private Bucket createBucket(boolean isAuthenticated) {
        long capacity = isAuthenticated
                ? properties.getAuthenticatedRequestsPerMinute()
                : properties.getUnauthenticatedRequestsPerMinute();

        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, Duration.ofSeconds(properties.getTimeWindowSeconds()))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Add rate limit headers to response.
     *
     * @param response HTTP response
     * @param remaining remaining requests
     */
    private void addRateLimitHeaders(ServerHttpResponse response, long remaining) {
        response.getHeaders().add(X_RATE_LIMIT_REMAINING, String.valueOf(Math.max(0, remaining)));
    }

    /**
     * Handle rate limit exceeded.
     *
     * @param exchange server web exchange
     * @return Mono of Void
     */
    private Mono<Void> rateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add(X_RATE_LIMIT_REMAINING, "0");
        response.getHeaders().add(X_RATE_LIMIT_RETRY_AFTER, String.valueOf(properties.getTimeWindowSeconds()));

        String errorBody = "{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBody.getBytes())));
    }
}
