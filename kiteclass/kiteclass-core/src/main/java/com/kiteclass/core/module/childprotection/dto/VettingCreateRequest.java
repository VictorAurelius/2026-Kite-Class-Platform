package com.kiteclass.core.module.childprotection.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request payload for {@code POST /api/v1/vettings}.
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
public record VettingCreateRequest(
        @NotNull Long teacherId,
        String lltpNumber,
        String policeCheckDetails,
        Instant expiresAt
) {
}
