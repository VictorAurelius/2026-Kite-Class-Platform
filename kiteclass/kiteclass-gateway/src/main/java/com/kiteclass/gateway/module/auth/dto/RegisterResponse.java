package com.kiteclass.gateway.module.auth.dto;

import lombok.Builder;

/**
 * Response DTO for student registration.
 *
 * <p>Returned after successful registration with JWT tokens for immediate login.
 *
 * @param accessToken  JWT access token for API authentication
 * @param refreshToken JWT refresh token for token renewal
 * @param tokenType    Token type (always "Bearer")
 * @param expiresIn    Access token expiration time in seconds
 * @param user         User information including profile
 * @author KiteClass Team
 * @since 1.8.0
 */
@Builder
public record RegisterResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserInfo user
) {
    /**
     * User information embedded in registration response.
     *
     * @param id          User ID from Gateway
     * @param email       User's email
     * @param name        User's name
     * @param userType    User type (STUDENT)
     * @param referenceId Student ID from Core service
     */
    @Builder
    public record UserInfo(
            Long id,
            String email,
            String name,
            String userType,
            Long referenceId
    ) {
    }
}
