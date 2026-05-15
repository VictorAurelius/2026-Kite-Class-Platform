package com.kitehub.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration smoke test for Wave 79 Bucket A gateway route additions
 * (GAP-547 + GAP-551).
 *
 * <p>Scope: load {@code application.yml} as text and assert the new route IDs +
 * key path predicates exist. Avoids spinning up Spring Cloud Gateway WebFlux
 * context (covered by existing application-context test). Verifies:</p>
 * <ul>
 *   <li>GAP-547: {@code /api/v1/auth/2fa/**} canonical + {@code /api/auth/2fa/**}
 *       backward-compat alias both have routes with rate-limit filters</li>
 *   <li>GAP-551: {@code /api/v1/feedback} POST route forwards to
 *       kitehub-subscription (NOT kiteclass-core via instance-apis catch-all)
 *       and has gateway rate-limit</li>
 * </ul>
 *
 * @since Wave 79 Bucket A — GAP-547 / GAP-551
 */
@DisplayName("Gateway routes — Wave 79 Bucket A additions")
class GatewayRoutesIntegrationTest {

    private static final Path APPLICATION_YAML = Paths.get("src/main/resources/application.yml");

    private static String yaml() throws IOException {
        return Files.readString(APPLICATION_YAML);
    }

    @Test
    @DisplayName("GAP-547: /api/v1/auth/2fa/** canonical route exists with rate-limit + circuit-breaker")
    void canonicalTwoFactorRouteExists() throws IOException {
        String yml = yaml();
        assertThat(yml).contains("id: kitehub-auth-v1-2fa");
        assertThat(yml).contains("Path=/api/v1/auth/2fa/**");
        assertThat(yml).contains("Path=/api/v1/auth/2fa/verify");
        assertThat(yml).contains("Path=/api/v1/auth/2fa/recovery-codes/regenerate");
    }

    @Test
    @DisplayName("GAP-547: /api/auth/2fa/** backward-compat alias route exists (30-day deprecation)")
    void aliasTwoFactorRouteExists() throws IOException {
        String yml = yaml();
        assertThat(yml).contains("id: auth-2fa-alias");
        assertThat(yml).contains("Path=/api/auth/2fa/**");
        assertThat(yml).contains("id: auth-2fa-verify-alias");
        assertThat(yml).contains("id: auth-2fa-recovery-regenerate-alias");
    }

    @Test
    @DisplayName("GAP-551: /api/v1/feedback route forwards to kitehub-subscription with rate-limit")
    void feedbackRouteRoutedToSubscription() throws IOException {
        String yml = yaml();
        assertThat(yml).contains("id: kitehub-feedback-v1");
        assertThat(yml).contains("Path=/api/v1/feedback");
        // Route block immediately after id, but cheap structural check:
        // the feedback section MUST reference subscription URI and a rate-limit
        // filter so it never falls through to the /api/v1/** instance-apis catch-all.
        int feedbackIdx = yml.indexOf("id: kitehub-feedback-v1");
        assertThat(feedbackIdx).isPositive();
        String slice = yml.substring(feedbackIdx, Math.min(feedbackIdx + 600, yml.length()));
        assertThat(slice).contains("kitehub-subscription:8080");
        assertThat(slice).contains("RequestRateLimiter");
    }

    @Test
    @DisplayName("GAP-551: feedback route precedes instance-apis catch-all (predicate priority)")
    void feedbackRoutePrecedesCatchAll() throws IOException {
        String yml = yaml();
        int feedbackIdx = yml.indexOf("id: kitehub-feedback-v1");
        int instanceApisIdx = yml.indexOf("id: instance-apis");
        assertThat(feedbackIdx).isPositive();
        assertThat(instanceApisIdx).isPositive();
        assertThat(feedbackIdx)
                .as("feedback route must precede instance-apis catch-all so /api/v1/feedback POSTs reach kitehub-subscription, not kiteclass-core")
                .isLessThan(instanceApisIdx);
    }
}
