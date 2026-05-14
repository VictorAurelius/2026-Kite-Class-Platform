package com.kitehub.subscription.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Login response DTO.
 *
 * <p>Wave 72b (GAP-516) extension: when the authenticated user has 2FA enrolled
 * OR is required to enroll, the standard {@code accessToken/refreshToken/user}
 * triple is replaced by {@code requires2fa(_enrollment)} + {@code challenge_token}.
 * Per the auth api-contract, callers MUST check these flags FIRST.</p>
 *
 * @since 1.0.0
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private UserInfo user;
    private String accessToken;
    private String refreshToken;
    private List<InstanceResponse> instances;

    /** When TRUE, FE must redirect to the 2FA challenge page. */
    @JsonProperty("requires2fa")
    private Boolean requires2fa;

    /** When TRUE, FE must redirect to the 2FA enrollment wizard. */
    @JsonProperty("requires2fa_enrollment")
    private Boolean requires2faEnrollment;

    /** Short-lived (5 min) token consumed by the 2FA controller. */
    @JsonProperty("challenge_token")
    private String challengeToken;

    /**
     * User information.
     */
    @Data
    @Builder
    public static class UserInfo {
        private UUID id;
        private String email;
        private String name;
        private String role;
    }
}
