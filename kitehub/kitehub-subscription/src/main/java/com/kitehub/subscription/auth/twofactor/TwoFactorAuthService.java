package com.kitehub.subscription.auth.twofactor;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * TOTP secret + verification helper for 2FA (GAP-516).
 *
 * <p>Thin wrapper around {@code dev.samstevens.totp} so the rest of the codebase
 * (controller, service, tests) only depends on this domain-friendly facade. The
 * verifier allows a ±1 step (30s) skew per BR-AUTH-006.</p>
 *
 * @since 1.0.0 (Wave 72b GAP-516)
 */
@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {

    /** Issuer label baked into the otpauth URI. Surfaced in authenticator apps. */
    private static final String ISSUER = "KiteHub";
    /** Step-skew tolerance in 30-second windows. */
    private static final int ALLOWED_DISCREPANCY = 1;
    /** TOTP step duration. RFC 6238 default. */
    private static final int TIME_PERIOD = 30;

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator(); // 32-char base32
    private final CodeVerifier verifier = newVerifier();

    /** Generate a fresh base32-encoded TOTP secret (160 bits). */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Build the otpauth URI consumed by authenticator apps when they scan the
     * QR. The {@code accountLabel} is typically the user's email.
     */
    public String generateQrUri(String secret, String accountLabel) {
        QrData data = new QrData.Builder()
            .label(accountLabel)
            .secret(secret)
            .issuer(ISSUER)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(TIME_PERIOD)
            .build();
        return data.getUri();
    }

    /** Convenience for tests / docs — returns a manually-built otpauth URI. */
    public String buildManualUri(String secret, String accountLabel) {
        String label = URLEncoder.encode(ISSUER + ":" + accountLabel, StandardCharsets.UTF_8);
        String issuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
            + "?secret=" + secret
            + "&issuer=" + issuer
            + "&algorithm=SHA1&digits=6&period=" + TIME_PERIOD;
    }

    /**
     * Verify a candidate TOTP code against a stored secret. Returns true when
     * the code matches within the ±1 step skew window.
     */
    public boolean verifyCode(String secret, String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        return verifier.isValidCode(secret, candidate.trim());
    }

    private static CodeVerifier newVerifier() {
        DefaultCodeVerifier v = new DefaultCodeVerifier(
            new DefaultCodeGenerator(),
            new SystemTimeProvider());
        v.setAllowedTimePeriodDiscrepancy(ALLOWED_DISCREPANCY);
        v.setTimePeriod(TIME_PERIOD);
        return v;
    }
}
