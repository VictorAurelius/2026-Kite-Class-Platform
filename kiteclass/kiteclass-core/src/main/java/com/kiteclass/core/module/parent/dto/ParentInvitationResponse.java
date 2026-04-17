package com.kiteclass.core.module.parent.dto;

import java.time.Instant;

/**
 * Admin-facing response describing a parent invitation.
 *
 * @param id          invitation id
 * @param email       recipient email
 * @param studentId   child id
 * @param studentName child display name (denormalised for admin UI)
 * @param status      PENDING / REDEEMED / EXPIRED / REVOKED
 * @param expiresAt   absolute expiry
 * @param token       opaque redemption token — only returned on create; omitted in list responses
 * @since 2.14.0
 */
public record ParentInvitationResponse(
        Long id,
        String email,
        Long studentId,
        String studentName,
        String status,
        Instant expiresAt,
        String token
) {
}
