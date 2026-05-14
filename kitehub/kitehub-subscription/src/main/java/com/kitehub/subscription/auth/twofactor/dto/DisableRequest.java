package com.kitehub.subscription.auth.twofactor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/2fa/disable}.
 *
 * <p>Both the TOTP code and the password must reconfirm — possession + knowledge
 * (BR-AUTH-005).</p>
 */
public record DisableRequest(
    @JsonProperty("current_totp_code")
    @NotBlank
    String currentTotpCode,

    @JsonProperty("password_reconfirm")
    @NotBlank
    String passwordReconfirm
) { }
