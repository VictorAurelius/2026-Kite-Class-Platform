package com.kitehub.subscription.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for migration-domain outbox events.
 *
 * <p>Writes are expected to happen inside the TrialToPaidService transaction.
 * Reads are used by tests (to assert events produced) and the future dispatcher
 * (to drain undispatched records).</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
@Repository
public interface MigrationOutboxRepository extends JpaRepository<MigrationOutboxEvent, UUID> {

    /** All events for an instance, newest-first. Used by tests + future audit query. */
    List<MigrationOutboxEvent> findByInstanceIdOrderByCreatedAtDesc(UUID instanceId);

    /** Undispatched events (dispatcher drains this). Bounded result — add paging in dispatcher. */
    List<MigrationOutboxEvent> findByDispatchedAtIsNullOrderByCreatedAtAsc();
}
