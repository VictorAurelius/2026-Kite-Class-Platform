package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Subscription entity.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("SELECT s FROM Subscription s WHERE s.id = :id AND s.deleted = false")
    Optional<Subscription> findById(@Param("id") UUID id);

    @Query("SELECT s FROM Subscription s WHERE s.instanceId = :instanceId " +
           "AND s.status = 'ACTIVE' AND s.deleted = false ORDER BY s.createdAt DESC")
    Optional<Subscription> findActiveByInstanceId(@Param("instanceId") UUID instanceId);

    @Query("SELECT s FROM Subscription s WHERE s.instanceId = :instanceId AND s.deleted = false")
    List<Subscription> findByInstanceId(@Param("instanceId") UUID instanceId);

    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.deleted = false")
    List<Subscription> findByStatus(@Param("status") SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' " +
           "AND s.expiresAt > :expiresAfter AND s.expiresAt <= :expiresBefore " +
           "AND s.autoRenew = true AND s.deleted = false")
    List<Subscription> findExpiringSoon(
        @Param("expiresAfter") LocalDateTime expiresAfter,
        @Param("expiresBefore") LocalDateTime expiresBefore
    );

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' " +
           "AND s.expiresAt < :now AND s.deleted = false")
    List<Subscription> findExpired(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Subscription s WHERE s.status = :status " +
           "AND s.expiresAt >= :startDate AND s.expiresAt <= :endDate " +
           "AND s.deleted = false")
    List<Subscription> findExpiringBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("status") SubscriptionStatus status
    );

    @Query("SELECT s FROM Subscription s WHERE (s.status = 'ACTIVE' OR s.status = 'EXPIRED') " +
           "AND s.expiresAt < :now AND s.deleted = false")
    List<Subscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    // GAP-1017: end-of-cycle cancellations — CANCELLED subs whose expiresAt has passed
    // are skipped by findExpiredSubscriptions, so their instances never get suspended.
    @Query("SELECT s FROM Subscription s WHERE s.status = 'CANCELLED' " +
           "AND s.expiresAt < :now AND s.deleted = false")
    List<Subscription> findCancelledExpiredSubscriptions(@Param("now") LocalDateTime now);

    // =========================================================
    // GAP-432 Wave 41 Bucket C: bounded analytics aggregations
    // (replace prior unbounded findAll() callsites in AnalyticsService).
    // =========================================================

    /** DB-side aggregation: tier -> count for non-deleted subscriptions. */
    @Query("SELECT s.tier, COUNT(s) FROM Subscription s WHERE s.deleted = false GROUP BY s.tier")
    List<Object[]> countGroupByTier();

    /** Sum priceVnd for ACTIVE non-deleted subscriptions (= MRR). */
    @Query("SELECT COALESCE(SUM(s.priceVnd), 0) FROM Subscription s " +
           "WHERE s.status = 'ACTIVE' AND s.deleted = false")
    Long sumActiveMrr();

    /** Sum priceVnd for CANCELLED non-deleted subscriptions (= churn impact). */
    @Query("SELECT COALESCE(SUM(s.priceVnd), 0) FROM Subscription s " +
           "WHERE s.status = 'CANCELLED' AND s.deleted = false")
    Long sumCancelledRevenue();

    /** DB-side aggregation: tier -> revenue sum for ACTIVE non-deleted subscriptions. */
    @Query("SELECT s.tier, COALESCE(SUM(s.priceVnd), 0) FROM Subscription s " +
           "WHERE s.status = 'ACTIVE' AND s.deleted = false GROUP BY s.tier")
    List<Object[]> sumActiveRevenueByTier();

    /** Convenience: tier name -> count (string-keyed, for DTO compatibility). */
    default Map<String, Long> countSubscriptionsByTier() {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : countGroupByTier()) {
            PricingTier tier = (PricingTier) row[0];
            Long count = ((Number) row[1]).longValue();
            result.put(tier.name(), count);
        }
        return result;
    }

    /**
     * Find subscriptions overlapping a date range. Replaces prior
     * findAll() + in-memory filter in AnalyticsService.getRevenueReport.
     *
     * <p>A subscription is "active in period" if its {@code startedAt} is on/before
     * {@code rangeEnd} AND its {@code expiresAt} is null or on/after {@code rangeStart}.</p>
     */
    @Query("SELECT s FROM Subscription s WHERE s.deleted = false " +
           "AND s.startedAt <= :rangeEnd " +
           "AND (s.expiresAt IS NULL OR s.expiresAt >= :rangeStart)")
    List<Subscription> findActiveInPeriod(
        @Param("rangeStart") LocalDateTime rangeStart,
        @Param("rangeEnd") LocalDateTime rangeEnd
    );
}
