package com.kitehub.subscription.auth.otp;

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

/**
 * Mints + verifies short-lived (10 min) signup tokens issued once a phone number
 * has been proven via OTP (GAP-286 — mobile signup).
 *
 * <p>The signup token is the only credential the client holds between
 * {@code POST /api/v1/auth/signup/verify-otp} (phone ownership proven) and the
 * later tenant-create step (out of scope here). It asserts the single claim
 * "this phone is verified"; it is signed with a dedicated HS256 secret
 * ({@code jwt.signup-secret}) so it is intentionally NOT accepted by the regular
 * access-token verifier nor the 2FA challenge verifier.</p>
 *
 * <p>Mirrors {@link com.kitehub.subscription.auth.twofactor.ChallengeTokenService}
 * (issue + verify + production fail-fast smoke test).</p>
 *
 * @since GAP-286 (mobile signup OTP)
 */
@Service
@Slf4j
public class SignupTokenService {

    /** Reasons the verifier failed. Mapped to HTTP error codes by callers. */
    public enum FailureReason { INVALID, EXPIRED }

    @Getter
    public static final class Verified {
        private final String phone;

        private Verified(String phone) {
            this.phone = phone;
        }
    }

    /** Thrown when the signup token fails verification. */
    @Getter
    public static class SignupTokenException extends RuntimeException {
        private final FailureReason reason;

        public SignupTokenException(FailureReason reason, String message) {
            super(message);
            this.reason = reason;
        }
    }

    private static final long TTL_SECONDS = 10 * 60L;

    /**
     * Hard-coded dev fallback documented in this file's javadoc. Production MUST
     * override via {@code JWT_SIGNUP_SECRET} env — equality to this string triggers
     * fail-fast in production profile (mirrors GAP-553 challenge-secret guard).
     */
    static final String DEV_DEFAULT_SECRET = "dev-signup-secret-pad-pad-pad-pad-pad-pad";

    private final SecretKey key;
    private final boolean productionProfile;
    private final boolean usingDevDefault;
    private final int configuredSecretLength;

    public SignupTokenService(
        @Value("${jwt.signup-secret:dev-signup-secret-pad-pad-pad-pad-pad-pad}") String secret,
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
        // Fail-fast: refuse to boot in production if secret matches the hard-coded
        // dev default OR if config supplied < 32 bytes of entropy.
        if (productionProfile && (usingDevDefault || configuredSecretLength < 32)) {
            throw new IllegalStateException(
                "JWT signup secret MUST be set via jwt.signup-secret (JWT_SIGNUP_SECRET) "
                + "(>=32 bytes, not the dev default) in production profile. "
                + "Got length=" + configuredSecretLength + ", isDevDefault=" + usingDevDefault);
        }

        String tok = issue("0000000000");
        verify(tok); // throws if smoke-test fails
        log.info("SignupTokenService initialised — sign+verify round-trip OK (production={}, devDefault={})",
            productionProfile, usingDevDefault);
    }

    /**
     * Issue a short-lived token asserting the given phone has been verified.
     *
     * @param phone the phone number proven via OTP
     * @return a signed HS256 JWT (10-min TTL)
     */
    public String issue(String phone) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(phone)
            .claim("type", "signup")
            .claim("purpose", "MOBILE_SIGNUP")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(TTL_SECONDS, ChronoUnit.SECONDS)))
            .signWith(key)
            .compact();
    }

    /**
     * Verify a previously-issued signup token.
     *
     * @throws SignupTokenException if the token is invalid, expired, or wrong type
     */
    public Verified verify(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            if (!"signup".equals(claims.get("type", String.class))) {
                throw new SignupTokenException(FailureReason.INVALID, "Wrong token type");
            }
            return new Verified(claims.getSubject());
        } catch (ExpiredJwtException ex) {
            throw new SignupTokenException(FailureReason.EXPIRED, "Signup token expired");
        } catch (SignupTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SignupTokenException(FailureReason.INVALID, "Signup token invalid");
        }
    }
}
