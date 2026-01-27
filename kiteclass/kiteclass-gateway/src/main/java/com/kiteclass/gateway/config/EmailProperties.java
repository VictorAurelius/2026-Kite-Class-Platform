package com.kiteclass.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Email configuration properties.
 *
 * <p>Binds properties from application.yml under 'email' prefix.
 *
 * @author KiteClass Team
 * @since 1.5.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "email")
public class EmailProperties {

    /**
     * Email sender (from address with optional name).
     * Example: "KiteClass <noreply@kiteclass.com>"
     */
    private String from = "KiteClass <noreply@kiteclass.com>";

    /**
     * Base URL of the application (for email links).
     * Example: "http://localhost:3000" or "https://kiteclass.com"
     */
    private String baseUrl = "http://localhost:3000";

    /**
     * Password reset token expiration in milliseconds.
     * Default: 1 hour (3600000ms)
     */
    private Long resetTokenExpiration = 3600000L;
}
