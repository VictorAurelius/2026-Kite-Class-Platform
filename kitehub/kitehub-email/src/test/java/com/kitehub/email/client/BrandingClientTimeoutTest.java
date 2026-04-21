package com.kitehub.email.client;

import com.kitehub.email.dto.TenantBranding;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Timeout-path integration test for {@link BrandingClient} — closes the email
 * half of GAP-146 (remaining 3 sites from GAP-131).
 *
 * <p>{@code BrandingClient} wraps a reactive Netty {@code HttpClient} with a
 * 5 s connect timeout + {@code responseTimeout} of {@code timeoutSeconds+1}
 * (GAP-131 fix in this file). This test stands up a local {@link HttpServer}
 * that deliberately blocks for much longer than the configured timeout, then
 * asserts two properties:
 *
 * <ol>
 *   <li>The call returns within a bounded window (configured timeout + small slack),
 *       proving the Netty timeout fired and the blocking {@code .block()}
 *       call unwound — guard against regressing to JVM-default infinite
 *       timeouts (the original audit finding).</li>
 *   <li>The returned payload is the default branding, not an exception —
 *       emails must keep going even when the branding upstream is slow.</li>
 * </ol>
 *
 * <p>No WireMock dependency is added; {@link HttpServer} is part of the JDK.
 *
 * @since Wave 9-F (GAP-146)
 */
@DisplayName("BrandingClient — response timeout + graceful fallback (GAP-146)")
class BrandingClientTimeoutTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Slow upstream (> responseTimeout) must fall back to defaults within bounded time")
    void fallsBackWhenUpstreamExceedsResponseTimeout() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            // Deliberate blocking delay far longer than the 1s client timeout
            // so responseTimeout fires before any bytes reach the caller.
            try {
                Thread.sleep(Duration.ofSeconds(10).toMillis());
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                exchange.close();
            }
        });
        server.start();

        int port = server.getAddress().getPort();
        // timeoutSeconds=1 → Netty responseTimeout = 2s; reactor .timeout = 1s
        BrandingClient client = new BrandingClient(
                "http://127.0.0.1:" + port,
                /* timeoutSeconds */ 1,
                /* brandingEnabled */ true);

        Instant start = Instant.now();
        TenantBranding branding = client.fetchBranding(42L, "tenant-slow-upstream");
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(branding)
                .as("client must degrade gracefully on upstream timeout")
                .isNotNull();
        assertThat(branding.getDisplayName())
                .as("should receive DEFAULT branding, not upstream response")
                .isEqualTo("KiteClass");

        // Client timeout = 1s; Netty responseTimeout = 2s. Anything under ~5s
        // proves the timeout fired rather than the request hanging at JVM
        // defaults (which historically meant infinite).
        assertThat(elapsed)
                .as("call must return within bounded window — not block indefinitely")
                .isLessThan(Duration.ofSeconds(5));
    }
}
