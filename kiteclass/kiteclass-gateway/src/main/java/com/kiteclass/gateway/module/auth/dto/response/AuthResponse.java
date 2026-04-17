package com.kiteclass.gateway.module.auth.dto.response;

import lombok.Builder;

/**
 * Simplified authentication response for registration and login.
 *
 * <p>Contains JWT tokens and basic user information.
 * Used by both register and login operations.
 *
 * @param userId       User ID from Gateway database
 * @param accessToken  JWT access token for API authentication
 * @param refreshToken JWT refresh token for token renewal
 * @param tokenType    Token type (always "Bearer")
 * @param expiresIn    Access token expiration time in seconds
 * @author KiteClass Team
 * @since 1.1.0
 */
@Builder
public record AuthResponse(
        Long userId,
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {
}
