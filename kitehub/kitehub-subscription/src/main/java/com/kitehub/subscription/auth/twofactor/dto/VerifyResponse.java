package com.kitehub.subscription.auth.twofactor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body for {@code POST /api/auth/2fa/verify} (GAP-516).
 *
 * <p>The {@code regenerateRecommended} + {@code codesRemaining} fields are
 * present only when the user verified via recovery code (so the FE can prompt
 * them to regenerate).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerifyResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    UserSummary user,
    @JsonProperty("regenerate_recommended") Boolean regenerateRecommended,
    @JsonProperty("codes_remaining") Long codesRemaining
) {
    public record UserSummary(String id, String email, String role) { }
}
