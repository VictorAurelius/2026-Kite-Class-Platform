package com.kitehub.branding.lifecycle.repository;

import com.kitehub.branding.lifecycle.entity.BrandingLifecycleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BrandingLifecycleEventRepository
    extends JpaRepository<BrandingLifecycleEvent, UUID> {

    /**
     * Events for an instance, newest-first, optionally bounded by ts and limit.
     * Pagination uses offset because event volume per-instance is low (≤200 typical).
     */
    @Query("SELECT e FROM BrandingLifecycleEvent e "
        + "WHERE e.instanceId = :instanceId "
        + "  AND (:since IS NULL OR e.occurredAt >= :since) "
        + "ORDER BY e.occurredAt DESC, e.id DESC")
    List<BrandingLifecycleEvent> findByInstanceIdSince(
        @Param("instanceId") UUID instanceId,
        @Param("since") LocalDateTime since,
        org.springframework.data.domain.Pageable pageable);
}
