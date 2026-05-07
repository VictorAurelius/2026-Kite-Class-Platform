package com.kitehub.admin.service;

import com.kitehub.admin.config.CacheConfig;
import com.kitehub.admin.dto.DashboardStats;
import com.kitehub.admin.dto.RevenueReport;
import com.kitehub.admin.event.SubscriptionDataChangedEvent;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies GAP-126 cache behavior + GAP-432 bounded query path:
 * dashboard + revenue stats are cached, and data-change events evict admin
 * caches. Asserts cache hits trigger zero additional repository calls along
 * the bounded aggregation path (replacing the old findAll()-based path).
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

    private List<Subscription> activeInPeriodFixture;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CacheConfig.ADMIN_DASHBOARD_CACHE).clear();
        cacheManager.getCache(CacheConfig.ADMIN_REVENUE_REPORT_CACHE).clear();

        Subscription s1 = new Subscription();
        s1.setId(UUID.randomUUID());
        s1.setInstanceId(UUID.randomUUID());
        s1.setTier(PricingTier.BASIC);
        s1.setBillingCycle(BillingCycle.MONTHLY);
        s1.setPriceVnd(500_000L);
        s1.setStatus(SubscriptionStatus.ACTIVE);
        s1.setStartedAt(LocalDateTime.now().minusDays(30));
        s1.setExpiresAt(LocalDateTime.now().plusDays(30));
        activeInPeriodFixture = List.of(s1);

        // GAP-432 bounded-query stubs (replace prior findAll() stubs).
        when(instanceRepository.countByDeletedFalse()).thenReturn(1L);
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put(InstanceStatus.ACTIVE.name(), 1L);
        when(instanceRepository.countInstancesByStatus()).thenReturn(statusCounts);
        when(instanceRepository.countByDeletedFalseAndCreatedAtAfter(any(LocalDateTime.class)))
            .thenReturn(1L);

        Map<String, Long> tierCounts = new HashMap<>();
        tierCounts.put(PricingTier.BASIC.name(), 1L);
        when(subscriptionRepository.countSubscriptionsByTier()).thenReturn(tierCounts);
        when(subscriptionRepository.sumActiveMrr()).thenReturn(500_000L);
        when(subscriptionRepository.sumCancelledRevenue()).thenReturn(0L);
        java.util.List<Object[]> tierRevenue = new java.util.ArrayList<>();
        tierRevenue.add(new Object[] { PricingTier.BASIC, 500_000L });
        when(subscriptionRepository.sumActiveRevenueByTier()).thenReturn(tierRevenue);
        when(subscriptionRepository.findActiveInPeriod(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(activeInPeriodFixture);
    }

    @Test
    void dashboardStats_secondCall_servedFromCache() {
        // GAP-126 §AC — "<5 SQL queries per dashboard request" on cache-hit path.
        // GAP-432: bounded path now uses count + group-by + sum (3 instance + 3 sub
        // calls cold; zero on warm). We assert cache hit = zero re-invocations.
        DashboardStats first = analyticsService.getDashboardStats();
        DashboardStats second = analyticsService.getDashboardStats();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second).isSameAs(first); // cached object reference

        // Cold call: each bounded method invoked exactly once.
        verify(instanceRepository, times(1)).countByDeletedFalse();
        verify(instanceRepository, times(1)).countInstancesByStatus();
        verify(subscriptionRepository, times(1)).sumActiveMrr();
        // Legacy unbounded path must never run.
        verify(instanceRepository, times(0)).findAll();
        verify(subscriptionRepository, times(0)).findAll();
    }

    @Test
    void revenueReport_secondCall_servedFromCache() {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        RevenueReport first = analyticsService.getRevenueReport("MONTHLY", start, end);
        RevenueReport second = analyticsService.getRevenueReport("MONTHLY", start, end);

        assertThat(second).isSameAs(first);
        verify(subscriptionRepository, times(1))
            .findActiveInPeriod(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(subscriptionRepository, times(0)).findAll();
    }

    @Test
    void subscriptionDataChangedEvent_evictsAdminCaches() {
        // Prime caches
        analyticsService.getDashboardStats();
        analyticsService.getRevenueReport(
                "MONTHLY", LocalDate.now().minusDays(7), LocalDate.now());

        verify(instanceRepository, times(1)).countByDeletedFalse();
        verify(subscriptionRepository, times(2)).sumActiveMrr(); // dashboard + revenue
        verify(subscriptionRepository, times(1))
            .findActiveInPeriod(any(LocalDateTime.class), any(LocalDateTime.class));

        // Fire event — listener evicts both caches
        eventPublisher.publishEvent(
                new SubscriptionDataChangedEvent(this, "subscription.created", null));

        // Next calls should re-hit repos
        analyticsService.getDashboardStats();
        analyticsService.getRevenueReport(
                "MONTHLY", LocalDate.now().minusDays(7), LocalDate.now());

        verify(instanceRepository, times(2)).countByDeletedFalse();
        verify(subscriptionRepository, times(4)).sumActiveMrr();
        verify(subscriptionRepository, times(2))
            .findActiveInPeriod(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(instanceRepository, times(0)).findAll();
        verify(subscriptionRepository, times(0)).findAll();
    }
}
