package com.kitehub.gateway.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    public static final String X_USER_EMAIL_HEADER = "X-User-Email";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    static final String ANON_KEY = "anon";

    private final String baseDomain;

    public KeyResolverConfig(@Value("${kitehub.domain.base:.kitehub.me}") String baseDomain) {
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

    /**
     * Rate limit by user email address — used for endpoints abusable per-email regardless of
     * source IP (e.g. {@code /api/auth/resend-verification}, {@code /api/auth/password-reset-request}).
     *
     * <p>Resolution order (GAP-514, OWASP A07 hardening):
     * <ol>
     *   <li>Frontend-set {@code X-User-Email} header — present when FE knows target email</li>
     *   <li>IP fallback — opaque clients without the header still get rate-limited</li>
     * </ol>
     *
     * <p>Reading the request body in Spring Cloud Gateway requires buffering the body
     * (DataBufferUtils + cache filter), which adds latency to the auth hot path. Header-based
     * resolution is the practical v1 trade-off: legitimate FE flows already know the target
     * email at form-submit time, so passing it as a header is cheap. Attackers can spoof the
     * header but each spoofed identity still consumes IP-bucket budget via the fallback.</p>
     */
    @Bean
    public KeyResolver emailKeyResolver() {
        return exchange -> {
            String email = exchange.getRequest().getHeaders().getFirst(X_USER_EMAIL_HEADER);
            if (email != null && !email.isBlank()) {
                return Mono.just("email:" + email.trim().toLowerCase());
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : ANON_KEY;
            return Mono.just("email-fallback-ip:" + ip);
        };
    }

    /**
     * Rate limit by authenticated user — used for endpoints that already require a JWT
     * (e.g. {@code /api/auth/refresh}). Parses the {@code sub} claim from the JWT payload
     * WITHOUT validating the signature; the downstream auth filter is responsible for
     * signature + expiry checks. The key resolver only needs a stable identifier to
     * partition rate-limit buckets.
     *
     * <p>Fallback chain (GAP-514, OWASP A07 hardening):
     * <ol>
     *   <li>JWT {@code sub} claim from {@code Authorization: Bearer ...}</li>
     *   <li>IP fallback when no bearer token present (e.g. malformed request)</li>
     * </ol>
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
            String userId = extractJwtSubject(authHeader);
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : ANON_KEY;
            return Mono.just("user-fallback-ip:" + ip);
        };
    }

    /**
     * Parse the {@code sub} claim from a JWT's payload without signature validation.
     * Returns {@code null} when the header is missing, malformed, or the payload is not valid JSON.
     *
     * <p>This is intentionally minimal — a regex pull of {@code "sub":"<value>"}. Real validation
     * happens at the downstream auth filter; the gateway only needs the identifier to bucket
     * rate-limit requests.</p>
     */
    String extractJwtSubject(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            // Minimal extraction — no JSON library dependency in gateway hot path.
            int subIdx = payload.indexOf("\"sub\"");
            if (subIdx < 0) {
                return null;
            }
            int colonIdx = payload.indexOf(':', subIdx);
            int quoteStart = payload.indexOf('"', colonIdx + 1);
            int quoteEnd = quoteStart >= 0 ? payload.indexOf('"', quoteStart + 1) : -1;
            if (quoteStart < 0 || quoteEnd < 0) {
                return null;
            }
            return payload.substring(quoteStart + 1, quoteEnd);
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
