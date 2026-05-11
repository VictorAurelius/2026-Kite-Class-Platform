package com.kiteclass.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for SecurityHeadersFilter.
 *
 * <p>Extended 2026-05-11 (GAP-472 — Wave 61 Bucket E) to verify HSTS,
 * Content-Security-Policy, and Permissions-Policy in addition to the original
 * four headers.</p>
 *
 * @since 2026-03-24
 */
class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    @DisplayName("Should add all 7 security headers (HSTS+CSP+4 original+Permissions-Policy)")
    void filter_addsSecurityHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = filterExchange -> Mono.empty();

        filter.filter(exchange, chain).block();

        var headers = exchange.getResponse().getHeaders();
        assertEquals(SecurityHeadersFilter.HSTS_VALUE,
            headers.getFirst("Strict-Transport-Security"));
        assertEquals(SecurityHeadersFilter.CSP_VALUE,
            headers.getFirst("Content-Security-Policy"));
        assertEquals("nosniff",
            headers.getFirst("X-Content-Type-Options"));
        assertEquals("DENY",
            headers.getFirst("X-Frame-Options"));
        assertEquals("1; mode=block",
            headers.getFirst("X-XSS-Protection"));
        assertEquals("strict-origin-when-cross-origin",
            headers.getFirst("Referrer-Policy"));
        assertEquals(SecurityHeadersFilter.PERMISSIONS_VALUE,
            headers.getFirst("Permissions-Policy"));
    }
}
