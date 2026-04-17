package com.kitehub.admin.service;

import com.kitehub.admin.dto.DashboardStats;
import com.kitehub.admin.dto.RevenueReport;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for calculating platform analytics and metrics.
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
     * @return dashboard stats
     */
    public DashboardStats getDashboardStats() {
        log.info("Calculating dashboard statistics");

        List<Instance> allInstances = instanceRepository.findAll();
        List<Subscription> allSubscriptions = subscriptionRepository.findAll();

        // Total instances
        long totalInstances = allInstances.size();

        // Instances by status
        Map<String, Long> instancesByStatus = allInstances.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getStatus().name(),
                        Collectors.counting()
                ));

        // Instances by tier (from subscription)
        Map<String, Long> instancesByTier = allSubscriptions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getTier().name(),
                        Collectors.counting()
                ));

        // Calculate MRR
        BigDecimal mrr = calculateMRR(allSubscriptions);
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12));

        // Calculate churn rate
        double churnRate = calculateChurnRate(allInstances);

        // Calculate conversion rate
        double conversionRate = calculateConversionRate(allInstances);

        // New signups last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long newSignupsLast30Days = allInstances.stream()
                .filter(i -> i.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

        // Total active users (mock - would query user service)
        long totalActiveUsers = allInstances.stream()
                .filter(i -> InstanceStatus.ACTIVE.equals(i.getStatus()))
                .count() * 10; // Estimate: 10 users per active instance

        // Revenue by tier
        Map<String, BigDecimal> revenueByTier = calculateRevenueByTier(allSubscriptions);

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
     * @param period    report period (DAILY, MONTHLY, YEARLY)
     * @param startDate start date
     * @param endDate   end date
     * @return revenue report
     */
    public RevenueReport getRevenueReport(String period, LocalDate startDate, LocalDate endDate) {
        log.info("Generating revenue report for period: {}, {} to {}", period, startDate, endDate);

        List<Subscription> allSubscriptions = subscriptionRepository.findAll();

        // Filter subscriptions active in the period
        List<Subscription> activeSubscriptions = allSubscriptions.stream()
                .filter(s -> isActiveInPeriod(s, startDate, endDate))
                .collect(Collectors.toList());

        // Calculate total revenue
        BigDecimal totalRevenue = activeSubscriptions.stream()
                .map(s -> BigDecimal.valueOf(s.getPriceVnd()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Revenue by tier
        List<RevenueReport.RevenueTierBreakdown> revenueByTier = calculateRevenueByTierList(activeSubscriptions);

        // Daily revenue (mock data for now)
        List<RevenueReport.DailyRevenue> dailyRevenue = generateDailyRevenue(startDate, endDate, totalRevenue);

        // MRR and projected ARR
        BigDecimal mrr = calculateMRR(allSubscriptions);
        BigDecimal projectedArr = mrr.multiply(BigDecimal.valueOf(12));

        // Churn impact (revenue lost)
        BigDecimal churnImpact = calculateChurnImpact(allSubscriptions);

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

    /**
     * Calculate Monthly Recurring Revenue (MRR).
     */
    private BigDecimal calculateMRR(List<Subscription> subscriptions) {
        return subscriptions.stream()
                .filter(s -> SubscriptionStatus.ACTIVE.equals(s.getStatus()))
                .map(s -> BigDecimal.valueOf(s.getPriceVnd()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate churn rate (percentage of cancelled/expired instances).
     */
    private double calculateChurnRate(List<Instance> instances) {
        long totalInstances = instances.size();
        if (totalInstances == 0) {
            return 0.0;
        }

        long churnedInstances = instances.stream()
                .filter(i -> InstanceStatus.SUSPENDED.equals(i.getStatus()) || InstanceStatus.DELETED.equals(i.getStatus()))
                .count();

        return (churnedInstances * 100.0) / totalInstances;
    }

    /**
     * Calculate trial to paid conversion rate.
     */
    private double calculateConversionRate(List<Instance> instances) {
        long trialInstances = instances.stream()
                .filter(i -> InstanceStatus.TRIAL.equals(i.getStatus()))
                .count();

        long activeInstances = instances.stream()
                .filter(i -> InstanceStatus.ACTIVE.equals(i.getStatus()))
                .count();

        long totalNonTrial = trialInstances + activeInstances;
        if (totalNonTrial == 0) {
            return 0.0;
        }

        return (activeInstances * 100.0) / totalNonTrial;
    }

    /**
     * Calculate revenue by tier.
     */
    private Map<String, BigDecimal> calculateRevenueByTier(List<Subscription> subscriptions) {
        Map<String, BigDecimal> revenueMap = new HashMap<>();

        subscriptions.stream()
                .filter(s -> SubscriptionStatus.ACTIVE.equals(s.getStatus()))
                .forEach(s -> {
                    String tier = s.getTier().name();
                    BigDecimal currentRevenue = revenueMap.getOrDefault(tier, BigDecimal.ZERO);
                    revenueMap.put(tier, currentRevenue.add(BigDecimal.valueOf(s.getPriceVnd())));
                });

        return revenueMap;
    }

    /**
     * Calculate revenue by tier as list.
     */
    private List<RevenueReport.RevenueTierBreakdown> calculateRevenueByTierList(List<Subscription> subscriptions) {
        Map<String, List<Subscription>> tierGroups = subscriptions.stream()
                .collect(Collectors.groupingBy(s -> s.getTier().name()));

        List<RevenueReport.RevenueTierBreakdown> result = new ArrayList<>();

        tierGroups.forEach((tier, subs) -> {
            BigDecimal tierRevenue = subs.stream()
                    .map(s -> BigDecimal.valueOf(s.getPriceVnd()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(RevenueReport.RevenueTierBreakdown.builder()
                    .tier(tier)
                    .revenue(tierRevenue)
                    .subscriptionCount((long) subs.size())
                    .build());
        });

        return result;
    }

    /**
     * Check if subscription was active during the period.
     */
    private boolean isActiveInPeriod(Subscription subscription, LocalDate startDate, LocalDate endDate) {
        LocalDate subStartDate = subscription.getStartedAt().toLocalDate();
        LocalDate subEndDate = subscription.getExpiresAt().toLocalDate();

        // Subscription overlaps with the period
        return !subStartDate.isAfter(endDate) &&
               (subEndDate == null || !subEndDate.isBefore(startDate));
    }

    /**
     * Generate daily revenue data (mock for now).
     */
    private List<RevenueReport.DailyRevenue> generateDailyRevenue(LocalDate startDate, LocalDate endDate, BigDecimal totalRevenue) {
        List<RevenueReport.DailyRevenue> dailyData = new ArrayList<>();

        // Simple distribution: divide total revenue evenly
        long dayCount = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        BigDecimal dailyAmount = dayCount > 0 ? totalRevenue.divide(BigDecimal.valueOf(dayCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

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

    /**
     * Calculate churn impact (revenue lost from cancelled subscriptions).
     */
    private BigDecimal calculateChurnImpact(List<Subscription> subscriptions) {
        return subscriptions.stream()
                .filter(s -> SubscriptionStatus.CANCELLED.equals(s.getStatus()))
                .map(s -> BigDecimal.valueOf(s.getPriceVnd()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
