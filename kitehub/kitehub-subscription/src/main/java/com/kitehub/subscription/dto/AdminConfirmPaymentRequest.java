package com.kitehub.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for admin payment confirm endpoint (Wave flow-kh3).
 *
 * <p>Used by {@code POST /api/platform/admin/payments/{id}/confirm} — admin enters the
 * bank transaction id after off-line reconciliation (per UC-SUB-07).</p>
 *
 * <p>Mirrors the api-contract shape in
 * {@code documents/01-business/kitehub/subscription-billing/api-contract.md} §POST confirm.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminConfirmPaymentRequest {

    /** Bank transaction id captured from the statement (e.g. {@code VCB-20260604-000123}). */
    @NotBlank(message = "transactionId is required")
    private String transactionId;
}
