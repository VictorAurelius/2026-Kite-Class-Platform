package com.kitehub.subscription.auth.twofactor;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TwoFactorAuthService} (GAP-516 Wave 72b Bucket A).
 */
@DisplayName("TwoFactorAuthService — TOTP secret + verify")
class TwoFactorAuthServiceTest {

    private final TwoFactorAuthService svc = new TwoFactorAuthService();

    @Test
    @DisplayName("generateSecret returns a base32 string of expected length")
    void generateSecret_isBase32() {
        String secret = svc.generateSecret();
        assertThat(secret).isNotBlank();
        // samstevens.totp default is 32 chars base32.
        assertThat(secret).hasSize(32);
        assertThat(secret).matches("[A-Z2-7]+");
    }

    @Test
    @DisplayName("generateQrUri contains issuer + label + secret")
    void generateQrUri_includesAllFields() {
        String secret = svc.generateSecret();
        String uri = svc.generateQrUri(secret, "admin@kitehub.me");
        assertThat(uri).startsWith("otpauth://totp/");
        assertThat(uri).contains("KiteHub");
        assertThat(uri).contains("admin");
        assertThat(uri).contains(secret);
    }

    @Test
    @DisplayName("verifyCode accepts the code generated for the current time window")
    void verifyCode_happyPath() throws Exception {
        String secret = svc.generateSecret();
        String code = generateCurrentCode(secret);
        assertThat(svc.verifyCode(secret, code)).isTrue();
    }

    @Test
    @DisplayName("verifyCode rejects a clearly-wrong code")
    void verifyCode_wrongCode() {
        String secret = svc.generateSecret();
        assertThat(svc.verifyCode(secret, "000000")).isFalse();
    }

    @Test
    @DisplayName("verifyCode rejects null / blank")
    void verifyCode_blank() {
        String secret = svc.generateSecret();
        assertThat(svc.verifyCode(secret, null)).isFalse();
        assertThat(svc.verifyCode(secret, "")).isFalse();
        assertThat(svc.verifyCode(secret, "   ")).isFalse();
    }

    @Test
    @DisplayName("buildManualUri matches expected otpauth shape")
    void buildManualUri_shape() {
        String secret = "JBSWY3DPEHPK3PXP";
        String uri = svc.buildManualUri(secret, "admin@kitehub.me");
        assertThat(uri).contains("otpauth://totp/");
        assertThat(uri).contains("KiteHub");
        assertThat(uri).contains("secret=" + secret);
        assertThat(uri).contains("period=30");
    }

    /** Helper to generate the TOTP code valid at the current 30s window. */
    private static String generateCurrentCode(String secret) throws Exception {
        SystemTimeProvider clock = new SystemTimeProvider();
        long counter = Math.floorDiv(clock.getTime(), 30);
        return new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6).generate(secret, counter);
    }
}
