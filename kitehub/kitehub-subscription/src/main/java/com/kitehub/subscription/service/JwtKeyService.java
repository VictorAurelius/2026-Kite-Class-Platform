package com.kitehub.subscription.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Central JWT signing + verifying key resolver (GAP-520 — dual-key rotation).
 *
 * <p>Holds the CURRENT signing key (always used for new tokens) and an optional
 * PREVIOUS verifying key (honored during a rotation window so refresh tokens
 * issued before the rotation still validate). Once the refresh-token TTL window
 * has elapsed since rotation, the previous key can be safely dropped.</p>
 *
 * <p><b>Quarterly rotation runbook:</b> see
 * {@code documents/05-guides/operations/jwt-rotation-runbook.md}.</p>
 *
 * <p><b>Config:</b>
 * <ul>
 *   <li>{@code jwt.secret} — required, ≥32 chars (256 bits). Backed by env
 *       {@code JWT_SECRET_CURRENT} (preferred) or legacy {@code JWT_SECRET}.</li>
 *   <li>{@code jwt.previous-secret} — optional, empty means "no fallback".
 *       Backed by env {@code JWT_SECRET_PREVIOUS}.</li>
 * </ul>
 *
 * @since 1.0.0 (Wave 72a GAP-520)
 */
@Service
@Slf4j
public class JwtKeyService {

    private final SecretKey currentKey;
    @Nullable
    private final SecretKey previousKey;
    @Nullable
    private final Counter fallbackCounter;

    public JwtKeyService(
        @Value("${jwt.secret:}") String currentSecret,
        @Value("${jwt.previous-secret:}") String previousSecret,
        @Autowired(required = false) MeterRegistry meterRegistry
    ) {
        if (currentSecret == null || currentSecret.isBlank()) {
            throw new IllegalStateException(
                "JWT signing secret is not configured! Set JWT_SECRET (or JWT_SECRET_CURRENT). " +
                "Generate with: openssl rand -base64 64");
        }
        if (currentSecret.length() < 32) {
            throw new IllegalStateException(
                "jwt.secret must be at least 32 characters (256 bits)");
        }
        this.currentKey = Keys.hmacShaKeyFor(currentSecret.getBytes(StandardCharsets.UTF_8));

        if (previousSecret != null && !previousSecret.isBlank()) {
            if (previousSecret.length() < 32) {
                throw new IllegalStateException(
                    "jwt.previous-secret must be at least 32 characters when set");
            }
            this.previousKey = Keys.hmacShaKeyFor(previousSecret.getBytes(StandardCharsets.UTF_8));
            log.info("JWT dual-key mode ACTIVE — current + previous (rotation window)");
        } else {
            this.previousKey = null;
            log.info("JWT single-key mode — no previous secret configured");
        }

        this.fallbackCounter = meterRegistry != null
            ? Counter.builder("jwt.verify.fallback")
                .description("JWT tokens verified against the previous signing key (rotation fallback)")
                .register(meterRegistry)
            : null;
    }

    @PostConstruct
    void logBoot() {
        log.info("JwtKeyService ready (rotation fallback: {})", previousKey != null ? "enabled" : "disabled");
    }

    /** Always-current signing key for new tokens. */
    public SecretKey signingKey() {
        return currentKey;
    }

    /**
     * Verify a JWS token, trying the current key first and the previous key
     * second (only if configured). Emits {@code jwt.verify.fallback} counter
     * when verification succeeds on the previous key.
     *
     * @param token compact JWS string
     * @return parsed claims
     * @throws JwtException if neither key validates the signature, or if the
     *                      token is malformed / expired
     */
    public Jws<Claims> parse(String token) {
        try {
            return Jwts.parser().verifyWith(currentKey).build().parseSignedClaims(token);
        } catch (JwtException primary) {
            if (previousKey == null) {
                throw primary;
            }
            try {
                Jws<Claims> parsed = Jwts.parser().verifyWith(previousKey).build().parseSignedClaims(token);
                if (fallbackCounter != null) {
                    fallbackCounter.increment();
                }
                log.warn("JWT verified against PREVIOUS key (rotation fallback). " +
                         "Caller must re-issue from current key on next refresh.");
                return parsed;
            } catch (JwtException secondary) {
                // Surface the ORIGINAL (current-key) error so callers see consistent
                // behavior when both keys reject — typically expiry / malformed.
                throw primary;
            }
        }
    }
}
