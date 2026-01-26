package com.kiteclass.gateway.security.jwt;

/**
 * JWT token types.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
public enum TokenType {
    /**
     * Access token for API authentication.
     * Short-lived (default 1 hour).
     */
    ACCESS,

    /**
     * Refresh token for obtaining new access tokens.
     * Long-lived (default 7 days).
     */
    REFRESH
}
