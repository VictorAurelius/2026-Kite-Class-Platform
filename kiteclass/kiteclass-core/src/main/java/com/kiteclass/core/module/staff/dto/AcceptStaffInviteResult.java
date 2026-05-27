package com.kiteclass.core.module.staff.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Internal Saga result returned to the gateway after staff invitation
 * acceptance — gateway uses this payload to provision the User row with
 * role binding + issue JWT.
 *
 * <p>Mirror of {@link com.kiteclass.core.module.parent.dto.RedeemInvitationResult}
 * adapted for staff (no student linkage; role-bearing). Password handling is
 * not in scope of kiteclass-core — gateway receives the plaintext password
 * from the FE, hashes it, and stores it on the new User row.
 *
 * @since 2026-05-27 (Wave meta-6 Bucket A — GAP-772)
 */
public record AcceptStaffInviteResult(
        Long invitationId,
        UUID tenantId,
        String email,
        String fullName,
        String role,
        Instant acceptedAt
) {}
