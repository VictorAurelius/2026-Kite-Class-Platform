package com.kiteclass.core.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Validates that critical security configuration values are not using defaults.
 * Only active in production profile — dev profile allows default values.
 *
 * @since 2026-03-24
 */
@Configuration
@Profile("prod")
public class SecurityConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfigValidator.class);

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${internal.api.secret:}")
    private String internalApiSecret;

    @PostConstruct
    public void validateSecurityConfig() {
        if (dbPassword.isBlank() || "kiteclass123".equals(dbPassword)) {
            throw new IllegalStateException(
                "SECURITY: Database password must not be blank or default value in production");
        }
        if (internalApiSecret.isBlank() || internalApiSecret.contains("dev-internal-secret")) {
            throw new IllegalStateException(
                "SECURITY: Internal API secret must not be blank or default value in production");
        }
        log.info("Security configuration validated successfully");
    }
}
