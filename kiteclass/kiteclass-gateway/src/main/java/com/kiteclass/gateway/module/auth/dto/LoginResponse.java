package com.kiteclass.gateway.module.auth.dto;

import lombok.Builder;

import java.util.List;

/**
 * Response DTO for successful login.
 *
 * @param accessToken JWT access token
 * @param refreshToken JWT refresh token
 * @param tokenType token type (always "Bearer")
 * @param expiresIn access token expiration time in seconds
 * @param user authenticated user information
 * @author KiteClass Team
 * @since 1.0.0
 */
@Builder
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserInfo user
) {

    /**
     * Authenticated user information.
     *
     * @param id user ID
     * @param email user email
     * @param name user display name
     * @param roles list of role codes
     */
    @Builder
    public record UserInfo(
            Long id,
            String email,
            String name,
            List<String> roles
    ) {
    }
}
