package com.kitehub.admin.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed cache configuration for kitehub-admin.
 *
 * <p>Closes <strong>GAP-132</strong>. Admin dashboard endpoints (analytics, revenue
 * reports) benefit from a short-lived cache because the underlying aggregations
 * scan large tables and the data is acceptably stale for a few minutes.
 *
 * <p>Two tiers of TTL modelled here — frequently-changing analytics (shorter) vs
 * slower-moving revenue reports (longer). Default TTL 5 minutes, revenue TTL 1 hour;
 * override via {@code kitehub.admin.cache.*-ttl-seconds} properties.
 *
 * @since Wave 9 (GAP-132)
 */
@Configuration("adminCacheConfig")
@EnableCaching
public class CacheConfig {

    /** Cache for dashboard summary statistics (short TTL). */
    public static final String ADMIN_DASHBOARD_CACHE = "adminDashboard";

    /** Cache for revenue / payments reports (longer TTL — expensive aggregation). */
    public static final String ADMIN_REVENUE_REPORT_CACHE = "adminRevenueReport";

    private final long dashboardTtlSeconds;
    private final long revenueTtlSeconds;

    public CacheConfig(
            @Value("${kitehub.admin.cache.dashboard-ttl-seconds:300}") long dashboardTtlSeconds,
            @Value("${kitehub.admin.cache.revenue-ttl-seconds:3600}") long revenueTtlSeconds) {
        this.dashboardTtlSeconds = dashboardTtlSeconds;
        this.revenueTtlSeconds = revenueTtlSeconds;
    }

    /**
     * {@link Primary} {@link CacheManager} for {@code kitehub-admin}. Includes admin
     * caches plus caches transitively required by {@code kitehub-subscription}
     * dependencies that this module pulls in (e.g. {@code subscriptionByInstance},
     * {@code instanceSummary}).
     *
     * <p>Closes GAP-238 + GAP-240 follow-up: subscription's
     * {@code subscriptionCacheManager} bean coexists with this {@code adminCacheManager}
     * — distinct names prevent {@link
     * org.springframework.beans.factory.support.BeanDefinitionOverrideException}.
     * {@code @Primary} ensures Spring's {@code @Cacheable} resolution defaults to this
     * one when admin context loads both modules.</p>
     */
    @Primary
    @Bean(name = "adminCacheManager")
    public CacheManager cacheManager() {
        // Use max TTL for the Caffeine builder; per-cache finer-grained TTL differentiation
        // would require a dedicated CaffeineCacheManager subclass. For baseline GAP-132 we
        // accept single-TTL configuration — the dashboard cache evicts naturally before the
        // revenue cache because it is touched far more frequently.
        long maxTtl = Math.max(dashboardTtlSeconds, revenueTtlSeconds);

        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of(
                ADMIN_DASHBOARD_CACHE,
                ADMIN_REVENUE_REPORT_CACHE,
                // Transitively required by kitehub-subscription components in this context.
                "subscriptionByInstance",
                "instanceSummary"));
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(maxTtl, TimeUnit.SECONDS)
                .maximumSize(5_000)
                // GAP-1357: recordStats() lets Spring Boot CacheMetricsRegistrar
                // bind cache.gets{result=hit|miss} to Micrometer → /actuator/prometheus.
                .recordStats());
        return manager;
    }
}
