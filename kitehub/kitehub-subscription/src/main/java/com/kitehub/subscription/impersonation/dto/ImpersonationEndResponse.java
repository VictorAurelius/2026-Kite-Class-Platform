package com.kitehub.subscription.impersonation.dto;

import com.kitehub.subscription.impersonation.ImpersonationAuditEntry.EndedReason;

import java.time.OffsetDateTime;

/**
 * Response payload for {@code POST /api/v1/admin/impersonate/end}.
 *
 * @since Wave 79 (GAP-040)
 */
public record ImpersonationEndResponse(
        Long sessionId,
        OffsetDateTime endedAt,
        EndedReason endedReason
) {}
