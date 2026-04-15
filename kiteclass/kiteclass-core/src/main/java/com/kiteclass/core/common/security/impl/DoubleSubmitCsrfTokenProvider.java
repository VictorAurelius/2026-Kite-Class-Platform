package com.kiteclass.core.common.security.impl;

import com.kiteclass.core.common.security.CsrfTokenProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Double-submit cookie CSRF token provider signed with HMAC-SHA256 per ADR-011.
 *
 * <p>Token format: {@code base64Url(nonce).base64Url(issuedAtEpochSec).base64Url(HMAC_SHA256(secret, nonce|issuedAt))}.
 *
 * <p>Verification is constant-time (on both the signature and the header↔cookie comparison) to
 * close timing side-channels.
 *
 * <p>The secret is read from {@code security.csrf.secret}. Values of {@code null}, blank, the
 * default placeholder {@code PLEASE_OVERRIDE_IN_PROD_32_BYTE_MIN}, or {@code insecure-default}
 * are rejected at startup (fail-loud): a {@link IllegalStateException} will be thrown so the
 * service refuses to boot with an unsafe default.
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.2)
 */
@Component
public class DoubleSubmitCsrfTokenProvider implements CsrfTokenProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DoubleSubmitCsrfTokenProvider.class);
    private static final String HMAC_ALG = "HmacSHA256";
    private static final int NONCE_BYTES = 32;
    private static final Base64.Encoder B64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DEC = Base64.getUrlDecoder();

    private final String secret;
    private final long ttlSeconds;
    private final SecureRandom random;

    public DoubleSubmitCsrfTokenProvider(
            @Value("${security.csrf.secret:}") String secret,
            @Value("${security.csrf.ttl-hours:4}") long ttlHours
    ) {
        this.secret = secret == null ? "" : secret;
        this.ttlSeconds = Duration.ofHours(Math.max(1, ttlHours)).toSeconds();
        this.random = new SecureRandom();
    }

    @PostConstruct
    void validateSecret() {
        if (secret.isBlank()
                || secret.equalsIgnoreCase("insecure-default")
                || secret.equals("PLEASE_OVERRIDE_IN_PROD_32_BYTE_MIN")
                || secret.length() < 32) {
            throw new IllegalStateException(
                    "security.csrf.secret is missing, default, or shorter than 32 chars — "
                            + "refusing to start with an insecure CSRF secret.");
        }
        LOG.info("CSRF token provider initialized (ttl={}h)", ttlSeconds / 3600);
    }

    @Override
    public String issue() {
        byte[] nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        long issuedAt = Instant.now().getEpochSecond();
        String nonceB64 = B64_URL.encodeToString(nonce);
        String issuedAtB64 = B64_URL.encodeToString(Long.toString(issuedAt).getBytes(StandardCharsets.UTF_8));
        String sig = sign(nonceB64 + "|" + issuedAtB64);
        return nonceB64 + "." + issuedAtB64 + "." + sig;
    }

    @Override
    public boolean verify(String token, String cookie) {
        if (token == null || cookie == null || token.isBlank() || cookie.isBlank()) {
            return false;
        }
        // Constant-time header↔cookie match first.
        if (!constantTimeEquals(token.getBytes(StandardCharsets.UTF_8),
                cookie.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        String nonceB64 = parts[0];
        String issuedAtB64 = parts[1];
        String givenSig = parts[2];

        String expectedSig = sign(nonceB64 + "|" + issuedAtB64);
        if (!constantTimeEquals(expectedSig.getBytes(StandardCharsets.UTF_8),
                givenSig.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        long issuedAt;
        try {
            String issuedAtStr = new String(B64_URL_DEC.decode(issuedAtB64), StandardCharsets.UTF_8);
            issuedAt = Long.parseLong(issuedAtStr);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        return now - issuedAt <= ttlSeconds && now >= issuedAt;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return B64_URL.encodeToString(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign CSRF token", ex);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }
        // MessageDigest.isEqual is documented constant-time on modern JDKs.
        return MessageDigest.isEqual(a, b);
    }
}
