package com.kitehub.subscription.auth.twofactor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/** Response body for {@code POST /api/auth/2fa/disable}. */
public record DisableResponse(
    boolean disabled,
    @JsonProperty("disabled_at") LocalDateTime disabledAt
) { }
