package com.kitehub.subscription.beta.repository;

import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    /** Duplicate-submission guard: only one PENDING request per email at a time. */
    Optional<BetaAccessRequest> findFirstByEmailAndStatusOrderByCreatedAtDesc(String email, BetaAccessRequestStatus status);
}
