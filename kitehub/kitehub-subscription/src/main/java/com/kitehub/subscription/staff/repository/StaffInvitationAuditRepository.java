package com.kitehub.subscription.staff.repository;

import com.kitehub.subscription.staff.entity.StaffInvitationAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link StaffInvitationAuditEntry} (Wave 80 Bucket B, GAP-561b).
 *
 * @since Wave 80 — GAP-561b
 */
@Repository
public interface StaffInvitationAuditRepository
        extends JpaRepository<StaffInvitationAuditEntry, UUID> {

    List<StaffInvitationAuditEntry> findAllByInvitationIdOrderByOccurredAtAsc(UUID invitationId);

    List<StaffInvitationAuditEntry> findAllByTenantIdOrderByOccurredAtDesc(UUID tenantId);
}
