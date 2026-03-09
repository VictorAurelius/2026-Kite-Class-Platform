package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * Find subscription by ID (excluding soft deleted).
     *
     * @param id Subscription UUID
     * @return Optional subscription
     */
    @Query("SELECT s FROM Subscription s WHERE s.id = :id AND s.deleted = false")
    Optional<Subscription> findById(@Param("id") UUID id);

    /**
     * Find active subscription for instance.
     *
     * @param instanceId Instance UUID
     * @return Optional subscription
     */
    @Query("SELECT s FROM Subscription s WHERE s.instanceId = :instanceId " +
           "AND s.status = 'ACTIVE' AND s.deleted = false ORDER BY s.createdAt DESC")
    Optional<Subscription> findActiveByInstanceId(@Param("instanceId") UUID instanceId);

    /**
     * Find all subscriptions for instance.
     *
     * @param instanceId Instance UUID
     * @return List of subscriptions
     */
    @Query("SELECT s FROM Subscription s WHERE s.instanceId = :instanceId AND s.deleted = false")
    List<Subscription> findByInstanceId(@Param("instanceId") UUID instanceId);

    /**
     * Find subscriptions by status.
     *
     * @param status Subscription status
     * @return List of subscriptions
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.deleted = false")
    List<Subscription> findByStatus(@Param("status") SubscriptionStatus status);

    /**
     * Find subscriptions expiring soon (for renewal reminders).
     *
     * @param expiresAfter Start of time range
     * @param expiresBefore End of time range
     * @return List of subscriptions expiring in range
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' " +
           "AND s.expiresAt > :expiresAfter AND s.expiresAt <= :expiresBefore " +
           "AND s.autoRenew = true AND s.deleted = false")
    List<Subscription> findExpiringSoon(
        @Param("expiresAfter") LocalDateTime expiresAfter,
        @Param("expiresBefore") LocalDateTime expiresBefore
    );

    /**
     * Find expired subscriptions.
     *
     * @param now Current timestamp
     * @return List of expired subscriptions
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' " +
           "AND s.expiresAt < :now AND s.deleted = false")
    List<Subscription> findExpired(@Param("now") LocalDateTime now);
}
