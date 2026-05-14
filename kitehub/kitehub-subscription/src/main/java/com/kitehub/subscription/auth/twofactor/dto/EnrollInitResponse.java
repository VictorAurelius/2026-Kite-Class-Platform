package com.kitehub.subscription.auth.twofactor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body for {@code POST /api/auth/2fa/enroll-init} (GAP-516).
 *
 * @param secret           base32 TOTP secret
 * @param qrUri            otpauth URI for QR rendering
 * @param recoveryCodes    ten single-use codes shown ONCE here
 */
public record EnrollInitResponse(
    String secret,
    @JsonProperty("qr_uri") String qrUri,
    @JsonProperty("recovery_codes") List<String> recoveryCodes
) { }
