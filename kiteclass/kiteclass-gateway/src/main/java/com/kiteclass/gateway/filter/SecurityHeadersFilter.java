package com.kiteclass.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Adds OWASP-recommended security headers to all responses.
 *
 * <p>Extended 2026-05-11 (GAP-472 — Wave 61 Bucket E) to include HSTS,
 * Content-Security-Policy, and Permissions-Policy alongside the original
 * X-Content-Type-Options / X-Frame-Options / X-XSS-Protection / Referrer-Policy
 * set introduced 2026-03-24.</p>
 *
 * <p>CSP is intentionally strict ({@code default-src 'none'; frame-ancestors 'none'})
 * because this gateway only serves JSON API responses; FE-served HTML uses a
 * separate (looser) CSP defined in {@code vercel.json}.</p>
 *
 * @since 2026-03-24 (HSTS+CSP+Permissions-Policy added 2026-05-11)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityHeadersFilter implements WebFilter {

    static final String HSTS_VALUE = "max-age=31536000; includeSubDomains; preload";
    static final String CSP_VALUE = "default-src 'none'; frame-ancestors 'none';";
    static final String PERMISSIONS_VALUE = "geolocation=(), microphone=(), camera=()";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        // Strict-Transport-Security — force HTTPS for 1 year, includeSubDomains, preload-ready.
        headers.set("Strict-Transport-Security", HSTS_VALUE);
        // Content-Security-Policy — JSON-API strict; FE sets its own CSP via vercel.json.
        headers.set("Content-Security-Policy", CSP_VALUE);
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("X-XSS-Protection", "1; mode=block");
        headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
        // Permissions-Policy — disable powerful features at the API surface.
        headers.set("Permissions-Policy", PERMISSIONS_VALUE);
        return chain.filter(exchange);
    }
}
