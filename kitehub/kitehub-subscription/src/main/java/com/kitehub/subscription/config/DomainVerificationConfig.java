package com.kitehub.subscription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for custom domain verification.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "kitehub.domain.verification")
@Data
public class DomainVerificationConfig {

    /**
     * Timeout in hours for DNS TXT record verification.
     * Default: 48 hours.
     */
    private int timeoutHours = 48;

    /**
     * Enable mock mode: if DNS not resolvable, return PENDING instead of FAILED.
     * Useful for development/testing environments.
     */
    private boolean mockMode = true;
}
