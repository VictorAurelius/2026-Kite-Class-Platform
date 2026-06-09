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
     * Events for an instance, newest-first, bounded by ts and limit.
     * Pagination uses offset because event volume per-instance is low (≤200 typical).
     *
     * <p>{@code since} MUST be non-null — the controller's {@code parseSince}
     * defaults to {@code now - 30d} when the query param is absent. The earlier
     * {@code (:since IS NULL OR ...)} form failed on PostgreSQL with
     * {@code 42P18 "could not determine data type of parameter"} because a bare
     * parameter used only in {@code ? IS NULL} gives Postgres no type to infer
     * at PREPARE time (H2 tolerated it, masking the bug). Per
     * {@code postgres-specific-type-testcontainers.md}, the null branch is
     * removed entirely rather than CAST-hinted.
     */
    @Query("SELECT e FROM BrandingLifecycleEvent e "
        + "WHERE e.instanceId = :instanceId "
        + "  AND e.occurredAt >= :since "
        + "ORDER BY e.occurredAt DESC, e.id DESC")
    List<BrandingLifecycleEvent> findByInstanceIdSince(
        @Param("instanceId") UUID instanceId,
        @Param("since") LocalDateTime since,
        org.springframework.data.domain.Pageable pageable);
}
