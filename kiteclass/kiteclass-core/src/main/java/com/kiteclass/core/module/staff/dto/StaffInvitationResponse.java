package com.kiteclass.core.module.staff.dto;

import com.kiteclass.core.common.constant.StaffInvitationStatus;

import java.time.Instant;

/**
 * Public-facing view of a {@link com.kiteclass.core.module.staff.entity.StaffInvitation}.
 *
 * <p>The {@code token} field is included only for the Owner-issuing response
 * (so the FE can render the redemption link / copy-to-clipboard). The
 * Owner-list endpoint omits the token to avoid surface area for leaks via
 * audit log scrapes; that variant is filtered server-side.
 *
 * @since 2026-05-27 (Wave meta-6 Bucket A — GAP-772)
 */
public record StaffInvitationResponse(
        Long id,
        String email,
        String role,
        String token,
        StaffInvitationStatus status,
        Instant expiresAt,
        Long invitedByUserId,
        Instant acceptedAt,
        Long acceptedUserId,
        Instant createdAt
) {}
