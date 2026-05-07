package com.kitehub.subscription.beta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Coordinator reject command payload.
 *
 * @param approverId coordinator user id (audit field)
 * @param rejectionReason free-text justification surfaced in audit log
 *
 * @since Wave 33 — GAP-372
 */
public record BetaRejectCommand(
        @NotBlank @Size(max = 100) String approverId,
        @NotBlank @Size(max = 1000) String rejectionReason
) {}
