package com.kitehub.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SecurityHeadersFilter} (GAP-472).
 */
@DisplayName("SecurityHeadersFilter (kitehub-gateway)")
class SecurityHeadersFilterTest {

    private SecurityHeadersFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
    }

    @Test
    @DisplayName("response carries all 6 security headers after filter completes")
    void allSixHeadersPresent() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://localhost/api/v1/health"));

        StepVerifier.create(filter.filter(exchange, passthroughChain())).verifyComplete();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getFirst(SecurityHeadersFilter.HSTS))
                .isEqualTo(SecurityHeadersFilter.HSTS_VALUE);
        assertThat(headers.getFirst(SecurityHeadersFilter.CSP))
                .isEqualTo(SecurityHeadersFilter.CSP_VALUE);
        assertThat(headers.getFirst(SecurityHeadersFilter.X_CONTENT_TYPE_OPTIONS))
                .isEqualTo("nosniff");
        assertThat(headers.getFirst(SecurityHeadersFilter.X_FRAME_OPTIONS))
                .isEqualTo("DENY");
        assertThat(headers.getFirst(SecurityHeadersFilter.REFERRER_POLICY))
                .isEqualTo("strict-origin-when-cross-origin");
        assertThat(headers.getFirst(SecurityHeadersFilter.PERMISSIONS_POLICY))
                .isEqualTo(SecurityHeadersFilter.PERMISSIONS_VALUE);
    }

    @Test
    @DisplayName("filter overrides any upstream-set value (e.g. weaker X-Frame-Options)")
    void overridesUpstreamHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://localhost/api/v1/foo"));
        // Simulate an upstream service setting a weaker value.
        exchange.getResponse().getHeaders().set(SecurityHeadersFilter.X_FRAME_OPTIONS, "SAMEORIGIN");

        StepVerifier.create(filter.filter(exchange, passthroughChain())).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(SecurityHeadersFilter.X_FRAME_OPTIONS))
                .isEqualTo("DENY");
    }

    @Test
    @DisplayName("filter order is LOWEST_PRECEDENCE - 1 so it runs after upstream filters")
    void orderRunsLast() {
        assertThat(filter.getOrder()).isEqualTo(org.springframework.core.Ordered.LOWEST_PRECEDENCE - 1);
    }

    private GatewayFilterChain passthroughChain() {
        return exchange -> Mono.empty();
    }
}
