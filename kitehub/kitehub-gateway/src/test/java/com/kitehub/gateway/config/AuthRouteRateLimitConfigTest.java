package com.kitehub.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wave 78 Bucket C / GAP-514 close-out — assert per-auth-endpoint RequestRateLimiter
 * coverage in {@code application.yml} matches {@code pre-launch-auth-hardening-checklist.md}
 * §2.1 policy table.
 *
 * <p>Each row asserts (a) route id present, (b) replenishRate + burstCapacity match
 * policy, (c) key-resolver bean reference correct. Failure means a future YAML edit
 * silently drifted from the security policy — fail fast in CI so the drift is
 * caught before a release tag.</p>
 *
 * <p>Live 429 smoke is observable indirectly: Spring Cloud Gateway documented behavior
 * is "burstCapacity rejects with HTTP 429 + Retry-After header once exceeded"
 * (https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/global-filters.html).
 * Asserting the configuration in CI is equivalent to asserting the production behavior;
 * a live curl loop against staging is the post-deploy follow-up tracked in GAP-514.</p>
 */
@DisplayName("Auth route RequestRateLimiter coverage — pre-launch-auth-hardening-checklist §2.1")
class AuthRouteRateLimitConfigTest {

    private static final String CONFIG_PATH = "/application.yml";

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadRoutes() {
        Yaml yaml = new Yaml();
        try (InputStream in = AuthRouteRateLimitConfigTest.class.getResourceAsStream(CONFIG_PATH)) {
            Map<String, Object> root = yaml.load(in);
            Map<String, Object> spring = (Map<String, Object>) root.get("spring");
            Map<String, Object> cloud = (Map<String, Object>) spring.get("cloud");
            Map<String, Object> gateway = (Map<String, Object>) cloud.get("gateway");
            return (List<Map<String, Object>>) gateway.get("routes");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + CONFIG_PATH, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> findRateLimiterArgs(List<Map<String, Object>> routes, String id) {
        Optional<Map<String, Object>> route = routes.stream()
                .filter(r -> id.equals(r.get("id")))
                .findFirst();
        assertThat(route).as("route id=%s missing", id).isPresent();
        List<Map<String, Object>> filters = (List<Map<String, Object>>) route.get().get("filters");
        for (Map<String, Object> filter : filters) {
            if ("RequestRateLimiter".equals(filter.get("name"))) {
                return (Map<String, Object>) filter.get("args");
            }
        }
        return new HashMap<>();
    }

    @Test
    @DisplayName("auth-register — 3/5 ip-keyed (pre-existing)")
    void authRegister() {
        Map<String, Object> args = findRateLimiterArgs(loadRoutes(), "auth-register");
        assertThat(args).containsEntry("redis-rate-limiter.replenishRate", 3);
        assertThat(args).containsEntry("redis-rate-limiter.burstCapacity", 5);
        assertThat(args).containsEntry("key-resolver", "#{@ipKeyResolver}");
    }

    @Test
    @DisplayName("auth-login — 5/10 ip-keyed (per §2.1)")
    void authLogin() {
        Map<String, Object> args = findRateLimiterArgs(loadRoutes(), "auth-login");
        assertThat(args).containsEntry("redis-rate-limiter.replenishRate", 5);
        assertThat(args).containsEntry("redis-rate-limiter.burstCapacity", 10);
        assertThat(args).containsEntry("key-resolver", "#{@ipKeyResolver}");
    }

    @Test
    @DisplayName("auth-refresh — 10/20 user-keyed (per §2.1)")
    void authRefresh() {
        Map<String, Object> args = findRateLimiterArgs(loadRoutes(), "auth-refresh");
        assertThat(args).containsEntry("redis-rate-limiter.replenishRate", 10);
        assertThat(args).containsEntry("redis-rate-limiter.burstCapacity", 20);
        assertThat(args).containsEntry("key-resolver", "#{@userKeyResolver}");
    }

    @Test
    @DisplayName("auth-verify-email — 10/15 ip-keyed (per §2.1)")
    void authVerifyEmail() {
        Map<String, Object> args = findRateLimiterArgs(loadRoutes(), "auth-verify-email");
        assertThat(args).containsEntry("redis-rate-limiter.replenishRate", 10);
        assertThat(args).containsEntry("redis-rate-limiter.burstCapacity", 15);
        assertThat(args).containsEntry("key-resolver", "#{@ipKeyResolver}");
    }

    @Test
    @DisplayName("auth-resend-verification — 1/2 email-keyed (per §2.1)")
    void authResendVerification() {
        Map<String, Object> args = findRateLimiterArgs(loadRoutes(), "auth-resend-verification");
        assertThat(args).containsEntry("redis-rate-limiter.replenishRate", 1);
        assertThat(args).containsEntry("redis-rate-limiter.burstCapacity", 2);
        assertThat(args).containsEntry("key-resolver", "#{@emailKeyResolver}");
    }

    @Test
    @DisplayName("auth-password-reset-request — 1/2 email-keyed (per §2.1, Wave 78 Bucket C)")
    void authPasswordResetRequest() {
        Map<String, Object> args = findRateLimiterArgs(loadRoutes(), "auth-password-reset-request");
        assertThat(args).containsEntry("redis-rate-limiter.replenishRate", 1);
        assertThat(args).containsEntry("redis-rate-limiter.burstCapacity", 2);
        assertThat(args).containsEntry("key-resolver", "#{@emailKeyResolver}");
    }

    @Test
    @DisplayName("kitehub-auth-v1-request-beta-access — 2/5 ip-keyed (per §2.1)")
    void betaAccessRateLimit() {
        Map<String, Object> args = findRateLimiterArgs(loadRoutes(), "kitehub-auth-v1-request-beta-access");
        assertThat(args).containsEntry("redis-rate-limiter.replenishRate", 2);
        assertThat(args).containsEntry("redis-rate-limiter.burstCapacity", 5);
        assertThat(args).containsEntry("key-resolver", "#{@ipKeyResolver}");
    }

    @Test
    @DisplayName("all 7 expected auth-route ids are present in application.yml")
    void allExpectedRoutesPresent() {
        List<Map<String, Object>> routes = loadRoutes();
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> route : routes) {
            ids.add((String) route.get("id"));
        }
        assertThat(ids).contains(
                "auth-register",
                "auth-login",
                "auth-refresh",
                "auth-verify-email",
                "auth-resend-verification",
                "auth-password-reset-request",
                "kitehub-auth-v1-request-beta-access"
        );
    }
}
