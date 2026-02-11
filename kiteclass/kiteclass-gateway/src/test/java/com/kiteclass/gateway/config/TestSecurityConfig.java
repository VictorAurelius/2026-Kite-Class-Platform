package com.kiteclass.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Test security configuration that disables security for tests.
 *
 * <p>This configuration is only active in test profile, replacing the main {@link SecurityConfig}.
 * Uses @Configuration (not @TestConfiguration) to ensure it's auto-discovered by component scanning.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Configuration
@EnableWebFluxSecurity
@Profile("test")
public class TestSecurityConfig {

    /**
     * Password encoder for tests.
     * Uses BCrypt same as production to ensure test compatibility.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
            .build();
    }
}
