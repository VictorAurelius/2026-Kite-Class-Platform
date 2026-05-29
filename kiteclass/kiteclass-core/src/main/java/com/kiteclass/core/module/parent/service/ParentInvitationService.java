package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.module.parent.dto.ParentInvitationResponse;
import com.kiteclass.core.module.parent.dto.RedeemInvitationRequest;
import com.kiteclass.core.module.parent.dto.RedeemInvitationResult;

import java.util.UUID;

/**
 * Lifecycle operations on {@link com.kiteclass.core.module.parent.entity.ParentInvitation}.
 *
 * <p>Each method documents the invariants it enforces. See
 * {@code documents/01-business/kiteclass/parent/rules.md} for the full set of
 * business rules.
 *
 * @since 2.14.0
 */
public interface ParentInvitationService {

    /**
     * Issues a new invitation and publishes the email event. Idempotency is
     * enforced at the email-queue layer (dedup per day per recipient) — this
     * method itself permits repeat invites as long as the parent account is
     * not yet created.
     *
     * <p>Invariants:
     * <ul>
     *   <li>BR-PARENT-INV-001: student must exist in the caller's tenant.</li>
     *   <li>BR-PARENT-INV-002: parent email must not already be a fully-
     *       redeemed Parent in this tenant.</li>
     *   <li>BR-PARENT-INV-003: token is a {@link UUID#randomUUID()} (128-bit
     *       entropy).</li>
     *   <li>BR-PARENT-INV-004: {@code expiresAt = now + ttl} (ttl from
     *       {@code kiteclass.parent-portal.invitation-ttl-hours}).</li>
     * </ul>
     *
     * @param tenantId       current tenant (from {@code TenantContext})
     * @param studentId      child to link upon redemption
     * @param parentEmail    recipient email
     * @param invitedByUserId gateway user id (UUID, X-User-Id / JWT sub) of the inviter (admin/teacher) — GAP-795
     * @return DTO with the generated token so the caller may surface it in
     *         test tooling; the production UI should treat it as opaque.
     */
    ParentInvitationResponse invite(
            UUID tenantId,
            Long studentId,
            String parentEmail,
            UUID invitedByUserId
    );

    /**
     * Redeems the invitation, creating (or re-using) the Parent row and the
     * ParentStudentLink edge, then returns the data the Gateway needs to
     * provision the matching User and mint tokens.
     *
     * <p>Invariants:
     * <ul>
     *   <li>BR-PARENT-INV-005: token must be PENDING and un-expired.</li>
     *   <li>BR-PARENT-INV-006: exactly one {@link
     *       com.kiteclass.core.module.parent.entity.Parent} per (tenant, email).
     *       If the row exists from an earlier redemption of a sibling's
     *       invitation, we link to it and skip creation.</li>
     *   <li>BR-PARENT-INV-007: invitation transitions to REDEEMED atomically
     *       with Parent + link creation.</li>
     * </ul>
     *
     * @param tenantId current tenant
     * @param token    opaque token from the invitation email
     * @param request  password + profile submitted by the parent
     */
    RedeemInvitationResult redeem(UUID tenantId, String token, RedeemInvitationRequest request);

    /**
     * Sweeper called by the scheduled job — transitions any PENDING invitation
     * whose {@code expiresAt} is in the past to EXPIRED. Returns the number of
     * rows transitioned for observability.
     */
    int expireStale();
}
