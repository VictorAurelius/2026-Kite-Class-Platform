package com.kiteclass.core.module.auth.dto;

/**
 * KC-native login response (Wave auth-1). {@code accessToken} is an HS512 JWT the
 * gateway validates + forwards as identity headers.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String role,
        Long referenceId,
        String tenantId
) {
}
