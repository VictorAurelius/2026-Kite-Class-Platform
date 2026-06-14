package com.kitehub.subscription.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-1372 — verify {@link AuthService#maskEmail(String)} never echoes a full
 * plaintext email into logs (PII per {@code logs-format-standard.md} §3.1).
 *
 * <p>The login attempt log + every sister auth-flow log site in {@link AuthService}
 * now routes its email through {@code maskEmail} until the platform-wide scrubber
 * (GAP-116) lands. This test pins the masking contract so a future refactor that
 * drops the mask is caught at PR time.</p>
 */
@DisplayName("AuthService.maskEmail — GAP-1372 PII quick-mask")
class AuthServiceMaskEmailTest {

    @Test
    @DisplayName("standard email → first char + ***@domain (local part never leaks)")
    void masksStandardEmail() {
        String masked = AuthService.maskEmail("alice@kitehub.me");
        assertThat(masked).isEqualTo("a***@kitehub.me");
        // The full local part must be gone — no enumeration leak.
        assertThat(masked).doesNotContain("alice");
    }

    @Test
    @DisplayName("null / blank → *** (no NPE, nothing to leak)")
    void masksNullAndBlank() {
        assertThat(AuthService.maskEmail(null)).isEqualTo("***");
        assertThat(AuthService.maskEmail("")).isEqualTo("***");
        assertThat(AuthService.maskEmail("   ")).isEqualTo("***");
    }

    @Test
    @DisplayName("malformed (no @) → first char + *** (still no full value)")
    void masksMalformed() {
        assertThat(AuthService.maskEmail("notanemail")).isEqualTo("n***");
    }

    @Test
    @DisplayName("leading @ → first char + *** (avoids substring(0) echo)")
    void masksLeadingAt() {
        assertThat(AuthService.maskEmail("@domain.com")).isEqualTo("@***");
    }
}
