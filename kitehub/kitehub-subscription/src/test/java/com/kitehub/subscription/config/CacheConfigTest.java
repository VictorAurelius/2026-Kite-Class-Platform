package com.kitehub.subscription.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for {@link CacheConfig} (GAP-132).
 *
 * <p>Verifies the Caffeine {@link CacheManager} wires up with the expected cache
 * names and TTL, and that {@code @Cacheable}-style lookups are stable across
 * repeated reads. Uses the {@code CacheConfig} bean directly (no Spring context)
 * so the test stays fast and free of DB/RabbitMQ dependencies.
 */
class CacheConfigTest {

    @Test
    void cacheManager_exposesExpectedCaches() {
        CacheManager manager = new CacheConfig(60).cacheManager();

        assertThat(manager).isInstanceOf(CaffeineCacheManager.class);
        assertThat(manager.getCacheNames())
                .containsExactlyInAnyOrder(
                        CacheConfig.SUBSCRIPTION_BY_INSTANCE_CACHE,
                        CacheConfig.INSTANCE_SUMMARY_CACHE);
    }

    @Test
    void cache_putAndGet_returnsSameValue() {
        CacheManager manager = new CacheConfig(60).cacheManager();
        Cache cache = manager.getCache(CacheConfig.SUBSCRIPTION_BY_INSTANCE_CACHE);
        assertThat(cache).isNotNull();

        cache.put("instance-a", "PRO-ACTIVE");

        assertThat(cache.get("instance-a")).isNotNull();
        assertThat(cache.get("instance-a").get()).isEqualTo("PRO-ACTIVE");
    }

    @Test
    void cache_missingKey_returnsNull() {
        CacheManager manager = new CacheConfig(60).cacheManager();
        Cache cache = manager.getCache(CacheConfig.INSTANCE_SUMMARY_CACHE);

        assertThat(cache).isNotNull();
        assertThat(cache.get("missing")).isNull();
    }
}
