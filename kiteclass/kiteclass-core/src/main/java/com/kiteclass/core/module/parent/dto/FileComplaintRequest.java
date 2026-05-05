package com.kiteclass.core.module.parent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/parent/complaints} (Wave 19 —
 * GAP-321c Phase 1C v1).
 *
 * <p>Free-text complaint scoped by {@code studentId}. v1 doesn't accept
 * attachments — those land with full workflow in GAP-339.
 *
 * @since 2.19.0
 */
public record FileComplaintRequest(
        @NotNull Long studentId,
        @NotBlank @Size(min = 10, max = 2000) String complaintText) {
}
