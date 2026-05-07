package com.kitehub.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Key resolvers for rate limiting (GAP-259).
 *
 * <p>Three keying strategies — pick per-route in {@code application.yml}:
 * <ul>
 *   <li>{@code ipKeyResolver} — anonymous routes (auth, public). NAT-shared IPs share one bucket.</li>
 *   <li>{@code tenantKeyResolver} — authenticated tenant routes. Each tenant subdomain (or
 *       {@code X-Instance-Subdomain} dev header) gets an isolated bucket so a flooded tenant
 *       cannot starve co-tenants behind the same NAT/CGNAT egress.</li>
 *   <li>{@code apiKeyResolver} — machine-to-machine routes that authenticate via
 *       {@code X-API-Key} (e.g. integrations).</li>
 * </ul>
 *
 * <p>Tenant resolution uses the same subdomain extraction as
 * {@code TenantResolverGatewayFilterFactory} but is stateless (no DB lookup). Rate-limit
 * filters run before route filters in Spring Cloud Gateway, so the resolver MUST NOT depend
 * on headers populated downstream by {@code TenantResolverGatewayFilterFactory}.</p>
 *
 * @author KiteHub Team
 * @since 1.1.0 (GAP-259)
 */
@Configuration
public class KeyResolverConfig {

    public static final String X_INSTANCE_SUBDOMAIN_HEADER = "X-Instance-Subdomain";
    public static final String X_API_KEY_HEADER = "X-API-Key";
    static final String ANON_KEY = "anon";

    private final String baseDomain;

    public KeyResolverConfig(@Value("${kitehub.domain.base:.kiteclass.com}") String baseDomain) {
        this.baseDomain = baseDomain;
    }

    /**
     * Rate limit by client IP address.
     * Used for anonymous routes (auth/register, public) where no tenant context exists.
     *
     * <p>Marked {@code @Primary} (GAP-419) so Spring Cloud Gateway's
     * {@code RequestRateLimiterGatewayFilterFactory} autoconfig — which expects a single
     * {@code KeyResolver} bean — picks this as the default. Routes that want
     * {@code tenantKeyResolver} or {@code apiKeyResolver} reference them by name via SpEL
     * (e.g. {@code key-resolver: "#{@tenantKeyResolver}"}) in {@code application.yml}.</p>
     */
    @Primary
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : ANON_KEY;
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * Rate limit by tenant subdomain (or {@code X-Instance-Subdomain} dev header).
     *
     * <p>Each tenant gets its own bucket regardless of source IP. Falls back to the IP-based
     * key when no tenant can be resolved (request will be subject to anonymous rate limit).</p>
     */
    @Bean
    public KeyResolver tenantKeyResolver() {
        return exchange -> {
            String subdomain = exchange.getRequest().getHeaders().getFirst(X_INSTANCE_SUBDOMAIN_HEADER);
            if (subdomain == null || subdomain.isBlank()) {
                String host = exchange.getRequest().getURI().getHost();
                subdomain = extractSubdomain(host);
            }
            if (subdomain == null || subdomain.isBlank()) {
                return Mono.just("tenant:" + ANON_KEY);
            }
            return Mono.just("tenant:" + subdomain);
        };
    }

    /**
     * Rate limit by API key for machine-to-machine traffic.
     * Falls back to the IP-based key when the {@code X-API-Key} header is absent.
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst(X_API_KEY_HEADER);
            if (apiKey == null || apiKey.isBlank()) {
                return Mono.just("apikey:" + ANON_KEY);
            }
            return Mono.just("apikey:" + apiKey);
        };
    }

    String extractSubdomain(String host) {
        if (host == null) {
            return null;
        }
        String hostname = host;
        int colonIdx = hostname.indexOf(':');
        if (colonIdx >= 0) {
            hostname = hostname.substring(0, colonIdx);
        }
        if (hostname.endsWith(baseDomain)) {
            int endIdx = hostname.indexOf(baseDomain);
            return endIdx > 0 ? hostname.substring(0, endIdx) : null;
        }
        return null;
    }
}
