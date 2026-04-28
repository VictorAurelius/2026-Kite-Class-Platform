package com.kitehub.gateway.filter;

import com.kitehub.gateway.config.KeyResolverConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RateLimitMetricsFilter} (GAP-259).
 */
@DisplayName("RateLimitMetricsFilter")
class RateLimitMetricsFilterTest {

    private MeterRegistry registry;
    private RateLimitMetricsFilter filter;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        filter = new RateLimitMetricsFilter(registry);
    }

    @Test
    @DisplayName("non-429 response → no counter increment")
    void noopOnNon429() {
        MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        StepVerifier.create(filter.filter(exchange, passthroughChain())).verifyComplete();

        assertThat(registry.find(RateLimitMetricsFilter.COUNTER_NAME).counters()).isEmpty();
    }

    @Test
    @DisplayName("429 with X-Instance-Subdomain → counter tagged key_type=tenant + tenant=<subdomain>")
    void counterTaggedTenant() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://localhost/api/platform/branding/ai/analyze-logo")
                .header(KeyResolverConfig.X_INSTANCE_SUBDOMAIN_HEADER, "schoolA")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

        StepVerifier.create(filter.filter(exchange, passthroughChain())).verifyComplete();

        double count = registry.counter(RateLimitMetricsFilter.COUNTER_NAME,
                "key_type", "tenant", "tenant", "schoolA").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("429 with X-API-Key → counter tagged key_type=apikey")
    void counterTaggedApiKey() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://localhost/api/m2m/foo")
                .header(KeyResolverConfig.X_API_KEY_HEADER, "k_live_xyz")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

        StepVerifier.create(filter.filter(exchange, passthroughChain())).verifyComplete();

        double count = registry.counter(RateLimitMetricsFilter.COUNTER_NAME,
                "key_type", "apikey", "tenant", "unknown").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("429 without tenant or apikey headers → key_type=ip + tenant=unknown")
    void counterTaggedIpFallback() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://localhost/api/auth/register")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

        StepVerifier.create(filter.filter(exchange, passthroughChain())).verifyComplete();

        double count = registry.counter(RateLimitMetricsFilter.COUNTER_NAME,
                "key_type", "ip", "tenant", "unknown").count();
        assertThat(count).isEqualTo(1.0);
    }

    private GatewayFilterChain passthroughChain() {
        return exchange -> Mono.empty();
    }
}
