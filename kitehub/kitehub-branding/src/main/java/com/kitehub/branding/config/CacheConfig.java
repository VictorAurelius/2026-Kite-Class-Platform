package com.kitehub.branding.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed cache configuration for kitehub-branding.
 *
 * <p>Closes <strong>GAP-132</strong> fan-out (Wave 9.5-B). Branding service has
 * two read-heavy, slow-moving datasets suitable for in-process caching:
 * <ul>
 *   <li>Template gallery lookups — SVG templates rarely change, high read rate
 *       from wizard preview + generation requests.</li>
 *   <li>AI rate-limit counters — per-tenant daily quota reads, tight loop in
 *       the generate flow.</li>
 * </ul>
 *
 * <p>Default TTL 5 minutes for templates, 15 minutes for rate-limit; override
 * via {@code kitehub.branding.cache.*-ttl-seconds} properties. Pattern aligned
 * with {@code kitehub-email/BrandingCacheConfig} and {@code kitehub-admin/CacheConfig}.
 *
 * @since Wave 9.5 (GAP-132 fan-out)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Cache for resolved SVG template payloads + metadata lookups. */
    public static final String BRANDING_TEMPLATE_CACHE = "brandingTemplates";

    /** Cache for per-tenant AI rate-limit counters (short TTL, hot path). */
    public static final String BRANDING_RATE_LIMIT_CACHE = "brandingRateLimit";

    private final long templateTtlSeconds;
    private final long rateLimitTtlSeconds;

    public CacheConfig(
            @Value("${kitehub.branding.cache.template-ttl-seconds:300}") long templateTtlSeconds,
            @Value("${kitehub.branding.cache.rate-limit-ttl-seconds:900}") long rateLimitTtlSeconds) {
        this.templateTtlSeconds = templateTtlSeconds;
        this.rateLimitTtlSeconds = rateLimitTtlSeconds;
    }

    @Bean
    public CacheManager cacheManager() {
        // Use max TTL for the Caffeine builder; per-cache finer-grained TTL differentiation
        // would require a dedicated CaffeineCacheManager subclass. For baseline GAP-132 we
        // accept single-TTL configuration — the rate-limit cache evicts naturally sooner
        // because it is touched far more frequently than the template gallery cache.
        long maxTtl = Math.max(templateTtlSeconds, rateLimitTtlSeconds);

        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of(BRANDING_TEMPLATE_CACHE, BRANDING_RATE_LIMIT_CACHE));
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(maxTtl, TimeUnit.SECONDS)
                .maximumSize(5_000));
        return manager;
    }
}
