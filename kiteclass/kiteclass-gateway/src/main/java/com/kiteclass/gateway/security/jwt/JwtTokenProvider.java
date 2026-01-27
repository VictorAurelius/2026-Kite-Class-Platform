package com.kiteclass.gateway.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT token provider for generating and validating tokens.
 *
 * <p>Handles:
 * <ul>
 *   <li>Access token generation (short-lived)</li>
 *   <li>Refresh token generation (long-lived)</li>
 *   <li>Token validation and parsing</li>
 *   <li>Claims extraction</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    /**
     * Generate access token for authenticated user.
     *
     * @param userId user ID
     * @param email user email
     * @param roles list of role codes
     * @return JWT access token
     */
    public String generateAccessToken(Long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .claim("type", TokenType.ACCESS.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * Generate refresh token for token renewal.
     *
     * @param userId user ID
     * @return JWT refresh token
     */
    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", TokenType.REFRESH.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * Validate token and return claims.
     *
     * @param token JWT token
     * @return parsed claims
     * @throws ExpiredJwtException if token is expired
     * @throws JwtException if token is invalid
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract user ID from token.
     *
     * @param token JWT token
     * @return user ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extract email from token.
     *
     * @param token JWT token
     * @return user email
     */
    public String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Extract roles from token.
     *
     * @param token JWT token
     * @return list of role codes
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = validateToken(token);
        return (List<String>) claims.get("roles");
    }

    /**
     * Check if token is access token.
     *
     * @param token JWT token
     * @return true if access token, false otherwise
     */
    public boolean isAccessToken(String token) {
        Claims claims = validateToken(token);
        String type = claims.get("type", String.class);
        return TokenType.ACCESS.name().equals(type);
    }

    /**
     * Check if token is refresh token.
     *
     * @param token JWT token
     * @return true if refresh token, false otherwise
     */
    public boolean isRefreshToken(String token) {
        Claims claims = validateToken(token);
        String type = claims.get("type", String.class);
        return TokenType.REFRESH.name().equals(type);
    }

    /**
     * Get secret key for JWT signing.
     *
     * @return SecretKey instance
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }
}
