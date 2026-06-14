package com.kitehub.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed cache for the gateway's branding lookups used by error pages.
 * 5-minute TTL mirrors the email service so stale payloads don't linger.
 *
 * @since Wave 4 (GAP-032)
 */
@Configuration
@EnableCaching
public class GatewayBrandingCacheConfig {

    public static final String GATEWAY_BRANDING_CACHE = "gatewayBranding";

    @Bean
    public CacheManager gatewayBrandingCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(GATEWAY_BRANDING_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(2_000)
                // GAP-1357: recordStats() lets Spring Boot CacheMetricsRegistrar
                // bind cache.gets{result=hit|miss} to Micrometer (when a registry is present).
                .recordStats());
        return manager;
    }
}
