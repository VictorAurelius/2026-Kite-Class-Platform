package com.kitehub.subscription.beta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Coordinator approve command payload.
 *
 * @param approverId coordinator user id (audit field)
 *
 * @since Wave 33 — GAP-372
 */
public record BetaApproveCommand(
        @NotBlank @Size(max = 100) String approverId
) {}
