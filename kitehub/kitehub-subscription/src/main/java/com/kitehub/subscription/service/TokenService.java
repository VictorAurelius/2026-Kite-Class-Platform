package com.kitehub.subscription.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Service for generating and validating JWT tokens.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class TokenService {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public TokenService(
        @Value("${jwt.secret:#{null}}") String secret,
        @Value("${jwt.access-token-expiration:86400000}") long accessTokenExpiration,
        @Value("${jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET is not configured! Set jwt.secret property or JWT_SECRET env var. " +
                "Generate with: openssl rand -base64 64");
        }
        // Ensure secret is at least 256 bits (32 bytes)
        String paddedSecret = secret.length() >= 32 ? secret : secret + "0".repeat(32 - secret.length());
        this.secretKey = Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
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
            .signWith(secretKey)
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
            .signWith(secretKey)
            .compact();
    }
}
