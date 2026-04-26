package com.kitehub.branding.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for branding-domain outbox events (per ADR-021).
 *
 * <p>Writes happen inside the calling service transaction (e.g.
 * {@code BrandingJobService.createJob}). Reads serve tests + the future
 * broker dispatcher.</p>
 *
 * @since Wave 7 (GAP-222a Phase 2)
 */
@Repository
public interface BrandingOutboxRepository extends JpaRepository<BrandingOutboxEvent, UUID> {

    List<BrandingOutboxEvent> findByAggregateIdOrderByCreatedAtDesc(UUID aggregateId);

    List<BrandingOutboxEvent> findByDispatchedAtIsNullOrderByCreatedAtAsc();
}
