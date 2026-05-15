package com.kitehub.subscription.impersonation.dto;

import com.kitehub.subscription.impersonation.ImpersonationAuditEntry;
import com.kitehub.subscription.impersonation.ImpersonationAuditEntry.EndedReason;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only projection of {@link ImpersonationAuditEntry} for the
 * {@code GET /api/v1/admin/impersonate/audit-log} endpoint.
 *
 * @since Wave 79 (GAP-040)
 */
public record ImpersonationAuditEntryDto(
        Long id,
        UUID adminUserId,
        UUID tenantId,
        String tenantSlug,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        EndedReason endedReason,
        String requestIp,
        String userAgent
) {
    public static ImpersonationAuditEntryDto from(ImpersonationAuditEntry e) {
        return new ImpersonationAuditEntryDto(
                e.getId(),
                e.getAdminUserId(),
                e.getTenantId(),
                e.getTenantSlug(),
                e.getStartedAt(),
                e.getEndedAt(),
                e.getEndedReason(),
                e.getRequestIp(),
                e.getUserAgent()
        );
    }
}
