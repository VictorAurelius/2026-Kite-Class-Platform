package com.kitehub.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for admin payment reject endpoint (Wave flow-kh3).
 *
 * <p>Used by {@code POST /api/platform/admin/payments/{id}/reject} — admin records the
 * rejection reason after off-line reconciliation finds the payment invalid
 * (per UC-SUB-07).</p>
 *
 * <p>Mirrors the api-contract shape in
 * {@code documents/01-business/kitehub/subscription-billing/api-contract.md} §POST reject.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRejectPaymentRequest {

    /** Human-readable rejection reason (shown in admin audit log + future tenant notification). */
    @NotBlank(message = "reason is required")
    private String reason;
}
