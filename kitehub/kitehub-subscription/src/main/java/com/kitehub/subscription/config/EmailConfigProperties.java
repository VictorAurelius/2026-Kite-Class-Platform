package com.kitehub.subscription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for email feature toggles.
 * <p>
 * Allows admin to enable/disable specific email types at runtime
 * (in-memory update via admin API, persistent via application.yml).
 * <p>
 * Prefix: kitehub.email
 *
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "kitehub.email")
public class EmailConfigProperties {

    /**
     * Whether to use RabbitMQ queue for email dispatch (true)
     * or direct HTTP (false). Default: true.
     */
    private boolean useQueue = true;

    /**
     * Per email-type toggles. Key = email type (e.g., "trial-warning"),
     * value = enabled (true) or disabled (false).
     * Types not present in this map default to enabled.
     */
    private Map<String, Boolean> typeToggles = new HashMap<>();
}
