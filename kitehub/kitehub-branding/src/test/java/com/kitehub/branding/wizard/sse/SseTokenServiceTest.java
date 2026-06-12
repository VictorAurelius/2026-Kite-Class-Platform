package com.kitehub.branding.wizard.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link SseTokenService} (GAP-1021 — SSE EventSource auth via query token).
 */
@DisplayName("SseTokenService")
class SseTokenServiceTest {

    private final SseTokenService service = new SseTokenService("unit-test-secret", 120);

    @Test
    @DisplayName("mint → verify round-trip returns the carried identity")
    void roundTrip() {
        UUID jobId = UUID.randomUUID();
        String token = service.mint("user-42", "OWNER,ADMIN", jobId);

        Optional<SseTokenService.SseAuth> auth = service.verify(token, jobId);

        assertThat(auth).isPresent();
        assertThat(auth.get().userId()).isEqualTo("user-42");
        assertThat(auth.get().roles()).isEqualTo("OWNER,ADMIN");
    }

    @Test
    @DisplayName("token bound to a different jobId is rejected")
    void wrongJobIdRejected() {
        UUID jobId = UUID.randomUUID();
        String token = service.mint("user-42", "OWNER", jobId);

        assertThat(service.verify(token, UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("tampered signature is rejected")
    void tamperedRejected() {
        UUID jobId = UUID.randomUUID();
        String token = service.mint("user-42", "OWNER", jobId);
        String tampered = token.substring(0, token.length() - 2) + "xy";

        assertThat(service.verify(tampered, jobId)).isEmpty();
    }

    @Test
    @DisplayName("token minted with a different secret is rejected")
    void differentSecretRejected() {
        UUID jobId = UUID.randomUUID();
        String token = new SseTokenService("other-secret", 120).mint("user-42", "OWNER", jobId);

        assertThat(service.verify(token, jobId)).isEmpty();
    }

    @Test
    @DisplayName("expired token is rejected")
    void expiredRejected() throws InterruptedException {
        SseTokenService shortLived = new SseTokenService("unit-test-secret", 1);
        UUID jobId = UUID.randomUUID();
        String token = shortLived.mint("user-42", "OWNER", jobId);
        // TTL=1s, expiry is second-granular (epoch seconds). Sleep > 2s so the current epoch
        // second is strictly greater than the expiry second regardless of sub-second start.
        Thread.sleep(2200);

        assertThat(shortLived.verify(token, jobId)).isEmpty();
    }

    @Test
    @DisplayName("null / blank / malformed tokens are rejected, not thrown")
    void malformedRejected() {
        UUID jobId = UUID.randomUUID();
        assertThat(service.verify(null, jobId)).isEmpty();
        assertThat(service.verify("", jobId)).isEmpty();
        assertThat(service.verify("not-a-token", jobId)).isEmpty();
        assertThat(service.verify("a.b.c:sig", jobId)).isEmpty();
    }
}
