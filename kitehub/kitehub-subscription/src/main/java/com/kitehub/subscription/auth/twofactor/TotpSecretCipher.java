package com.kitehub.subscription.auth.twofactor;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-GCM helper for encrypting TOTP secrets at rest (GAP-516).
 *
 * <p>Phase 1 BETA stores the encryption key in config
 * ({@code kitehub.auth.totp.encryption-key}, 32 bytes). Phase 1.5+ migrates this
 * to AWS KMS — see {@code pre-launch-secrets-hardening-checklist.md} §2.4.</p>
 *
 * <p>Cipher: AES-256-GCM with a fresh 12-byte IV per encryption. Output format
 * (base64) is {@code IV || CIPHERTEXT || GCM_TAG}.</p>
 *
 * @since 1.0.0 (Wave 72b)
 */
@Component
@Slf4j
public class TotpSecretCipher {

    private static final int GCM_IV_LEN = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String TRANSFORM = "AES/GCM/NoPadding";

    /**
     * Hard-coded dev fallback documented in this file's javadoc. Production MUST
     * override via {@code kitehub.auth.totp.encryption-key} env — equality to this
     * string triggers fail-fast in production profile (GAP-553).
     */
    static final String DEV_DEFAULT_KEY = "dev-key-32-chars-pad-pad-pad-pad-pad";

    private final SecretKey key;
    private final SecureRandom rng = new SecureRandom();
    private final boolean productionProfile;
    private final boolean usingDevDefault;
    private final int configuredKeyLength;

    public TotpSecretCipher(
        @Value("${kitehub.auth.totp.encryption-key:dev-key-32-chars-pad-pad-pad-pad-pad}") String configuredKey,
        Environment environment) {
        // Detect production profile up-front so #validate() can fail-fast (GAP-553).
        this.productionProfile = isProduction(environment);
        this.usingDevDefault = DEV_DEFAULT_KEY.equals(configuredKey);
        this.configuredKeyLength = configuredKey.getBytes(StandardCharsets.UTF_8).length;

        // We want a 32-byte AES-256 key. If the config value is shorter we pad
        // with the ASCII representation; if longer we truncate. In production the
        // config MUST supply ≥32 bytes — checked in #validate().
        byte[] keyBytes = new byte[32];
        byte[] src = configuredKey.getBytes(StandardCharsets.UTF_8);
        if (src.length < 32 && !productionProfile) {
            log.warn("TOTP encryption key length {} < 32; padding with zeros (non-prod profile only). "
                + "MUST set kitehub.auth.totp.encryption-key (≥32 bytes) in production.", src.length);
        }
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 32));
        this.key = new SecretKeySpec(keyBytes, "AES");
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
        // GAP-553 fail-fast: refuse to boot in production if key matches the
        // hard-coded dev default OR if config supplied < 32 bytes of entropy.
        if (productionProfile && (usingDevDefault || configuredKeyLength < 32)) {
            throw new IllegalStateException(
                "TOTP encryption key MUST be set via kitehub.auth.totp.encryption-key "
                + "(>=32 bytes, not the dev default) in production profile. "
                + "Got length=" + configuredKeyLength + ", isDevDefault=" + usingDevDefault);
        }

        // Smoke test: encrypt+decrypt a known plaintext at boot to catch
        // misconfiguration before any real secret is processed.
        String roundTrip = decrypt(encrypt("totp-cipher-smoke"));
        if (!"totp-cipher-smoke".equals(roundTrip)) {
            throw new IllegalStateException("TotpSecretCipher self-test failed");
        }
        log.info("TotpSecretCipher initialised — AES/GCM round-trip OK (production={}, devDefault={})",
            productionProfile, usingDevDefault);
    }

    /** Encrypt a UTF-8 plaintext (typically a base32 TOTP secret). */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            rng.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt TOTP secret", ex);
        }
    }

    /** Decrypt a base64 payload produced by {@link #encrypt(String)}. */
    public String decrypt(String encoded) {
        try {
            byte[] in = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[GCM_IV_LEN];
            byte[] ct = new byte[in.length - GCM_IV_LEN];
            System.arraycopy(in, 0, iv, 0, GCM_IV_LEN);
            System.arraycopy(in, GCM_IV_LEN, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt TOTP secret", ex);
        }
    }
}
