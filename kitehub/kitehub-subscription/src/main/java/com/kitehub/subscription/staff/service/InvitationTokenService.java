package com.kitehub.subscription.staff.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues + verifies HMAC-signed invitation tokens for the Owner→Staff
 * invitation flow (Wave 80 Bucket B, GAP-561b).
 *
 * <p>Token shape:
 * {@code base64url(invitationId.UUID || ':' || expiresAtEpochSeconds || ':' || nonceB64)}.HMAC_SHA256
 * — three colon-separated fields in the payload half, then a base64url HMAC
 * signature. Single string with one {@code .} separating payload and signature
 * (mirrors JWT-style without the JSON header overhead — tenant invitation
 * doesn't need algorithm agility).</p>
 *
 * <p>Security model per
 * {@code .claude/rules/pre-launch-auth-hardening-checklist.md} §2.6:</p>
 * <ul>
 *   <li>HMAC-SHA256 with secret key ≥32 bytes (production fail-fast)</li>
 *   <li>TTL 7 days enforced at verification</li>
 *   <li>Constant-time signature comparison (timing-attack resistant)</li>
 *   <li>Raw token never persisted — only SHA-256 hash via
 *       {@link com.kitehub.subscription.staff.service.StaffInvitationService#hashToken}</li>
 * </ul>
 *
 * <p>Cipher pattern reuses {@code TotpSecretCipher} fail-fast structure
 * (per {@code .claude/rules/pre-launch-auth-hardening-checklist.md} §2.6 +
 * GAP-553 dev-default guard).</p>
 *
 * @since Wave 80 — GAP-561b
 */
@Component
@Slf4j
public class InvitationTokenService {

    /** Default invitation TTL aligned với BR-ROLE-INVITE-TTL. */
    public static final int TOKEN_TTL_DAYS = 7;

    /** Hard-coded dev fallback. Production MUST override; equality triggers fail-fast. */
    static final String DEV_DEFAULT_SECRET = "dev-invitation-secret-32-bytes-pad-pad";

    private static final String HMAC_ALG = "HmacSHA256";
    private static final int NONCE_LEN = 12;

    private final byte[] secretBytes;
    private final SecureRandom rng = new SecureRandom();
    private final boolean productionProfile;
    private final boolean usingDevDefault;

    public InvitationTokenService(
        @Value("${kitehub.staff.invitation.signing-secret:dev-invitation-secret-32-bytes-pad-pad}") String configuredSecret,
        Environment environment) {
        this.productionProfile = isProduction(environment);
        this.usingDevDefault = DEV_DEFAULT_SECRET.equals(configuredSecret);
        this.secretBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
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
        // Fail-fast in production if secret matches dev default OR < 32 bytes.
        if (productionProfile && (usingDevDefault || secretBytes.length < 32)) {
            throw new IllegalStateException(
                "Staff invitation signing secret MUST be set via "
                + "kitehub.staff.invitation.signing-secret (>=32 bytes, not the dev default) "
                + "in production profile. Got length=" + secretBytes.length
                + ", isDevDefault=" + usingDevDefault);
        }

        // Boot-time round-trip self-test catches misconfiguration before any
        // real invitation is issued.
        UUID smokeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant smokeExpiry = Instant.now().plusSeconds(60);
        String token = generate(smokeId, smokeExpiry);
        InvitationTokenPayload payload = verify(token)
            .orElseThrow(() -> new IllegalStateException(
                "InvitationTokenService self-test failed: verify returned empty"));
        if (!smokeId.equals(payload.invitationId())) {
            throw new IllegalStateException(
                "InvitationTokenService self-test failed: round-trip mismatch");
        }
        log.info("InvitationTokenService initialised — HMAC-SHA256 round-trip OK "
            + "(production={}, devDefault={})", productionProfile, usingDevDefault);
    }

    /**
     * Generate a fresh signed token with default TTL.
     *
     * @param invitationId DB row id of the {@code StaffInvitation}
     * @return opaque token string safe to email (URL-safe base64 segments)
     */
    public String generate(UUID invitationId) {
        Instant expiresAt = Instant.now().plusSeconds(TOKEN_TTL_DAYS * 86400L);
        return generate(invitationId, expiresAt);
    }

    String generate(UUID invitationId, Instant expiresAt) {
        byte[] nonce = new byte[NONCE_LEN];
        rng.nextBytes(nonce);
        String nonceB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
        String payload = invitationId.toString() + ":" + expiresAt.getEpochSecond() + ":" + nonceB64;
        String signature = hmac(payload);
        // payload + "." + signature — keep payload human-debuggable for audit
        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return payloadB64 + "." + signature;
    }

    /**
     * Verify token authenticity + expiry. Returns empty when token malformed,
     * signature mismatch, or TTL exceeded — callers MUST treat empty as
     * "invalid token, do not reveal cause externally".
     */
    public Optional<InvitationTokenPayload> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        int dot = token.lastIndexOf('.');
        if (dot <= 0 || dot >= token.length() - 1) {
            return Optional.empty();
        }
        String payloadB64 = token.substring(0, dot);
        String providedSig = token.substring(dot + 1);
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }

        String expectedSig = hmac(payload);
        if (!constantTimeEquals(expectedSig, providedSig)) {
            return Optional.empty();
        }

        String[] parts = payload.split(":");
        if (parts.length != 3) {
            return Optional.empty();
        }
        UUID invitationId;
        long expiryEpoch;
        try {
            invitationId = UUID.fromString(parts[0]);
            expiryEpoch = Long.parseLong(parts[1]);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        Instant expiresAt = Instant.ofEpochSecond(expiryEpoch);
        if (expiresAt.isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(new InvitationTokenPayload(invitationId, expiresAt));
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALG));
            byte[] sig = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC computation failed", ex);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }

    /** Verified payload (id + expiry). */
    public record InvitationTokenPayload(UUID invitationId, Instant expiresAt) {}
}
