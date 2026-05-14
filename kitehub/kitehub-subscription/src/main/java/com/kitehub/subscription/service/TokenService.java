package com.kitehub.subscription.service;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Service for generating JWT access + refresh tokens.
 *
 * <p>Since Wave 72a GAP-520, signing delegates to {@link JwtKeyService} so the
 * dual-key rotation policy applies uniformly across every issuer in the
 * subscription service.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class TokenService {

    private final JwtKeyService jwtKeyService;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public TokenService(
        JwtKeyService jwtKeyService,
        @Value("${jwt.access-token-expiration:86400000}") long accessTokenExpiration,
        @Value("${jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration
    ) {
        this.jwtKeyService = jwtKeyService;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Generate access token for user.
     *
     * @param userId user UUID
     * @param email user email
     * @param role user role
     * @return JWT access token
     */
    public String generateAccessToken(UUID userId, String email, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
            .subject(userId.toString())
            .claims(Map.of(
                "email", email,
                "role", role,
                "type", "access"
            ))
            .issuedAt(now)
            .expiration(expiration)
            .signWith(jwtKeyService.signingKey())
            .compact();
    }

    /**
     * Generate refresh token for user.
     *
     * @param userId user UUID
     * @return JWT refresh token
     */
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
            .subject(userId.toString())
            .claims(Map.of("type", "refresh"))
            .issuedAt(now)
            .expiration(expiration)
            .signWith(jwtKeyService.signingKey())
            .compact();
    }
}
