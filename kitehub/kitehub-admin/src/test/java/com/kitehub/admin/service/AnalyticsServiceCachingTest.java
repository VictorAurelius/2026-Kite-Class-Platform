package com.kitehub.admin.service;

import com.kitehub.admin.config.CacheConfig;
import com.kitehub.admin.dto.DashboardStats;
import com.kitehub.admin.dto.RevenueReport;
import com.kitehub.admin.event.SubscriptionDataChangedEvent;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies GAP-126 cache behavior: dashboard + revenue stats are cached, and
 * data-change events evict admin caches.
 *
 * <p>Uses a focused Spring context (only cache infrastructure + AnalyticsService +
 * CacheInvalidationListener) so we don't drag in JPA/Testcontainers.</p>
 */
@SpringBootTest(classes = {
        CacheConfig.class,
        AnalyticsService.class,
        com.kitehub.admin.event.AdminCacheInvalidationListener.class
})
@Import(AnalyticsServiceCachingTest.NoOpJpaAutoConfigStub.class)
@TestPropertySource(properties = {
        "spring.cache.type=caffeine",
        "kitehub.admin.cache.dashboard-ttl-seconds=300",
        "kitehub.admin.cache.revenue-ttl-seconds=3600"
})
class AnalyticsServiceCachingTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class NoOpJpaAutoConfigStub {
        // Empty — disables JPA autoconfig via @SpringBootTest classes whitelist.
    }

    @MockitoBean
    private InstanceRepository instanceRepository;

    @MockitoBean
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private List<Instance> instances;
    private List<Subscription> subscriptions;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CacheConfig.ADMIN_DASHBOARD_CACHE).clear();
        cacheManager.getCache(CacheConfig.ADMIN_REVENUE_REPORT_CACHE).clear();

        Instance i1 = new Instance();
        i1.setId(UUID.randomUUID());
        i1.setOrganizationName("Org");
        i1.setSubdomain("org");
        i1.setStatus(InstanceStatus.ACTIVE);
        i1.setCreatedAt(LocalDateTime.now().minusDays(5));
        instances = List.of(i1);

        Subscription s1 = new Subscription();
        s1.setId(UUID.randomUUID());
        s1.setInstanceId(i1.getId());
        s1.setTier(PricingTier.BASIC);
        s1.setBillingCycle(BillingCycle.MONTHLY);
        s1.setPriceVnd(500_000L);
        s1.setStatus(SubscriptionStatus.ACTIVE);
        s1.setStartedAt(LocalDateTime.now().minusDays(30));
        s1.setExpiresAt(LocalDateTime.now().plusDays(30));
        subscriptions = List.of(s1);

        when(instanceRepository.findAll()).thenReturn(instances);
        when(subscriptionRepository.findAll()).thenReturn(subscriptions);
    }

    @Test
    void dashboardStats_secondCall_servedFromCache() {
        // GAP-126 §AC — "<5 SQL queries per dashboard request" on cache-hit path.
        // We don't need datasource-proxy here: the cache hit means ZERO underlying
        // repository calls, which is strictly fewer than 5 SQL queries. We assert
        // the strongest form: the second call invokes the repos zero additional
        // times beyond the first cold call.
        DashboardStats first = analyticsService.getDashboardStats();
        DashboardStats second = analyticsService.getDashboardStats();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second).isSameAs(first); // cached object reference

        verify(instanceRepository, times(1)).findAll();
        verify(subscriptionRepository, times(1)).findAll();
    }

    @Test
    void revenueReport_secondCall_servedFromCache() {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        RevenueReport first = analyticsService.getRevenueReport("MONTHLY", start, end);
        RevenueReport second = analyticsService.getRevenueReport("MONTHLY", start, end);

        assertThat(second).isSameAs(first);
        verify(subscriptionRepository, times(1)).findAll();
    }

    @Test
    void subscriptionDataChangedEvent_evictsAdminCaches() {
        // Prime caches
        analyticsService.getDashboardStats();
        analyticsService.getRevenueReport(
                "MONTHLY", LocalDate.now().minusDays(7), LocalDate.now());

        verify(instanceRepository, times(1)).findAll();
        verify(subscriptionRepository, times(2)).findAll();

        // Fire event — listener evicts both caches
        eventPublisher.publishEvent(
                new SubscriptionDataChangedEvent(this, "subscription.created", null));

        // Next calls should re-hit repos
        analyticsService.getDashboardStats();
        analyticsService.getRevenueReport(
                "MONTHLY", LocalDate.now().minusDays(7), LocalDate.now());

        verify(instanceRepository, times(2)).findAll();
        verify(subscriptionRepository, times(4)).findAll();
    }
}
