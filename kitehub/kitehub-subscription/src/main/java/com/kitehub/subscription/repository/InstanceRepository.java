package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Instance entity.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Repository
public interface InstanceRepository extends JpaRepository<Instance, UUID> {

    /**
     * Find instance by subdomain (not deleted).
     *
     * @param subdomain subdomain to search
     * @return Optional containing instance if found
     */
    Optional<Instance> findBySubdomainAndDeletedFalse(String subdomain);

    /**
     * Find all instances by owner ID (not deleted).
     *
     * @param ownerId owner UUID
     * @return list of instances
     */
    List<Instance> findByOwnerIdAndDeletedFalse(UUID ownerId);

    /**
     * Find all instances by status (not deleted).
     *
     * @param status instance status
     * @return list of instances
     */
    List<Instance> findByStatusAndDeletedFalse(InstanceStatus status);

    /**
     * Find expired trial instances.
     *
     * @param now current timestamp
     * @return list of instances with expired trials
     */
    @Query("SELECT i FROM Instance i WHERE i.status = 'TRIAL' " +
           "AND i.trialExpiresAt < :now AND i.deleted = false")
    List<Instance> findExpiredTrials(@Param("now") LocalDateTime now);

    /**
     * Find expired subscriptions.
     *
     * @param now current timestamp
     * @return list of instances with expired subscriptions
     */
    @Query("SELECT i FROM Instance i WHERE i.status = 'ACTIVE' " +
           "AND i.subscriptionExpiresAt < :now AND i.deleted = false")
    List<Instance> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    /**
     * Check if subdomain exists (not deleted).
     *
     * @param subdomain subdomain to check
     * @return true if exists, false otherwise
     */
    boolean existsBySubdomainAndDeletedFalse(String subdomain);

    /**
     * Count instances by owner ID (not deleted).
     *
     * @param ownerId owner UUID
     * @return count of instances
     */
    long countByOwnerIdAndDeletedFalse(UUID ownerId);
}
