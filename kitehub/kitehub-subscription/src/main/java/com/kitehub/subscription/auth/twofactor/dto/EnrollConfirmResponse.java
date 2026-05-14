package com.kitehub.subscription.auth.twofactor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Response body for {@code POST /api/auth/2fa/enroll-confirm} (GAP-516).
 */
public record EnrollConfirmResponse(
    boolean enrolled,
    @JsonProperty("totp_enrolled_at") LocalDateTime totpEnrolledAt,
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    UserSummary user
) {
    public record UserSummary(
        String id,
        String email,
        String role,
        @JsonProperty("totp_enrolled_at") LocalDateTime totpEnrolledAt
    ) { }
}
