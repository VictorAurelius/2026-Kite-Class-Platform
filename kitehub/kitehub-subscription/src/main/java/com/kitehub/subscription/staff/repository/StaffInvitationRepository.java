package com.kitehub.subscription.staff.repository;

import com.kitehub.subscription.staff.entity.StaffInvitation;
import com.kitehub.subscription.staff.entity.StaffInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link StaffInvitation}.
 *
 * <p>All queries parameterized — no string concatenation per
 * {@code .claude/rules/pre-launch-owasp-rest-hardening-checklist.md} §2.3
 * (A03 Injection guard).</p>
 *
 * @since Wave 79 — GAP-561
 */
@Repository
public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, UUID> {

    Optional<StaffInvitation> findByTokenHash(String tokenHash);

    /**
     * Resolve a STAFF user's tenant binding for JWT enrichment (GAP-531 follow-up,
     * Wave flow-kc2). After a recipient accepts an invitation, their tenant is the
     * invitation's {@code tenantId}; {@code accepted_user_id} is the JWT subject.
     */
    Optional<StaffInvitation> findFirstByAcceptedUserIdAndStatus(
            UUID acceptedUserId, StaffInvitationStatus status);

    /** Pending invitation for the same tenant+email (idempotency guard for re-invite). */
    @Query("SELECT s FROM StaffInvitation s WHERE s.tenantId = :tenantId AND s.email = :email AND s.status = 'PENDING'")
    Optional<StaffInvitation> findPendingByTenantAndEmail(
            @Param("tenantId") UUID tenantId,
            @Param("email") String email);

    List<StaffInvitation> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Query("SELECT s FROM StaffInvitation s WHERE s.status = :status AND s.expiresAt < :now")
    List<StaffInvitation> findExpired(
            @Param("status") StaffInvitationStatus status,
            @Param("now") OffsetDateTime now);

    long countByTenantIdAndStatus(UUID tenantId, StaffInvitationStatus status);
}
