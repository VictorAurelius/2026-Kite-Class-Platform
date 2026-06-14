package com.kitehub.email.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed cache for tenant branding lookups.
 *
 * <p>5-minute TTL matches the freshness SLA on the branding package endpoint.
 * Entries are evicted eagerly on {@code branding.updated} RabbitMQ events so
 * tenants see edits within seconds; the TTL is the safety net for missed events.
 *
 * @since Wave 4 (GAP-021)
 */
@Configuration
@EnableCaching
public class BrandingCacheConfig {

    public static final String TENANT_BRANDING_CACHE = "tenantBranding";

    @Bean
    public CacheManager brandingCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(TENANT_BRANDING_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(5_000)
                // GAP-1357: recordStats() lets Spring Boot CacheMetricsRegistrar
                // bind cache.gets{result=hit|miss} to Micrometer → /actuator/prometheus.
                .recordStats());
        return manager;
    }
}
