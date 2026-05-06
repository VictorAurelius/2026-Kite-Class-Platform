package com.kitehub.subscription.consent.repository;

import com.kitehub.subscription.consent.entity.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ConsentRecord}.
 *
 * @since Wave 25 Bucket A — GAP-353b
 */
@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {

    /**
     * Find the most recent (non-revoked or revoked — caller decides) record
     * for a visitor. Used by the idempotent upsert path: if an active record
     * exists we update in place; otherwise insert new.
     */
    Optional<ConsentRecord> findFirstByVisitorIdOrderByCreatedAtDesc(UUID visitorId);

    /**
     * Hard-delete records whose creation timestamp is older than the DR-03
     * cutoff. Returns count deleted for cron logging.
     */
    @Modifying
    @Query("DELETE FROM ConsentRecord c WHERE c.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") OffsetDateTime cutoff);
}
