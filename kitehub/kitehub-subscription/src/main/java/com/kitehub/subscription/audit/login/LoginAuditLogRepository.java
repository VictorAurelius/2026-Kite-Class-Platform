package com.kitehub.subscription.audit.login;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link LoginAuditLog}.
 *
 * @since 1.0.0 (Wave 72b Bucket C GAP-517)
 */
@Repository
public interface LoginAuditLogRepository extends JpaRepository<LoginAuditLog, Long> {

    /**
     * Find the most recent row for {@code (userId, fingerprintHash)} since
     * {@code since}. Used by {@link LoginAuditService} to decide whether a
     * fingerprint is "new" and whether the cooldown window has elapsed.
     */
    @Query("""
        SELECT l FROM LoginAuditLog l
        WHERE l.userId = :userId
          AND l.fingerprintHash = :fingerprint
          AND l.loginAt >= :since
        ORDER BY l.loginAt DESC
        """)
    Optional<LoginAuditLog> findRecentByUserAndFingerprint(
        @Param("userId") UUID userId,
        @Param("fingerprint") String fingerprint,
        @Param("since") LocalDateTime since);
}
