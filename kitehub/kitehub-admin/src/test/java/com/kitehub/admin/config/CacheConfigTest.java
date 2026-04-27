package com.kitehub.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for {@link CacheConfig} (GAP-132).
 *
 * <p>Verifies Caffeine cache manager wires both admin caches with distinct TTL
 * inputs. These tests are intentionally lightweight — no Spring context — so they
 * run on every Maven invocation without Docker/Testcontainers.
 */
class CacheConfigTest {

    @Test
    void cacheManager_exposesExpectedCaches() {
        CacheManager manager = new CacheConfig(300, 3600).cacheManager();

        assertThat(manager).isInstanceOf(CaffeineCacheManager.class);
        // GAP-238: admin's CacheManager is the primary in mixed admin+subscription contexts,
        // so it declares subscription's cache names too (transitively required by
        // kitehub-subscription components autowired into admin).
        assertThat(manager.getCacheNames())
                .containsExactlyInAnyOrder(
                        CacheConfig.ADMIN_DASHBOARD_CACHE,
                        CacheConfig.ADMIN_REVENUE_REPORT_CACHE,
                        "subscriptionByInstance",
                        "instanceSummary");
    }

    @Test
    void dashboardCache_putAndGet_returnsSameValue() {
        CacheManager manager = new CacheConfig(300, 3600).cacheManager();
        Cache cache = manager.getCache(CacheConfig.ADMIN_DASHBOARD_CACHE);
        assertThat(cache).isNotNull();

        cache.put("stats:2026-04", 42L);

        assertThat(cache.get("stats:2026-04")).isNotNull();
        assertThat(cache.get("stats:2026-04").get()).isEqualTo(42L);
    }

    @Test
    void revenueCache_clear_removesEntries() {
        CacheManager manager = new CacheConfig(300, 3600).cacheManager();
        Cache cache = manager.getCache(CacheConfig.ADMIN_REVENUE_REPORT_CACHE);
        assertThat(cache).isNotNull();

        cache.put("revenue:2026-Q1", 1_000_000L);
        cache.clear();

        assertThat(cache.get("revenue:2026-Q1")).isNull();
    }
}
