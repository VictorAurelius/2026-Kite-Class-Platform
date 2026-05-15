package com.kitehub.subscription.impersonation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response payload for {@code POST /api/v1/admin/impersonate/{tenantSlug}}.
 *
 * @param sessionId         audit-log row id, used by the FE to call /end
 * @param impersonationToken short-lived JWT (30s TTL) bearing tenant claim + impersonated_by claim
 * @param tenantId          UUID of the tenant being viewed
 * @param tenantSlug        slug of the tenant being viewed (echo)
 * @param expiresAt         absolute wall-clock instant when the token + session expire
 *
 * @since Wave 79 (GAP-040)
 */
public record ImpersonationStartResponse(
        Long sessionId,
        String impersonationToken,
        UUID tenantId,
        String tenantSlug,
        OffsetDateTime expiresAt
) {}
