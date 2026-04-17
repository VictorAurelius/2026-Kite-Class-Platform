package com.kitehub.branding.repository;

import com.kitehub.branding.domain.entity.AIUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AI usage log entries.
 *
 * @since 1.0.0
 */
@Repository
public interface AIUsageLogRepository extends JpaRepository<AIUsageLog, UUID> {

    /**
     * Find usage log for a specific instance and date.
     */
    Optional<AIUsageLog> findByInstanceIdAndUsageDate(UUID instanceId, LocalDate usageDate);

    /**
     * Increment the request count for an existing usage log entry.
     * Uses native query for atomic increment.
     */
    @Modifying
    @Query("UPDATE AIUsageLog a SET a.requestCount = a.requestCount + 1 " +
           "WHERE a.instanceId = :instanceId AND a.usageDate = :usageDate")
    int incrementRequestCount(@Param("instanceId") UUID instanceId,
                              @Param("usageDate") LocalDate usageDate);
}
