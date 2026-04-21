package com.kitehub.subscription.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for idempotency keys (GAP-192 Phase 4b-i).
 *
 * <p>Lookups are keyed on the pair {@code (idempotencyKey, instanceId)} — see entity
 * javadoc for why both fields are needed.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192 Phase 4b-i)
 */
@Repository
public interface MigrationIdempotencyKeyRepository extends JpaRepository<MigrationIdempotencyKey, UUID> {

    /** Lookup for duplicate-request detection. */
    Optional<MigrationIdempotencyKey> findByIdempotencyKeyAndInstanceId(String idempotencyKey, UUID instanceId);

    /**
     * Purge expired keys. Called by the scheduler on a fixed delay so the table
     * stays small. Return value is row-count for metrics.
     */
    @Modifying
    @Query("DELETE FROM MigrationIdempotencyKey k WHERE k.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
