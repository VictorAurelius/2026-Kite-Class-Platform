package com.kitehub.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Injects OWASP-recommended security headers into every response.
 *
 * <p>Closes GAP-472 — kitehub-gateway previously had no SecurityHeadersFilter. Adds
 * defense-in-depth headers covering HSTS, CSP, frame protection, MIME sniffing,
 * referrer policy, and Permissions-Policy.</p>
 *
 * <p>CSP for the gateway is intentionally strict ({@code default-src 'none';
 * frame-ancestors 'none'}) because the gateway only serves JSON API responses;
 * static assets and HTML are served by the Vercel-hosted frontend, which sets
 * its own (looser) CSP via {@code vercel.json}.</p>
 *
 * @since 1.2.0 (GAP-472 — Wave 61 Bucket E)
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    static final String HSTS = "Strict-Transport-Security";
    static final String CSP = "Content-Security-Policy";
    static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    static final String X_FRAME_OPTIONS = "X-Frame-Options";
    static final String REFERRER_POLICY = "Referrer-Policy";
    static final String PERMISSIONS_POLICY = "Permissions-Policy";

    static final String HSTS_VALUE = "max-age=31536000; includeSubDomains; preload";
    static final String CSP_VALUE = "default-src 'none'; frame-ancestors 'none';";
    static final String PERMISSIONS_VALUE = "geolocation=(), microphone=(), camera=()";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            // Use put to avoid duplicates if an upstream service also sets the header.
            headers.set(HSTS, HSTS_VALUE);
            headers.set(CSP, CSP_VALUE);
            headers.set(X_CONTENT_TYPE_OPTIONS, "nosniff");
            headers.set(X_FRAME_OPTIONS, "DENY");
            headers.set(REFERRER_POLICY, "strict-origin-when-cross-origin");
            headers.set(PERMISSIONS_POLICY, PERMISSIONS_VALUE);
        }));
    }

    @Override
    public int getOrder() {
        // Run last so we overwrite any header set by upstream services. Using
        // LOWEST_PRECEDENCE - 1 keeps RateLimitMetricsFilter at LOWEST_PRECEDENCE,
        // which still fires after this filter's response-write hook completes.
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
