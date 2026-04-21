package com.kitehub.subscription.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MigrationWebhookVerifier} (GAP-192 Phase 4b-i).
 */
@DisplayName("MigrationWebhookVerifier")
class MigrationWebhookVerifierTest {

    private MigrationWebhookVerifier verifier;

    private static final String SECRET = "test-webhook-secret-0123456789";
    private static final String SAMPLE_BODY = "{\"eventType\":\"payment.captured\",\"paymentIntentId\":\"pi_1\"}";

    @BeforeEach
    void setUp() {
        verifier = new MigrationWebhookVerifier();
        ReflectionTestUtils.setField(verifier, "webhookSecret", SECRET);
    }

    private static String hmacHex(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("accepts a signature computed with the shared secret")
    void acceptsValid() throws Exception {
        String signature = hmacHex(SECRET, SAMPLE_BODY);
        assertThat(verifier.verify(SAMPLE_BODY, signature)).isTrue();
    }

    @Test
    @DisplayName("accepts signature in uppercase hex (normalised to lowercase)")
    void acceptsUppercaseHex() throws Exception {
        String signature = hmacHex(SECRET, SAMPLE_BODY).toUpperCase();
        assertThat(verifier.verify(SAMPLE_BODY, signature)).isTrue();
    }

    @Test
    @DisplayName("rejects when signature was computed with a different secret")
    void rejectsWrongSecret() throws Exception {
        String signature = hmacHex("wrong-secret", SAMPLE_BODY);
        assertThat(verifier.verify(SAMPLE_BODY, signature)).isFalse();
    }

    @Test
    @DisplayName("rejects when body was tampered with after signing")
    void rejectsTamperedBody() throws Exception {
        String signature = hmacHex(SECRET, SAMPLE_BODY);
        String tampered = SAMPLE_BODY.replace("pi_1", "pi_9999");
        assertThat(verifier.verify(tampered, signature)).isFalse();
    }

    @Test
    @DisplayName("rejects null / blank signatures")
    void rejectsNullSignature() {
        assertThat(verifier.verify(SAMPLE_BODY, null)).isFalse();
        assertThat(verifier.verify(SAMPLE_BODY, "")).isFalse();
    }

    @Test
    @DisplayName("rejects when secret is not configured")
    void rejectsNoSecret() throws Exception {
        ReflectionTestUtils.setField(verifier, "webhookSecret", "");
        String signature = hmacHex(SECRET, SAMPLE_BODY);
        assertThat(verifier.verify(SAMPLE_BODY, signature)).isFalse();
    }
}
