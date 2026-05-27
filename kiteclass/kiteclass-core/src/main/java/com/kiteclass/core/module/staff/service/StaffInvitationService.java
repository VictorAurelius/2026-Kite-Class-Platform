package com.kiteclass.core.module.staff.service;

import com.kiteclass.core.module.staff.dto.AcceptStaffInviteRequest;
import com.kiteclass.core.module.staff.dto.AcceptStaffInviteResult;
import com.kiteclass.core.module.staff.dto.StaffInvitationResponse;

import java.util.List;
import java.util.UUID;

/**
 * Business operations for staff onboarding via token-based invitations.
 *
 * <p>Mirror of {@link com.kiteclass.core.module.parent.service.ParentInvitationService}
 * adapted for the staff scope (no child linkage; role-bearing User creation
 * delegated to gateway).
 *
 * @since 2026-05-27 (Wave meta-6 Bucket A — GAP-772)
 */
public interface StaffInvitationService {

    /**
     * Issue a new invitation. Owner must be authenticated (verified at
     * gateway + controller layer); the inviter id is captured for audit.
     *
     * @param tenantId   tenant scope (from {@code TenantContext})
     * @param email      staff email — becomes login identifier
     * @param role       role to provision on acceptance (STAFF/TEACHER/MANAGER)
     * @param inviterId  gateway user id of the Owner/Admin issuing the invite
     * @return invitation row including the redemption token (Owner copies link)
     */
    StaffInvitationResponse invite(UUID tenantId, String email, String role, Long inviterId);

    /**
     * List PENDING + ACCEPTED + EXPIRED + REVOKED invitations for the current
     * tenant. Token field omitted on list to reduce leak surface.
     */
    List<StaffInvitationResponse> listForTenant(UUID tenantId);

    /**
     * Owner-side cancel — flips PENDING → REVOKED. No-op on already-resolved
     * (ACCEPTED / EXPIRED / REVOKED) rows.
     */
    void revoke(UUID tenantId, Long invitationId);

    /**
     * Public claim — invitee POST against the token. Marks the invitation
     * ACCEPTED and returns the credential payload for gateway to provision
     * the User row + role binding.
     *
     * @throws com.kiteclass.core.common.exception.BusinessException
     *         INVITATION_NOT_FOUND / INVITATION_EXPIRED / INVITATION_REVOKED /
     *         INVITATION_ALREADY_ACCEPTED
     */
    AcceptStaffInviteResult accept(UUID tenantId, String token, AcceptStaffInviteRequest request);
}
