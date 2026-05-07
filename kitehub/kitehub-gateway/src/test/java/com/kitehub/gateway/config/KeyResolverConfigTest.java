package com.kitehub.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KeyResolverConfig} (GAP-259).
 */
@DisplayName("KeyResolverConfig")
class KeyResolverConfigTest {

    private KeyResolverConfig config;

    @BeforeEach
    void setUp() {
        config = new KeyResolverConfig(".kiteclass.com");
    }

    @Test
    @DisplayName("ipKeyResolver — returns ip:<address> from remote address")
    void ipResolverReadsRemoteAddress() {
        KeyResolver resolver = config.ipKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://example.com/test")
                .remoteAddress(new java.net.InetSocketAddress("203.0.113.5", 0))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("ip:203.0.113.5")
                .verifyComplete();
    }

    @Test
    @DisplayName("ipKeyResolver — falls back to anon when remote address missing")
    void ipResolverFallsBackToAnon() {
        KeyResolver resolver = config.ipKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://example.com/test")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("ip:anon")
                .verifyComplete();
    }

    @Test
    @DisplayName("tenantKeyResolver — uses X-Instance-Subdomain header when present")
    void tenantResolverUsesHeader() {
        KeyResolver resolver = config.tenantKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://localhost:9000/api/platform/branding/ai/analyze-logo")
                .header(KeyResolverConfig.X_INSTANCE_SUBDOMAIN_HEADER, "schoolA")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("tenant:schoolA")
                .verifyComplete();
    }

    @Test
    @DisplayName("tenantKeyResolver — extracts subdomain from base-domain Host")
    void tenantResolverExtractsSubdomain() {
        KeyResolver resolver = config.tenantKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://schoolB.kiteclass.com/api/platform/branding/ai/analyze-logo")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("tenant:schoolB")
                .verifyComplete();
    }

    @Test
    @DisplayName("tenantKeyResolver — falls back to anon for unknown host")
    void tenantResolverFallsBackForUnknownHost() {
        KeyResolver resolver = config.tenantKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://random.example.com/api/v1/students")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("tenant:anon")
                .verifyComplete();
    }

    @Test
    @DisplayName("tenantKeyResolver — different subdomains produce DIFFERENT keys (multi-tenant isolation)")
    void tenantResolverPartitionsByTenant() {
        KeyResolver resolver = config.tenantKeyResolver();
        MockServerHttpRequest req1 = MockServerHttpRequest
                .get("http://localhost:9000/api/platform/branding/ai/analyze-logo")
                .header(KeyResolverConfig.X_INSTANCE_SUBDOMAIN_HEADER, "tenantA")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.1", 0))
                .build();
        MockServerHttpRequest req2 = MockServerHttpRequest
                .get("http://localhost:9000/api/platform/branding/ai/analyze-logo")
                .header(KeyResolverConfig.X_INSTANCE_SUBDOMAIN_HEADER, "tenantB")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.1", 0)) // same NAT IP!
                .build();

        String key1 = resolver.resolve(MockServerWebExchange.from(req1)).block();
        String key2 = resolver.resolve(MockServerWebExchange.from(req2)).block();

        assertThat(key1).isEqualTo("tenant:tenantA");
        assertThat(key2).isEqualTo("tenant:tenantB");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("apiKeyResolver — returns apikey:<value> when X-API-Key present")
    void apiKeyResolverReadsHeader() {
        KeyResolver resolver = config.apiKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://example.com/test")
                .header(KeyResolverConfig.X_API_KEY_HEADER, "k_live_xyz")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("apikey:k_live_xyz")
                .verifyComplete();
    }

    @Test
    @DisplayName("apiKeyResolver — falls back to anon when header missing")
    void apiKeyResolverFallsBack() {
        KeyResolver resolver = config.apiKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://example.com/test")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("apikey:anon")
                .verifyComplete();
    }

    @Test
    @DisplayName("ipKeyResolver — annotated @Primary so Spring Cloud Gateway autoconfig picks it (GAP-419)")
    void ipKeyResolverIsPrimary() throws NoSuchMethodException {
        // Spring Cloud Gateway's RequestRateLimiterGatewayFilterFactory autoconfig requires
        // a single KeyResolver bean. We declare three (ip/tenant/apiKey) — without @Primary
        // on the default, the gateway crashes at startup with NoUniqueBeanDefinitionException.
        // tenantKeyResolver and apiKeyResolver remain accessible by name via SpEL in routes.
        assertThat(KeyResolverConfig.class.getMethod("ipKeyResolver").isAnnotationPresent(Primary.class))
                .as("ipKeyResolver must be @Primary to satisfy SCG autoconfig single-bean requirement")
                .isTrue();
        assertThat(KeyResolverConfig.class.getMethod("tenantKeyResolver").isAnnotationPresent(Primary.class))
                .as("tenantKeyResolver must NOT be @Primary (only one default allowed)")
                .isFalse();
        assertThat(KeyResolverConfig.class.getMethod("apiKeyResolver").isAnnotationPresent(Primary.class))
                .as("apiKeyResolver must NOT be @Primary (only one default allowed)")
                .isFalse();
    }

    @Test
    @DisplayName("extractSubdomain — strips port + matches base domain")
    void extractSubdomainHandlesPort() {
        assertThat(config.extractSubdomain("schoolA.kiteclass.com")).isEqualTo("schoolA");
        assertThat(config.extractSubdomain("schoolA.kiteclass.com:443")).isEqualTo("schoolA");
        assertThat(config.extractSubdomain("kiteclass.com")).isNull(); // bare base
        assertThat(config.extractSubdomain("other.example.com")).isNull(); // wrong base
        assertThat(config.extractSubdomain(null)).isNull();
    }
}
