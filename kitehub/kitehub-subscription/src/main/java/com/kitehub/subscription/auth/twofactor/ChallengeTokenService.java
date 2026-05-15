package com.kitehub.subscription.auth.twofactor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * Mints + verifies short-lived (5 min) challenge tokens used during the 2FA
 * dance (GAP-516).
 *
 * <p>The challenge token is the only credential a client holds between
 * {@code POST /api/auth/login} (password verified, but 2FA pending) and
 * {@code POST /api/auth/2fa/{verify|enroll-*}}. It is signed with a dedicated
 * HS256 secret ({@code jwt.challenge-secret}) so it is intentionally NOT
 * accepted by the regular access-token verifier.</p>
 *
 * @since 1.0.0 (Wave 72b GAP-516)
 */
@Service
@Slf4j
public class ChallengeTokenService {

    /** Reasons the verifier failed. Mapped to HTTP error codes by the controller. */
    public enum FailureReason { INVALID, EXPIRED }

    /** Action the challenge unlocks. */
    public enum Purpose { TWO_FACTOR_VERIFY, TWO_FACTOR_ENROLL }

    @Getter
    public static final class Verified {
        private final UUID userId;
        private final Purpose purpose;

        private Verified(UUID userId, Purpose purpose) {
            this.userId = userId;
            this.purpose = purpose;
        }
    }

    /** Thrown when the challenge token fails verification. */
    @Getter
    public static class ChallengeTokenException extends RuntimeException {
        private final FailureReason reason;

        public ChallengeTokenException(FailureReason reason, String message) {
            super(message);
            this.reason = reason;
        }
    }

    private static final long TTL_SECONDS = 5 * 60L;

    /**
     * Hard-coded dev fallback documented in this file's javadoc. Production MUST
     * override via {@code JWT_CHALLENGE_SECRET} env — equality to this string
     * triggers fail-fast in production profile (GAP-553).
     */
    static final String DEV_DEFAULT_SECRET = "dev-challenge-secret-pad-pad-pad-pad-pad";

    private final SecretKey key;
    private final boolean productionProfile;
    private final boolean usingDevDefault;
    private final int configuredSecretLength;

    public ChallengeTokenService(
        @Value("${jwt.challenge-secret:dev-challenge-secret-pad-pad-pad-pad-pad}") String secret,
        Environment environment) {
        this.productionProfile = isProduction(environment);
        this.usingDevDefault = DEV_DEFAULT_SECRET.equals(secret);
        this.configuredSecretLength = secret.getBytes(StandardCharsets.UTF_8).length;

        // HS256 requires at least 256 bits (32 bytes); pad short config values
        // (non-prod only; production fail-fast handled in #validate).
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(raw, 0, padded, 0, raw.length);
            raw = padded;
        }
        this.key = Keys.hmacShaKeyFor(raw);
    }

    private static boolean isProduction(Environment environment) {
        if (environment == null) {
            return false;
        }
        String[] active = environment.getActiveProfiles();
        return active != null && Arrays.stream(active)
            .anyMatch(p -> "production".equalsIgnoreCase(p) || "prod".equalsIgnoreCase(p));
    }

    @PostConstruct
    public void validate() {
        // GAP-553 fail-fast: refuse to boot in production if secret matches the
        // hard-coded dev default OR if config supplied < 32 bytes of entropy.
        if (productionProfile && (usingDevDefault || configuredSecretLength < 32)) {
            throw new IllegalStateException(
                "JWT challenge secret MUST be set via jwt.challenge-secret (JWT_CHALLENGE_SECRET) "
                + "(>=32 bytes, not the dev default) in production profile. "
                + "Got length=" + configuredSecretLength + ", isDevDefault=" + usingDevDefault);
        }

        String tok = issue(UUID.randomUUID(), Purpose.TWO_FACTOR_VERIFY);
        verify(tok); // throws if smoke-test fails
        log.info("ChallengeTokenService initialised — sign+verify round-trip OK (production={}, devDefault={})",
            productionProfile, usingDevDefault);
    }

    public String issue(UUID userId, Purpose purpose) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "challenge")
            .claim("purpose", purpose.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(TTL_SECONDS, ChronoUnit.SECONDS)))
            .signWith(key)
            .compact();
    }

    public Verified verify(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            if (!"challenge".equals(claims.get("type", String.class))) {
                throw new ChallengeTokenException(FailureReason.INVALID, "Wrong token type");
            }
            UUID userId = UUID.fromString(claims.getSubject());
            Purpose purpose = Purpose.valueOf(claims.get("purpose", String.class));
            return new Verified(userId, purpose);
        } catch (ExpiredJwtException ex) {
            throw new ChallengeTokenException(FailureReason.EXPIRED, "Challenge token expired");
        } catch (ChallengeTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ChallengeTokenException(FailureReason.INVALID, "Challenge token invalid");
        }
    }
}
