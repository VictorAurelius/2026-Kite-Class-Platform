package com.kitehub.subscription.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed cache configuration for kitehub-subscription.
 *
 * <p>Closes <strong>GAP-132</strong> — prior to this class, no {@code @EnableCaching}
 * existed in kitehub-subscription, so any future {@code @Cacheable} annotation would
 * silently no-op. Caffeine (in-memory, per-JVM) mirrors the pattern already used in
 * kitehub-email ({@code BrandingCacheConfig}) and kitehub-gateway
 * ({@code GatewayBrandingCacheConfig}).
 *
 * <p>TTL is intentionally short (default 5 minutes) because this service hosts
 * instance/subscription CRUD — staleness after a webhook-driven update is the main
 * failure mode. Redis migration (cross-pod coherence) remains tracked as follow-up
 * under GAP-132 AC (belt-and-braces).
 *
 * <p>Caches declared here are the seed set; adding a new {@code @Cacheable} with a
 * new value name is fine — CaffeineCacheManager creates caches on-demand when
 * {@code setAllowNullValues(false)} and no fixed list is set. We list the known
 * seed caches to make the intent explicit.
 *
 * <p>Tune TTL via {@code kitehub.subscription.cache.ttl-seconds} property.
 *
 * @since Wave 9 (GAP-132)
 */
@Configuration("subscriptionCacheConfig")
@EnableCaching
public class CacheConfig {

    /** Cache for subscription lookups (tier, status, expiry). */
    public static final String SUBSCRIPTION_BY_INSTANCE_CACHE = "subscriptionByInstance";

    /** Cache for instance metadata (slug, owner, created-at). */
    public static final String INSTANCE_SUMMARY_CACHE = "instanceSummary";

    private final long ttlSeconds;

    public CacheConfig(@Value("${kitehub.subscription.cache.ttl-seconds:300}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Backs subscription-side caches.
     *
     * <p>Explicit bean name {@code subscriptionCacheManager} prevents
     * {@code BeanDefinitionOverrideException} when consumer modules (e.g.
     * {@code kitehub-admin}) define their own {@code @Primary CacheManager}.
     * {@code @ConditionalOnMissingBean} alone is insufficient because Spring's
     * @Configuration ordering across modules is non-deterministic — explicit unique
     * naming + @Primary on the consumer's bean is the reliable pattern. Closes GAP-238 +
     * GAP-240 follow-up (full SpringBootTest verification).</p>
     */
    @Bean(name = "subscriptionCacheManager")
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of(SUBSCRIPTION_BY_INSTANCE_CACHE, INSTANCE_SUMMARY_CACHE));
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(10_000)
                // GAP-1357: recordStats() lets Spring Boot CacheMetricsRegistrar
                // bind cache.gets{result=hit|miss} to Micrometer → /actuator/prometheus.
                .recordStats());
        return manager;
    }
}
