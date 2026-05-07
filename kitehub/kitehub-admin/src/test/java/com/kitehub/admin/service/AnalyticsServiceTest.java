package com.kitehub.admin.service;

import com.kitehub.admin.dto.DashboardStats;
import com.kitehub.admin.dto.RevenueReport;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
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
 * Tests for AnalyticsService.
 *
 * <p>GAP-432 (Wave 41 Bucket C): rewritten to assert that dashboard + revenue
 * report are computed via DB-side aggregation methods (count, sum, group-by)
 * instead of {@code findAll()}. The mocks now stub the new repository methods
 * directly; if a future refactor reintroduces {@code findAll()} the tests
 * will fail because those calls are explicitly verified to never run.</p>
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AnalyticsService Unit Tests (GAP-432 bounded queries)")
class AnalyticsServiceTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        // 3 instances: 1 ACTIVE, 1 TRIAL, 1 SUSPENDED.
        when(instanceRepository.countByDeletedFalse()).thenReturn(3L);
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put(InstanceStatus.ACTIVE.name(), 1L);
        statusCounts.put(InstanceStatus.TRIAL.name(), 1L);
        statusCounts.put(InstanceStatus.SUSPENDED.name(), 1L);
        when(instanceRepository.countInstancesByStatus()).thenReturn(statusCounts);
        // 2 instances created in the last 30 days (instance1, instance2 in legacy fixture).
        when(instanceRepository.countByDeletedFalseAndCreatedAtAfter(any(LocalDateTime.class)))
            .thenReturn(2L);

        // Tier breakdown: 1 BASIC + 1 PREMIUM (from active subs).
        Map<String, Long> tierCounts = new HashMap<>();
        tierCounts.put(PricingTier.BASIC.name(), 1L);
        tierCounts.put(PricingTier.PREMIUM.name(), 1L);
        when(subscriptionRepository.countSubscriptionsByTier()).thenReturn(tierCounts);

        // MRR = 500_000 + 1_500_000 = 2_000_000 (DB SUM).
        when(subscriptionRepository.sumActiveMrr()).thenReturn(2_000_000L);
        // No CANCELLED subs in fixture.
        when(subscriptionRepository.sumCancelledRevenue()).thenReturn(0L);

        // Revenue by tier: BASIC=500k, PREMIUM=1.5M (DB GROUP BY + SUM).
        java.util.List<Object[]> tierRevenue = new java.util.ArrayList<>();
        tierRevenue.add(new Object[] { PricingTier.BASIC, 500_000L });
        tierRevenue.add(new Object[] { PricingTier.PREMIUM, 1_500_000L });
        when(subscriptionRepository.sumActiveRevenueByTier()).thenReturn(tierRevenue);
    }

    @Test
    @DisplayName("getDashboardStats uses DB-side aggregation (no findAll)")
    void testGetDashboardStats() {
        DashboardStats stats = analyticsService.getDashboardStats();

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalInstances()).isEqualTo(3L);

        assertThat(stats.getInstancesByStatus())
                .containsEntry("ACTIVE", 1L)
                .containsEntry("TRIAL", 1L)
                .containsEntry("SUSPENDED", 1L);

        assertThat(stats.getInstancesByTier())
                .containsEntry("BASIC", 1L)
                .containsEntry("PREMIUM", 1L);

        assertThat(stats.getMrr()).isEqualByComparingTo(new BigDecimal("2000000"));
        assertThat(stats.getArr()).isEqualByComparingTo(new BigDecimal("24000000"));
        assertThat(stats.getChurnRate()).isCloseTo(33.33, org.assertj.core.data.Offset.offset(0.01));
        assertThat(stats.getNewSignupsLast30Days()).isEqualTo(2L);
        assertThat(stats.getTotalActiveUsers()).isEqualTo(10L); // 1 active * 10
        assertThat(stats.getCalculatedAt()).isNotNull();
        assertThat(stats.getRevenueByTier())
                .containsEntry("BASIC", new BigDecimal("500000"))
                .containsEntry("PREMIUM", new BigDecimal("1500000"));

        // GAP-432 invariant: never call findAll().
        verify(instanceRepository, Mockito.never()).findAll();
        verify(subscriptionRepository, Mockito.never()).findAll();
    }

    @Test
    @DisplayName("getRevenueReport uses bounded findActiveInPeriod (no findAll)")
    void testGetRevenueReport() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        Subscription s1 = new Subscription();
        s1.setId(UUID.randomUUID());
        s1.setTier(PricingTier.BASIC);
        s1.setBillingCycle(BillingCycle.MONTHLY);
        s1.setPriceVnd(500_000L);
        s1.setStatus(SubscriptionStatus.ACTIVE);
        s1.setStartedAt(LocalDateTime.now().minusDays(30));
        s1.setExpiresAt(LocalDateTime.now().plusDays(30));

        Subscription s2 = new Subscription();
        s2.setId(UUID.randomUUID());
        s2.setTier(PricingTier.PREMIUM);
        s2.setBillingCycle(BillingCycle.MONTHLY);
        s2.setPriceVnd(1_500_000L);
        s2.setStatus(SubscriptionStatus.ACTIVE);
        s2.setStartedAt(LocalDateTime.now().minusDays(20));
        s2.setExpiresAt(LocalDateTime.now().plusDays(40));

        when(subscriptionRepository.findActiveInPeriod(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(s1, s2));

        RevenueReport report = analyticsService.getRevenueReport("MONTHLY", startDate, endDate);

        assertThat(report).isNotNull();
        assertThat(report.getPeriod()).isEqualTo("MONTHLY");
        assertThat(report.getStartDate()).isEqualTo(startDate);
        assertThat(report.getEndDate()).isEqualTo(endDate);
        assertThat(report.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("2000000"));
        assertThat(report.getRevenueByTier()).hasSize(2);
        assertThat(report.getRevenueByTier()).extracting("tier")
                .containsExactlyInAnyOrder("BASIC", "PREMIUM");
        assertThat(report.getMrr()).isEqualByComparingTo(new BigDecimal("2000000"));
        assertThat(report.getProjectedArr()).isEqualByComparingTo(new BigDecimal("24000000"));
        assertThat(report.getChurnImpact()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getDailyRevenue()).isNotEmpty();

        // GAP-432 invariant: range filter pushed to DB.
        verify(subscriptionRepository, times(1))
            .findActiveInPeriod(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(subscriptionRepository, Mockito.never()).findAll();
    }

    @Test
    @DisplayName("getDashboardStats handles empty data via aggregation zeros")
    void testGetDashboardStats_EmptyData() {
        when(instanceRepository.countByDeletedFalse()).thenReturn(0L);
        when(instanceRepository.countInstancesByStatus()).thenReturn(new HashMap<>());
        when(instanceRepository.countByDeletedFalseAndCreatedAtAfter(any(LocalDateTime.class)))
            .thenReturn(0L);
        when(subscriptionRepository.countSubscriptionsByTier()).thenReturn(new HashMap<>());
        when(subscriptionRepository.sumActiveMrr()).thenReturn(0L);
        when(subscriptionRepository.sumCancelledRevenue()).thenReturn(0L);
        when(subscriptionRepository.sumActiveRevenueByTier()).thenReturn(List.of());

        DashboardStats stats = analyticsService.getDashboardStats();

        assertThat(stats.getTotalInstances()).isEqualTo(0L);
        assertThat(stats.getMrr()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getChurnRate()).isEqualTo(0.0);
        assertThat(stats.getConversionRate()).isEqualTo(0.0);
        assertThat(stats.getNewSignupsLast30Days()).isEqualTo(0L);
        verify(instanceRepository, Mockito.never()).findAll();
        verify(subscriptionRepository, Mockito.never()).findAll();
    }

    @Test
    @DisplayName("Conversion rate derives from status-count map (no findAll)")
    void testConversionRate_Calculation() {
        DashboardStats stats = analyticsService.getDashboardStats();
        // 1 ACTIVE / (1 TRIAL + 1 ACTIVE) = 50%
        assertThat(stats.getConversionRate()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(0.01));
    }
}
