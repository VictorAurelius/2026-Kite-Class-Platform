package com.kitehub.branding.wizard.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Mints + verifies short-lived HMAC tokens that authorize a browser {@code EventSource}
 * (SSE) connection to the deploy-stream / preview endpoints (GAP-1021 part 2 — FM-4).
 *
 * <p><b>Why:</b> kitehub-branding trusts gateway-forwarded {@code X-User-Id}/{@code X-User-Roles}
 * headers, but a browser {@code EventSource} cannot set custom headers — so an authenticated
 * fetch first mints a token (carrying the caller's identity + the target jobId + a short TTL),
 * then opens the stream with {@code ?access_token=<token>}. {@link SseQueryTokenAuthFilter}
 * verifies it and re-establishes the Spring authentication for that one request.</p>
 *
 * <p>Token layout (all printable; base64url alphabet excludes {@code "."} and {@code ":"}):
 * <pre>
 *   inner  = b64url(userId) "." b64url(roles) "." jobId "." expiryEpochSeconds
 *   token  = inner ":" b64url(HMAC-SHA256(inner))
 * </pre>
 * Bound to a single jobId so a leaked token only re-opens the same stream, and expires within
 * {@code kitehub.branding.sse.token-ttl-seconds} (default 120s).</p>
 *
 * @since GAP-1021 (Wave branding-100 Bucket C)
 */
@Slf4j
@Service
public class SseTokenService {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long ttlSeconds;

    public SseTokenService(
            @Value("${kitehub.branding.sse.token-secret:dev-only-sse-secret-change-me}") String secret,
            @Value("${kitehub.branding.sse.token-ttl-seconds:120}") long ttlSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds > 0 ? ttlSeconds : 120;
    }

    /** Resolved SSE caller identity carried by a verified token. */
    public record SseAuth(String userId, String roles) {
    }

    /**
     * Mint a token authorizing {@code userId}/{@code roles} to stream {@code jobId}.
     */
    public String mint(String userId, String roles, UUID jobId) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String inner = enc(nullToEmpty(userId)) + "." + enc(nullToEmpty(roles)) + "."
                + jobId + "." + exp;
        return inner + ":" + B64.encodeToString(hmac(inner));
    }

    /**
     * Verify a token is well-formed, signature-valid, unexpired, and bound to {@code jobId}.
     *
     * @return the carried identity when valid, else empty.
     */
    public Optional<SseAuth> verify(String token, UUID jobId) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        int colon = token.lastIndexOf(':');
        if (colon <= 0 || colon == token.length() - 1) {
            return Optional.empty();
        }
        String inner = token.substring(0, colon);
        String sig = token.substring(colon + 1);
        if (!constantTimeEquals(sig, B64.encodeToString(hmac(inner)))) {
            return Optional.empty();
        }
        String[] parts = inner.split("\\.", -1);
        if (parts.length != 4) {
            return Optional.empty();
        }
        String userId;
        String roles;
        try {
            userId = dec(parts[0]);
            roles = dec(parts[1]);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        String tokenJobId = parts[2];
        long exp;
        try {
            exp = Long.parseLong(parts[3]);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
        if (!tokenJobId.equals(jobId.toString())) {
            return Optional.empty();
        }
        if (Instant.now().getEpochSecond() > exp) {
            return Optional.empty();
        }
        return Optional.of(new SseAuth(userId, roles));
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    private static String enc(String s) {
        return B64.encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String s) {
        return new String(B64D.decode(s), StandardCharsets.UTF_8);
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secret, HMAC_ALG));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            // Misconfiguration only — never expected at request time.
            throw new IllegalStateException("SSE token HMAC failure: " + ex.getMessage(), ex);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] ba = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ba.length != bb.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < ba.length; i++) {
            diff |= ba[i] ^ bb[i];
        }
        return diff == 0;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
