package com.kiteclass.core.module.childprotection.dto;

import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for {@code PATCH /api/v1/vettings/{id}/transition}.
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
public record VettingTransitionRequest(
        @NotNull VettingStatus targetStatus
) {
}
