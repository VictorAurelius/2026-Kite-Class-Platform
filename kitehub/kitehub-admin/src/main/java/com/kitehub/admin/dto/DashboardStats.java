package com.kitehub.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Dashboard statistics DTO.
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    /**
     * Total number of instances.
     */
    private Long totalInstances;

    /**
     * Instances by status (TRIAL, ACTIVE, SUSPENDED, EXPIRED).
     */
    private Map<String, Long> instancesByStatus;

    /**
     * Instances by subscription tier (FREE, BASIC, PREMIUM, ENTERPRISE).
     */
    private Map<String, Long> instancesByTier;

    /**
     * Monthly Recurring Revenue (MRR) in VNĐ.
     */
    private BigDecimal mrr;

    /**
     * Annual Recurring Revenue (ARR = MRR × 12) in VNĐ.
     */
    private BigDecimal arr;

    /**
     * Churn rate (percentage of cancelled subscriptions).
     */
    private Double churnRate;

    /**
     * Trial to paid conversion rate (percentage).
     */
    private Double conversionRate;

    /**
     * New signups in the last 30 days.
     */
    private Long newSignupsLast30Days;

    /**
     * Total active users across all instances.
     */
    private Long totalActiveUsers;

    /**
     * Revenue breakdown by tier.
     */
    private Map<String, BigDecimal> revenueByTier;

    /**
     * Timestamp when stats were calculated.
     */
    private LocalDateTime calculatedAt;
}
