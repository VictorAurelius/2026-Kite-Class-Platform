package com.kiteclass.gateway.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing access token.
 *
 * @param refreshToken JWT refresh token
 * @author KiteClass Team
 * @since 1.0.0
 */
public record RefreshTokenRequest(

        @NotBlank(message = "{validation.refreshToken.required}")
        String refreshToken
) {
}
