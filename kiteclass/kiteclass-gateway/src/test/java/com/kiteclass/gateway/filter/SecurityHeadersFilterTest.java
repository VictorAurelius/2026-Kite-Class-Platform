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
 * @since 2026-03-24
 */
class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    @DisplayName("Should add all security headers to response")
    void filter_addsSecurityHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = filterExchange -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertEquals("nosniff",
            exchange.getResponse().getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("DENY",
            exchange.getResponse().getHeaders().getFirst("X-Frame-Options"));
        assertEquals("1; mode=block",
            exchange.getResponse().getHeaders().getFirst("X-XSS-Protection"));
        assertEquals("strict-origin-when-cross-origin",
            exchange.getResponse().getHeaders().getFirst("Referrer-Policy"));
    }
}
