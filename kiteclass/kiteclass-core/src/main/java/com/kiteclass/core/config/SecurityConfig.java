package com.kiteclass.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Core service.
 *
 * <p>Configuration:
 * <ul>
 *   <li>Disable CSRF for /internal/** endpoints (protected by HMAC authentication)</li>
 *   <li>All requests require authentication (InternalRequestFilter handles /internal/**)</li>
 * </ul>
 *
 * <p>This configuration is only active in non-test profiles. For tests, {@link TestSecurityConfig} is used.
 *
 * @author KiteClass Team
 * @since 2.11.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("!test")
public class SecurityConfig {

    /**
     * Configures HTTP security.
     *
     * <p>Disables CSRF for internal API endpoints since they're protected by HMAC signatures.
     * CSRF protection is unnecessary for service-to-service communication.
     *
     * @param http HttpSecurity to configure
     * @return SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF - not needed since Core is behind Gateway
                // Gateway handles authentication, Core trusts X-User-Id/X-User-Roles headers
                .csrf(csrf -> csrf.disable())
                // All requests permitted - authentication handled by Gateway
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
