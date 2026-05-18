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

    /**
     * Token validation lookup at signup time.
     *
     * <p>GAP-610 (Wave 91 Bucket D) — defensive hardening: explicit JPQL with
     * named parameter eliminates any chance of Spring Data method-derivation
     * ambiguity around the {@code UUID → PostgreSQL uuid} type binding.
     * Equivalent to the prior method-derived query
     * {@code SELECT b FROM BetaAccessRequest b WHERE b.inviteToken = ?1},
     * but the explicit form guarantees Hibernate emits
     * {@code WHERE invite_token = :token} with the parameter bound as the
     * native Postgres {@code uuid} type — matching the column declaration in
     * {@code V28__create_beta_access_request.sql} and the entity field
     * {@code @Column(name = "invite_token") private UUID inviteToken}.</p>
     *
     * <p>State-check on 2026-05-18 (Wave 91 Bucket D — see PR body §"Phase 1
     * state-check verdicts") confirmed: (a) no RLS policy on
     * {@code beta_access_request} table (V34 enables RLS only on
     * {@code instance_id}-keyed tables; V50 only on {@code admin_audit_logs}),
     * (b) UUID encoding via Hibernate native, (c) JPA query method-derived
     * was correct in theory but explicit JPQL adds defense in depth.</p>
     */
    @Query("SELECT b FROM BetaAccessRequest b WHERE b.inviteToken = :token")
    Optional<BetaAccessRequest> findByInviteToken(@Param("token") UUID token);

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

    /**
     * GAP-600 Wave 92 Bucket C — bulk mark stale PENDING requests as ABORTED.
     *
     * <p>Cleanup sweep query used by
     * {@link com.kitehub.subscription.beta.scheduler.BetaRequestAbortCleanupScheduler}.
     * Sweeps rows with status PENDING created before the threshold cutoff →
     * flips to ABORTED (terminal state preserving audit trail).</p>
     *
     * <p>Index support: V53 composite index {@code (status, created_at)} cho
     * planner index-only scan. Default cron mỗi 6h, threshold 24h —
     * configurable via {@code kitehub.beta.cleanup.*}.</p>
     *
     * @param threshold rows với {@code created_at < threshold} đủ điều kiện sweep
     * @param now       timestamp set into {@code updated_at}
     * @return số rows flipped PENDING → ABORTED
     */
    @Modifying
    @Query("UPDATE BetaAccessRequest b SET b.status = "
            + "com.kitehub.subscription.beta.entity.BetaAccessRequestStatus.ABORTED, "
            + "b.updatedAt = :now "
            + "WHERE b.status = com.kitehub.subscription.beta.entity.BetaAccessRequestStatus.PENDING "
            + "AND b.createdAt < :threshold")
    int markStaleAsAborted(@Param("threshold") OffsetDateTime threshold,
                            @Param("now") OffsetDateTime now);

    /**
     * GAP-600 Wave 92 Bucket C — count stale PENDING rows (used for logging
     * before bulk update fires, and by metrics if wired later).
     */
    @Query("SELECT COUNT(b) FROM BetaAccessRequest b "
            + "WHERE b.status = com.kitehub.subscription.beta.entity.BetaAccessRequestStatus.PENDING "
            + "AND b.createdAt < :threshold")
    long countStalePending(@Param("threshold") OffsetDateTime threshold);
}
