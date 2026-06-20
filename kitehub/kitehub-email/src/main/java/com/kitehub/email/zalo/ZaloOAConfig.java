package com.kitehub.email.zalo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the Zalo OA messaging provider.
 *
 * <p>Loads property block {@code zalo.*} into {@link ZaloProperties}. Bean
 * wiring for the {@link ZaloOAClient} implementations lives next to each impl
 * (the mock declares itself a {@code @Component} with conditional activation
 * via {@code @ConditionalOnProperty(prefix = "zalo", name = "provider",
 * havingValue = "mock", matchIfMissing = true)}).</p>
 *
 * <p><strong>Phase 1 (Wave local-doable-11 Bucket B):</strong> only the mock
 * implementation exists. {@link ZaloProperties#getProvider()} defaults to
 * {@code "mock"} so the platform boots without any Zalo credentials.</p>
 *
 * @since Wave local-doable-11 Bucket B (GAP-063 Phase 1 scaffold)
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ZaloOAConfig.ZaloProperties.class)
public class ZaloOAConfig {

    /**
     * Strongly-typed view of the {@code zalo.*} property block.
     */
    @Data
    @ConfigurationProperties(prefix = "zalo")
    public static class ZaloProperties {

        /**
         * Channel master switch — when {@code false} the {@code ZaloNotificationChannel}
         * adapter is not registered (no ZALO dispatch). Default {@code true} so the
         * Phase 1 mock channel is ON out of the box.
         */
        private boolean enabled = true;

        /**
         * Provider selector: {@code "mock"} (default, deterministic canned
         * responses) or {@code "live"} (real Zalo OA HTTP — requires Wave 12+
         * follow-up {@code ZaloOAHttpClient}).
         */
        private String provider = "mock";

        /**
         * Zalo OA account id (numeric string issued by Zalo when an OA is
         * created). Empty in mock; required for live.
         */
        private String oaId = "";

        /**
         * Zalo OA access token. Empty in mock; required for live. Live tokens
         * are short-lived — production wiring will pair this with a refresh
         * mechanism (deferred to Wave 12+ {@code ZaloOAHttpClient}).
         */
        private String accessToken = "";

        /**
         * Base URL of the Zalo OA HTTP API. Default points at the public
         * production endpoint; tests / mock override to a stub.
         */
        private String apiBaseUrl = "https://openapi.zalo.me";

        /**
         * Outbound request timeout for the live HTTP client, in seconds.
         * Default 5 — Zalo OA typical p95 is well under 1s.
         */
        private int timeoutSeconds = 5;

        /**
         * Logical-template-name → Zalo ZNS template id mapping. Consumed by
         * {@code ZaloNotificationChannel} to translate a {@code NotificationContext}
         * template name into the vendor ZNS template id. Empty in Phase 1 (mock ignores
         * the id); populated when the live ZNS adapter ships (Wave 12+).
         */
        private Map<String, String> znsTemplateIds = new HashMap<>();
    }
}
