package com.kitehub.subscription.audit.login;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link LoginAuditLog}.
 *
 * @since 1.0.0 (Wave 72b Bucket C GAP-517)
 */
@Repository
public interface LoginAuditLogRepository extends JpaRepository<LoginAuditLog, Long> {

    /**
     * Find rows for {@code (userId, fingerprintHash)} since {@code since},
     * ordered by most recent first. Caller passes {@code PageRequest.of(0, 1)}
     * when only the latest row is needed; this bounds the SQL with {@code LIMIT 1}
     * via Spring Data so a multi-row hit never triggers
     * {@code IncorrectResultSizeDataAccessException} (formerly emitted as
     * "Query did not return a unique result" WARN — GAP-707, fix shipped Wave 104 Bucket D).
     *
     * <p>Returns {@code List} rather than {@code Optional} because cooldown
     * windows commonly contain multiple matching rows (each successful login
     * adds a row); the prior single-result signature relied on caller-side
     * filtering and emitted a deterministic WARN per login. Pageable-bounded
     * List makes the contract explicit.</p>
     */
    @Query("""
        SELECT l FROM LoginAuditLog l
        WHERE l.userId = :userId
          AND l.fingerprintHash = :fingerprint
          AND l.loginAt >= :since
        ORDER BY l.loginAt DESC
        """)
    List<LoginAuditLog> findRecentByUserAndFingerprint(
        @Param("userId") UUID userId,
        @Param("fingerprint") String fingerprint,
        @Param("since") LocalDateTime since,
        Pageable pageable);
}
