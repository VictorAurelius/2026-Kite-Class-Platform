package com.kiteclass.core.module.parent.repository;

import com.kiteclass.core.common.constant.ParentInvitationStatus;
import com.kiteclass.core.module.parent.entity.ParentInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data-access for {@link ParentInvitation}.
 *
 * <p>Token lookup is the primary access pattern (public redemption endpoint is
 * keyed by token). Multi-tenant isolation applies — a stolen token from tenant
 * A cannot be redeemed in tenant B because the Hibernate filter clamps the
 * query to the current tenant.
 *
 * @since 2.14.0
 */
@Repository
public interface ParentInvitationRepository extends JpaRepository<ParentInvitation, Long> {

    /**
     * @return invitation if the token matches a non-deleted row.
     */
    Optional<ParentInvitation> findByTokenAndDeletedFalse(String token);

    /**
     * Lists PENDING invitations whose {@code expiresAt} is already in the past
     * — consumed by the scheduled sweeper that transitions them to EXPIRED.
     */
    List<ParentInvitation> findByStatusAndExpiresAtBeforeAndDeletedFalse(
            ParentInvitationStatus status,
            Instant expiresAtBefore
    );
}
