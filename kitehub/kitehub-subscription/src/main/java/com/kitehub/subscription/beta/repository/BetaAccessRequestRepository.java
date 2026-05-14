package com.kitehub.subscription.beta.repository;

import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link BetaAccessRequest}.
 *
 * <p>Used by {@code BetaAccessService} for the 3-stage Phase 1 BETA invite flow
 * (request → approve → signup). Queries are status-filtered for the coordinator
 * dashboard (PENDING-first listing).</p>
 *
 * @since Wave 33 — GAP-372
 */
@Repository
public interface BetaAccessRequestRepository extends JpaRepository<BetaAccessRequest, Long> {

    /** Coordinator dashboard query: paginated by status, newest first. */
    Page<BetaAccessRequest> findByStatusOrderByCreatedAtDesc(BetaAccessRequestStatus status, Pageable pageable);

    /** Token validation lookup at signup time. */
    Optional<BetaAccessRequest> findByInviteToken(UUID inviteToken);

    /** Claim-code → invite_token exchange (GAP-388 Wave 36 Bucket A 2FA). */
    Optional<BetaAccessRequest> findByClaimCode(String claimCode);

    /** Duplicate-submission guard: only one PENDING request per email at a time. */
    Optional<BetaAccessRequest> findFirstByEmailAndStatusOrderByCreatedAtDesc(String email, BetaAccessRequestStatus status);

    /**
     * GAP-534 Wave 77 — Atomic single-use enforcement on invite_token.
     *
     * <p>The {@code used_at IS NULL} predicate eliminates the race window
     * between two concurrent consumers: PostgreSQL acquires a row-level lock,
     * runs the predicate, and either updates (returning 1) or skips
     * (returning 0). The caller treats {@code 0} as a reuse attempt and
     * routes to the audit-log + 409 Conflict path.</p>
     *
     * @return number of rows updated (1 on first consume, 0 on reuse)
     */
    @Modifying
    @Query("UPDATE BetaAccessRequest b SET b.usedAt = :usedAt, b.consumedIp = :consumedIp, "
            + "b.consumedUserAgent = :consumedUserAgent "
            + "WHERE b.inviteToken = :token AND b.usedAt IS NULL")
    int consumeInviteToken(@Param("token") UUID token,
                            @Param("usedAt") OffsetDateTime usedAt,
                            @Param("consumedIp") String consumedIp,
                            @Param("consumedUserAgent") String consumedUserAgent);
}
