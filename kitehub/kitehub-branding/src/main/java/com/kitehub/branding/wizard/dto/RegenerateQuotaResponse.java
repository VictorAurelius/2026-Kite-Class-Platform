package com.kitehub.branding.wizard.dto;

import java.time.Instant;

/**
 * Response for GET /api/v1/branding/regenerate-quota (Wave 34 sub-GAP-272d).
 *
 * <p>{@code limit = -1} signals unlimited (ENTERPRISE); {@code resetAt} is null
 * in that case per api-contract.md.
 */
public record RegenerateQuotaResponse(
        String tier,
        int used,
        int limit,
        Instant resetAt) {}
