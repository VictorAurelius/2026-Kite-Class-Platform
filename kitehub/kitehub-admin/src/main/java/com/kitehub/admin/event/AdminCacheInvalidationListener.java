package com.kitehub.admin.event;

import com.kitehub.admin.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Evicts admin dashboard / revenue caches when subscription or instance data
 * mutates.
 *
 * <p>Closes part of <strong>GAP-126</strong>. The cache TTL (5 min) bounds
 * staleness; this listener provides write-through invalidation so the dashboard
 * never shows stale numbers immediately after a known mutation in the same
 * service instance.</p>
 *
 * <p>Future enhancement (follow-up gap): bridge a RabbitMQ {@code @RabbitListener}
 * on {@code instance.*}/{@code subscription.*} routing keys into a
 * {@link SubscriptionDataChangedEvent} so cross-service mutations also evict.</p>
 *
 * @since Wave 7-Perf (GAP-126)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminCacheInvalidationListener {

    private final CacheManager cacheManager;

    private static final List<String> ADMIN_CACHES = List.of(
            CacheConfig.ADMIN_DASHBOARD_CACHE,
            CacheConfig.ADMIN_REVENUE_REPORT_CACHE
    );

    @EventListener
    public void onSubscriptionDataChanged(SubscriptionDataChangedEvent event) {
        log.debug("Evicting admin caches due to changeType={}, aggregateId={}",
                event.getChangeType(), event.getAggregateId());
        evictAll();
    }

    /**
     * Evict every admin analytics cache. Safe to call manually (e.g. from
     * scheduled refresh). Idempotent.
     */
    public void evictAll() {
        for (String cacheName : ADMIN_CACHES) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
