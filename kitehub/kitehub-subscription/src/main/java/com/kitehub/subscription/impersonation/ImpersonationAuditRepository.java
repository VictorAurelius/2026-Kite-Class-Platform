package com.kitehub.subscription.impersonation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ImpersonationAuditEntry}.
 *
 * <p>Queries are intentionally narrow — list-per-admin for the audit panel
 * + active-session lookup for the 30-second timeout enforcement.</p>
 *
 * @since Wave 79 (GAP-040)
 */
@Repository
public interface ImpersonationAuditRepository extends JpaRepository<ImpersonationAuditEntry, Long> {

    /**
     * Most recent N impersonation sessions for the admin audit panel UI.
     */
    Page<ImpersonationAuditEntry> findAllByOrderByStartedAtDesc(Pageable pageable);

    /**
     * Per-admin history (admin-audit-panel filter).
     */
    Page<ImpersonationAuditEntry> findByAdminUserIdOrderByStartedAtDesc(UUID adminUserId, Pageable pageable);

    /**
     * Lookup the still-active session for an admin, if any.
     *
     * <p>By design, only ONE active session per admin is allowed; the service
     * MUST verify there is no row matching this query before starting a new
     * impersonation, OR auto-close the stale row first.</p>
     */
    @Query("""
        SELECT e
          FROM ImpersonationAuditEntry e
         WHERE e.adminUserId = :adminUserId
           AND e.endedAt IS NULL
        """)
    Optional<ImpersonationAuditEntry> findActiveSession(@Param("adminUserId") UUID adminUserId);

    /**
     * Sweep helper for the scheduled auto-expiry job.
     *
     * <p>Returns active rows whose {@code started_at} is older than the
     * provided cutoff — the service marks each as {@link ImpersonationAuditEntry.EndedReason#AUTO_TIMEOUT}.</p>
     */
    @Query("""
        SELECT e
          FROM ImpersonationAuditEntry e
         WHERE e.endedAt IS NULL
           AND e.startedAt < :cutoff
        """)
    List<ImpersonationAuditEntry> findExpiredActiveSessions(@Param("cutoff") java.time.OffsetDateTime cutoff);
}
