package com.kitehub.subscription.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for kitehub-subscription module outbox events.
 *
 * <p>Writes happen inside the originating service transaction (TrialToPaidService,
 * InstancePurgeService, EmailServiceClient). Reads are used by tests (assert event
 * produced) and the future dispatcher (drain undispatched records).</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192) — generalized in GAP-222c
 */
@Repository
public interface SubscriptionOutboxRepository extends JpaRepository<SubscriptionOutboxEvent, UUID> {

    /** All events for an instance, newest-first. Used by tests + audit query. */
    List<SubscriptionOutboxEvent> findByInstanceIdOrderByCreatedAtDesc(UUID instanceId);

    /** Undispatched events (dispatcher drains this). Bounded result — add paging in dispatcher. */
    List<SubscriptionOutboxEvent> findByDispatchedAtIsNullOrderByCreatedAtAsc();
}
