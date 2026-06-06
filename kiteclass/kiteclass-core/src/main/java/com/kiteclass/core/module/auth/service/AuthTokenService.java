package com.kiteclass.core.module.auth.service;

import com.kiteclass.core.module.auth.entity.AuthCredential;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Mints KC-native access tokens (Wave auth-1, Option B).
 *
 * <p>Signs with the SHARED {@code JWT_SECRET} (HS512 — same key + algorithm the
 * gateway's {@code JwtAuthenticationGatewayFilter} validates, GAP-705). The token
 * carries the three identity claims the gateway forwards downstream:
 * <ul>
 *   <li>{@code sub} → X-User-Id (audit UUID)</li>
 *   <li>{@code role} → X-User-Roles</li>
 *   <li>{@code email} → X-User-Email</li>
 *   <li>{@code tenantId} → resolved to X-Tenant-Id (gateway TenantResolver, GAP-711)</li>
 *   <li>{@code referenceId} → X-User-Reference-Id (gateway injection added Wave auth-1 Bucket C)</li>
 * </ul>
 */
@Slf4j
@Service
public class AuthTokenService {

    private final String jwtSecret;
    private final Duration accessTtl;
    private SecretKey signingKey;

    public AuthTokenService(
            @Value("${jwt.secret:${JWT_SECRET:}}") String jwtSecret,
            @Value("${kite.auth.access-token-ttl:PT12H}") Duration accessTtl) {
        this.jwtSecret = jwtSecret;
        this.accessTtl = accessTtl;
    }

    @PostConstruct
    void init() {
        if (jwtSecret == null || jwtSecret.getBytes().length < 64) {
            throw new IllegalStateException(
                    "JWT_SECRET must be >= 64 bytes for HS512 (KC-native login). Current length: "
                            + (jwtSecret == null ? 0 : jwtSecret.getBytes().length)
                            + ". Set JWT_SECRET env (same value as kite-gateway).");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        log.info("AuthTokenService initialised — HS512 access tokens, TTL {}", accessTtl);
    }

    /**
     * Mint an access token for an authenticated credential.
     *
     * @param credential the verified credential (caller MUST have checked the password)
     * @return signed compact JWS
     */
    public String mintAccessToken(AuthCredential credential) {
        Instant now = Instant.now();
        return Jwts.builder()
                // GAP-1013e: jti = unique token id, enables future revocation/blacklist.
                .id(UUID.randomUUID().toString())
                .subject(credential.getUserUuid().toString())
                .claim("role", credential.getEntityType())
                .claim("email", credential.getEmail())
                .claim("tenantId", credential.getInstanceId().toString())
                .claim("referenceId", credential.getEntityId())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }
}
