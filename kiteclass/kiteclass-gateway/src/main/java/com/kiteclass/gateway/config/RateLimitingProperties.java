package com.kiteclass.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for rate limiting.
 *
 * @author KiteClass Team
 * @since 1.6.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitingProperties {

    /**
     * Enable or disable rate limiting.
     */
    private boolean enabled = true;

    /**
     * Requests per minute for unauthenticated users (by IP address).
     */
    private long unauthenticatedRequestsPerMinute = 100;

    /**
     * Requests per minute for authenticated users.
     */
    private long authenticatedRequestsPerMinute = 1000;

    /**
     * Time window in seconds for rate limiting.
     */
    private long timeWindowSeconds = 60;
}
