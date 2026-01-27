package com.kiteclass.gateway.filter;

import com.kiteclass.gateway.config.RateLimitingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RateLimitingFilter}.
 *
 * @author KiteClass Team
 * @since 1.6.0
 */
class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private RateLimitingProperties properties;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        properties = new RateLimitingProperties();
        properties.setEnabled(true);
        properties.setUnauthenticatedRequestsPerMinute(5); // Low limit for testing
        properties.setAuthenticatedRequestsPerMinute(10);
        properties.setTimeWindowSeconds(60);

        filter = new RateLimitingFilter(properties);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void shouldAllowRequestWithinRateLimit() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.apply(new RateLimitingFilter.Config()).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull(); // Not set (allowed)
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isNotNull();
    }

    @Test
    void shouldRejectRequestExceedingRateLimit() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();

        // When - Make requests exceeding the limit
        for (int i = 0; i < 6; i++) {
            ServerWebExchange exchange = MockServerWebExchange.from(request);
            filter.apply(new RateLimitingFilter.Config()).filter(exchange, chain).block();

            if (i < 5) {
                // First 5 requests should succeed
                assertThat(exchange.getResponse().getStatusCode()).isNotIn(HttpStatus.TOO_MANY_REQUESTS);
            } else {
                // 6th request should be rate limited
                assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
                assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Retry-After-Seconds")).isNotNull();
            }
        }
    }

    @Test
    void shouldUseHigherLimitForAuthenticatedUsers() {
        // Given - Authenticated request
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .header(HttpHeaders.AUTHORIZATION, "Bearer eyJhbGciOiJIUzUxMiJ9...")
                .build();

        // When - Make 6 requests (should all succeed with authenticated limit of 10)
        for (int i = 0; i < 6; i++) {
            ServerWebExchange exchange = MockServerWebExchange.from(request);
            Mono<Void> result = filter.apply(new RateLimitingFilter.Config()).filter(exchange, chain);

            StepVerifier.create(result)
                    .verifyComplete();

            // All requests should succeed (limit is 10)
            assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Test
    void shouldBypassRateLimitingWhenDisabled() {
        // Given
        properties.setEnabled(false);
        filter = new RateLimitingFilter(properties);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();

        // When - Make many requests
        for (int i = 0; i < 10; i++) {
            ServerWebExchange exchange = MockServerWebExchange.from(request);
            Mono<Void> result = filter.apply(new RateLimitingFilter.Config()).filter(exchange, chain);

            StepVerifier.create(result)
                    .verifyComplete();

            // All requests should succeed (rate limiting disabled)
            assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Test
    void shouldUseDifferentBucketsForDifferentIPs() {
        // Given
        MockServerHttpRequest request1 = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();

        MockServerHttpRequest request2 = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.2", 8080))
                .build();

        // When - Exhaust limit for IP 1
        for (int i = 0; i < 5; i++) {
            ServerWebExchange exchange = MockServerWebExchange.from(request1);
            filter.apply(new RateLimitingFilter.Config()).filter(exchange, chain).block();
        }

        // Then - IP 2 should still be allowed
        ServerWebExchange exchange2 = MockServerWebExchange.from(request2);
        Mono<Void> result = filter.apply(new RateLimitingFilter.Config()).filter(exchange2, chain);

        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange2.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldHandleXForwardedForHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .header("X-Forwarded-For", "203.0.113.1, 198.51.100.1")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.apply(new RateLimitingFilter.Config()).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Should use first IP from X-Forwarded-For
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
