package com.kitehub.subscription.auth.twofactor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body for {@code POST /api/auth/2fa/recovery-codes/regenerate}.
 */
public record RegenerateResponse(
    @JsonProperty("new_recovery_codes") List<String> newRecoveryCodes,
    @JsonProperty("previous_codes_invalidated") int previousCodesInvalidated,
    String message
) { }
