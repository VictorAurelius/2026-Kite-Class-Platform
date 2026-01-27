package com.kiteclass.gateway.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT configuration properties.
 *
 * <p>Maps properties from {@code application.yml} under the {@code jwt} prefix.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Secret key for signing JWT tokens.
     * Must be at least 512 bits (64 characters) for HS512 algorithm.
     */
    private String secret;

    /**
     * Access token expiration time in milliseconds.
     * Default: 3600000 (1 hour).
     */
    private long accessTokenExpiration;

    /**
     * Refresh token expiration time in milliseconds.
     * Default: 604800000 (7 days).
     */
    private long refreshTokenExpiration;
}
