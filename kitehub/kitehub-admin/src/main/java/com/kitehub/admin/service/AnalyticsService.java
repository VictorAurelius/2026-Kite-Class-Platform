package com.kitehub.admin.service;

import com.kitehub.admin.config.CacheConfig;
import com.kitehub.admin.dto.DashboardStats;
import com.kitehub.admin.dto.RevenueReport;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for calculating platform analytics and metrics.
 *
 * <p><strong>GAP-432 (Wave 41 Bucket C):</strong> three prior {@code findAll()}
 * callsites at lines 57/58/129 were replaced with DB-side aggregations
 * ({@code count}, {@code sum}, {@code GROUP BY}). Cold-cache dashboard render
 * no longer streams every {@code Instance} + {@code Subscription} row through
 * Java. Caffeine 5-min TTL still smooths warm-cache; aggregation pushes the
 * first-load envelope from O(N) to a small fixed number of count/sum queries.</p>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final InstanceRepository instanceRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Get dashboard statistics.
     *
     * <p>Cached in {@link CacheConfig#ADMIN_DASHBOARD_CACHE} (Caffeine, 5-min TTL).
     * Closes <strong>GAP-126</strong> + <strong>GAP-432</strong>: previously every
     * admin page load triggered two unbounded {@code findAll()} scans plus six
     * in-memory aggregations. Now uses DB-side {@code COUNT(...) GROUP BY} +
     * {@code SUM(...)} on indexed columns.</p>
     */
    @Cacheable(value = CacheConfig.ADMIN_DASHBOARD_CACHE, key = "'stats'")
    public DashboardStats getDashboardStats() {
        log.info("Calculating dashboard statistics (DB-side aggregation, GAP-432)");

        long totalInstances = instanceRepository.countByDeletedFalse();
        Map<String, Long> instancesByStatus = instanceRepository.countInstancesByStatus();
        Map<String, Long> instancesByTier = subscriptionRepository.countSubscriptionsByTier();

        BigDecimal mrr = BigDecimal.valueOf(subscriptionRepository.sumActiveMrr());
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12));

        double churnRate = calculateChurnRate(instancesByStatus, totalInstances);
        double conversionRate = calculateConversionRate(instancesByStatus);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long newSignupsLast30Days =
            instanceRepository.countByDeletedFalseAndCreatedAtAfter(thirtyDaysAgo);

        long activeInstances = instancesByStatus.getOrDefault(InstanceStatus.ACTIVE.name(), 0L);
        long totalActiveUsers = activeInstances * 10;

        Map<String, BigDecimal> revenueByTier = revenueByTierFromDb();

        return DashboardStats.builder()
                .totalInstances(totalInstances)
                .instancesByStatus(instancesByStatus)
                .instancesByTier(instancesByTier)
                .mrr(mrr)
                .arr(arr)
                .churnRate(churnRate)
                .conversionRate(conversionRate)
                .newSignupsLast30Days(newSignupsLast30Days)
                .totalActiveUsers(totalActiveUsers)
                .revenueByTier(revenueByTier)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Get revenue report for a period.
     *
     * <p><strong>GAP-432:</strong> previously fetched every subscription via
     * {@code findAll()} and filtered in Java. Now uses
     * {@link SubscriptionRepository#findActiveInPeriod} with a DB-side range
     * filter so only matching rows are streamed.</p>
     */
    @Cacheable(value = CacheConfig.ADMIN_REVENUE_REPORT_CACHE,
            key = "T(java.util.Objects).hash(#period, #startDate, #endDate)")
    public RevenueReport getRevenueReport(String period, LocalDate startDate, LocalDate endDate) {
        log.info("Generating revenue report for period: {}, {} to {} (GAP-432 bounded)",
                period, startDate, endDate);

        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atTime(23, 59, 59);
        List<Subscription> activeInPeriod =
            subscriptionRepository.findActiveInPeriod(rangeStart, rangeEnd);

        BigDecimal totalRevenue = activeInPeriod.stream()
                .map(s -> BigDecimal.valueOf(s.getPriceVnd()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RevenueReport.RevenueTierBreakdown> revenueByTier =
            calculateRevenueByTierList(activeInPeriod);

        List<RevenueReport.DailyRevenue> dailyRevenue =
            generateDailyRevenue(startDate, endDate, totalRevenue);

        BigDecimal mrr = BigDecimal.valueOf(subscriptionRepository.sumActiveMrr());
        BigDecimal projectedArr = mrr.multiply(BigDecimal.valueOf(12));
        BigDecimal churnImpact = BigDecimal.valueOf(subscriptionRepository.sumCancelledRevenue());

        return RevenueReport.builder()
                .period(period)
                .startDate(startDate)
                .endDate(endDate)
                .totalRevenue(totalRevenue)
                .revenueByTier(revenueByTier)
                .dailyRevenue(dailyRevenue)
                .mrr(mrr)
                .projectedArr(projectedArr)
                .churnImpact(churnImpact)
                .build();
    }

    private double calculateChurnRate(Map<String, Long> instancesByStatus, long totalInstances) {
        if (totalInstances == 0) {
            return 0.0;
        }
        long churnedInstances =
            instancesByStatus.getOrDefault(InstanceStatus.SUSPENDED.name(), 0L)
            + instancesByStatus.getOrDefault(InstanceStatus.DELETED.name(), 0L);
        return (churnedInstances * 100.0) / totalInstances;
    }

    private double calculateConversionRate(Map<String, Long> instancesByStatus) {
        long trial = instancesByStatus.getOrDefault(InstanceStatus.TRIAL.name(), 0L);
        long active = instancesByStatus.getOrDefault(InstanceStatus.ACTIVE.name(), 0L);
        long total = trial + active;
        if (total == 0) {
            return 0.0;
        }
        return (active * 100.0) / total;
    }

    private Map<String, BigDecimal> revenueByTierFromDb() {
        Map<String, BigDecimal> revenueMap = new HashMap<>();
        for (Object[] row : subscriptionRepository.sumActiveRevenueByTier()) {
            PricingTier tier = (PricingTier) row[0];
            Number sum = (Number) row[1];
            revenueMap.put(tier.name(), BigDecimal.valueOf(sum.longValue()));
        }
        return revenueMap;
    }

    private List<RevenueReport.RevenueTierBreakdown> calculateRevenueByTierList(
            List<Subscription> subscriptions) {
        Map<String, List<Subscription>> tierGroups = new HashMap<>();
        for (Subscription s : subscriptions) {
            tierGroups.computeIfAbsent(s.getTier().name(), k -> new ArrayList<>()).add(s);
        }

        List<RevenueReport.RevenueTierBreakdown> result = new ArrayList<>();
        tierGroups.forEach((tier, subs) -> {
            BigDecimal tierRevenue = subs.stream()
                    .filter(s -> SubscriptionStatus.ACTIVE.equals(s.getStatus()))
                    .map(s -> BigDecimal.valueOf(s.getPriceVnd()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Fallback: if no ACTIVE in tier, sum all (preserve prior semantics).
            if (tierRevenue.compareTo(BigDecimal.ZERO) == 0) {
                tierRevenue = subs.stream()
                        .map(s -> BigDecimal.valueOf(s.getPriceVnd()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            result.add(RevenueReport.RevenueTierBreakdown.builder()
                    .tier(tier)
                    .revenue(tierRevenue)
                    .subscriptionCount((long) subs.size())
                    .build());
        });

        return result;
    }

    private List<RevenueReport.DailyRevenue> generateDailyRevenue(
            LocalDate startDate, LocalDate endDate, BigDecimal totalRevenue) {
        List<RevenueReport.DailyRevenue> dailyData = new ArrayList<>();

        long dayCount = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        BigDecimal dailyAmount = dayCount > 0
            ? totalRevenue.divide(BigDecimal.valueOf(dayCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dailyData.add(RevenueReport.DailyRevenue.builder()
                    .date(currentDate)
                    .revenue(dailyAmount)
                    .build());
            currentDate = currentDate.plusDays(1);
        }

        return dailyData;
    }
}
