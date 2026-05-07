package com.kitehub.subscription.beta.dto;

import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;

import java.time.OffsetDateTime;

/**
 * Public-safe view of a beta access request — used by:
 *
 * <ul>
 *   <li>Public submit endpoint return payload (acknowledgement only)</li>
 *   <li>Coordinator dashboard listing</li>
 *   <li>Token validation pre-fill response (only email + name + persona surfaced)</li>
 * </ul>
 *
 * <p>The {@code inviteToken} is intentionally NOT exposed in any GET-by-id
 * response — it travels via email only. The {@code rejectionReason} is exposed
 * to coordinator listings and to the requester via {@code GET /admin/...} only.</p>
 *
 * @since Wave 33 — GAP-372
 */
public record BetaRequestResponse(
        Long id,
        String email,
        String name,
        String orgName,
        String persona,
        String referralSource,
        BetaAccessRequestStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime approvedAt,
        OffsetDateTime rejectedAt,
        String rejectionReason
) {
    public static BetaRequestResponse from(BetaAccessRequest entity) {
        return new BetaRequestResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getName(),
                entity.getOrgName(),
                entity.getPersona(),
                entity.getReferralSource(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getApprovedAt(),
                entity.getRejectedAt(),
                entity.getRejectionReason()
        );
    }
}
