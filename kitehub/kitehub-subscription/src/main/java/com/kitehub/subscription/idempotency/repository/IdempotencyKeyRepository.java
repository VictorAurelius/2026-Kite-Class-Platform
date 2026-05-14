package com.kitehub.subscription.idempotency.repository;

import com.kitehub.subscription.idempotency.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Repository for {@link IdempotencyKey}. GAP-536 Wave 77 Bucket D.
 *
 * @since Wave 77 — GAP-536
 */
@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    Optional<IdempotencyKey> findByKeyAndEndpoint(String key, String endpoint);

    /** Periodic cleanup — invoked by {@code IdempotencyCleanupJob}. */
    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") OffsetDateTime cutoff);
}
