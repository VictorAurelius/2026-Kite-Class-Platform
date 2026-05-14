package com.kitehub.subscription.auth.twofactor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/2fa/enroll-confirm} (GAP-516).
 *
 * <p>The challenge token used by the controller is read from the
 * {@code Authorization: Bearer} header so it is intentionally absent here.</p>
 */
public record EnrollConfirmRequest(
    @JsonProperty("first_totp_code")
    @NotBlank
    String firstTotpCode
) { }
