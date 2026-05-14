package com.kitehub.subscription.auth.twofactor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /api/auth/2fa/verify} (GAP-516).
 *
 * <p>Exactly one of {@code totp_code} OR {@code recovery_code} must be present.
 * The controller returns HTTP 400 {@code INVALID_REQUEST} otherwise.</p>
 */
public record VerifyRequest(
    @JsonProperty("challenge_token") String challengeToken,
    @JsonProperty("totp_code") String totpCode,
    @JsonProperty("recovery_code") String recoveryCode
) { }
