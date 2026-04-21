package com.kitehub.branding.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for {@link CacheConfig} (GAP-132 fan-out — Wave 9.5-B).
 *
 * <p>Verifies the Caffeine cache manager wires the branding caches with the
 * configured TTL inputs. Lightweight (no Spring context) so they run on every
 * Maven invocation without Docker/Testcontainers.
 */
class CacheConfigTest {

    @Test
    void cacheManager_isCaffeineBackedWithExpectedCaches() {
        CacheManager manager = new CacheConfig(300, 900).cacheManager();

        assertThat(manager).isInstanceOf(CaffeineCacheManager.class);
        assertThat(manager.getCacheNames())
                .containsExactlyInAnyOrder(
                        CacheConfig.BRANDING_TEMPLATE_CACHE,
                        CacheConfig.BRANDING_RATE_LIMIT_CACHE);
    }

    @Test
    void templateCache_putAndGet_returnsSameValue() {
        CacheManager manager = new CacheConfig(300, 900).cacheManager();
        Cache cache = manager.getCache(CacheConfig.BRANDING_TEMPLATE_CACHE);
        assertThat(cache).isNotNull();

        cache.put("template:modern-v1", "svg-body-placeholder");

        assertThat(cache.get("template:modern-v1")).isNotNull();
        assertThat(cache.get("template:modern-v1").get()).isEqualTo("svg-body-placeholder");
    }

    @Test
    void rateLimitCache_clear_removesEntries() {
        CacheManager manager = new CacheConfig(300, 900).cacheManager();
        Cache cache = manager.getCache(CacheConfig.BRANDING_RATE_LIMIT_CACHE);
        assertThat(cache).isNotNull();

        cache.put("tenant:abc:2026-04-21", 5);
        cache.clear();

        assertThat(cache.get("tenant:abc:2026-04-21")).isNull();
    }
}
