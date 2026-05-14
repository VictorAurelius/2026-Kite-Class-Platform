package com.kitehub.subscription.auth.twofactor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/2fa/recovery-codes/regenerate}.
 */
public record RegenerateRequest(
    @JsonProperty("current_totp_code")
    @NotBlank
    String currentTotpCode
) { }
