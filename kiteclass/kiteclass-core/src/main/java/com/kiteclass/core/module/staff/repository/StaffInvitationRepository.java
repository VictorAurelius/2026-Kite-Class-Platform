package com.kiteclass.core.module.staff.repository;

import com.kiteclass.core.common.constant.StaffInvitationStatus;
import com.kiteclass.core.module.staff.entity.StaffInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data-access for {@link StaffInvitation}.
 *
 * <p>Token lookup is the primary access pattern (public claim endpoint is
 * keyed by token). Multi-tenant isolation applies — a stolen token from tenant
 * A cannot be redeemed in tenant B because the Hibernate filter clamps the
 * query to the current tenant.
 *
 * @since 2026-05-27 (Wave meta-6 Bucket A — GAP-772)
 */
@Repository
public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, Long> {

    /**
     * @return invitation if the token matches a non-deleted row.
     */
    Optional<StaffInvitation> findByTokenAndDeletedFalse(String token);

    /**
     * Lists invitations by status — consumed by the Owner-side list endpoint
     * and the scheduled sweeper for PENDING/EXPIRED rotation.
     */
    List<StaffInvitation> findByStatusAndDeletedFalseOrderByCreatedAtDesc(
            StaffInvitationStatus status
    );

    /**
     * Lists PENDING invitations whose {@code expiresAt} is already in the past
     * — consumed by the scheduled sweeper that transitions them to EXPIRED.
     */
    List<StaffInvitation> findByStatusAndExpiresAtBeforeAndDeletedFalse(
            StaffInvitationStatus status,
            Instant expiresAtBefore
    );
}
