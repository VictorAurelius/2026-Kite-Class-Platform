package com.kiteclass.core.common.security.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoubleSubmitCsrfTokenProviderTest {

    private static final String GOOD_SECRET =
            "test-secret-at-least-32-bytes-long-0123456789";

    private DoubleSubmitCsrfTokenProvider provider(String secret, long ttlHours) {
        DoubleSubmitCsrfTokenProvider p = new DoubleSubmitCsrfTokenProvider(secret, ttlHours);
        p.validateSecret();
        return p;
    }

    @Test
    void startupFailsOnMissingSecret() {
        DoubleSubmitCsrfTokenProvider p = new DoubleSubmitCsrfTokenProvider("", 4);
        assertThatThrownBy(p::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("security.csrf.secret");
    }

    @Test
    void startupFailsOnInsecureDefault() {
        DoubleSubmitCsrfTokenProvider p = new DoubleSubmitCsrfTokenProvider("insecure-default", 4);
        assertThatThrownBy(p::validateSecret).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startupFailsOnPlaceholder() {
        DoubleSubmitCsrfTokenProvider p = new DoubleSubmitCsrfTokenProvider(
                "PLEASE_OVERRIDE_IN_PROD_32_BYTE_MIN", 4);
        assertThatThrownBy(p::validateSecret).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startupFailsOnShortSecret() {
        DoubleSubmitCsrfTokenProvider p = new DoubleSubmitCsrfTokenProvider("short", 4);
        assertThatThrownBy(p::validateSecret).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void issueProducesThreePartToken() {
        String t = provider(GOOD_SECRET, 4).issue();

        assertThat(t.split("\\.")).hasSize(3);
    }

    @Test
    void issueProducesUniqueTokens() {
        DoubleSubmitCsrfTokenProvider p = provider(GOOD_SECRET, 4);

        String a = p.issue();
        String b = p.issue();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void verifyAcceptsMatchingHeaderAndCookie() {
        DoubleSubmitCsrfTokenProvider p = provider(GOOD_SECRET, 4);
        String t = p.issue();

        assertThat(p.verify(t, t)).isTrue();
    }

    @Test
    void verifyRejectsMismatchedHeaderAndCookie() {
        DoubleSubmitCsrfTokenProvider p = provider(GOOD_SECRET, 4);
        String t1 = p.issue();
        String t2 = p.issue();

        assertThat(p.verify(t1, t2)).isFalse();
    }

    @Test
    void verifyRejectsTamperedSignature() {
        DoubleSubmitCsrfTokenProvider p = provider(GOOD_SECRET, 4);
        String t = p.issue();
        // Flip last char of signature portion
        String[] parts = t.split("\\.");
        char c = parts[2].charAt(parts[2].length() - 1);
        char flipped = c == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "."
                + parts[2].substring(0, parts[2].length() - 1) + flipped;

        assertThat(p.verify(tampered, tampered)).isFalse();
    }

    @Test
    void verifyRejectsWrongFormat() {
        DoubleSubmitCsrfTokenProvider p = provider(GOOD_SECRET, 4);
        assertThat(p.verify("abc", "abc")).isFalse();
        assertThat(p.verify("a.b", "a.b")).isFalse();
    }

    @Test
    void verifyRejectsEmptyOrNull() {
        DoubleSubmitCsrfTokenProvider p = provider(GOOD_SECRET, 4);
        assertThat(p.verify(null, null)).isFalse();
        assertThat(p.verify("", "")).isFalse();
        assertThat(p.verify("x", null)).isFalse();
        assertThat(p.verify(null, "x")).isFalse();
    }

    @Test
    void verifyRejectsExpiredToken() throws Exception {
        // ttl = 1 hour in signature path; we synthesize a token dated 10 hours ago.
        DoubleSubmitCsrfTokenProvider p = provider(GOOD_SECRET, 1);
        String fresh = p.issue();
        String[] parts = fresh.split("\\.");

        // Build an issuedAt 10 hours in the past
        long old = java.time.Instant.now().minusSeconds(10 * 3600).getEpochSecond();
        String oldIssuedAtB64 = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Long.toString(old).getBytes());

        // Re-sign nonce|oldIssuedAt with the same secret (using reflection-free re-issue via
        // constructing the expected signature ourselves).
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(
                GOOD_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal((parts[0] + "|" + oldIssuedAtB64)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String sigB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(sig);

        String expired = parts[0] + "." + oldIssuedAtB64 + "." + sigB64;

        assertThat(p.verify(expired, expired)).isFalse();
    }
}
