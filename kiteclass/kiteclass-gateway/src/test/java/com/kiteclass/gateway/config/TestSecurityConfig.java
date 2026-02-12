package com.kiteclass.gateway.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Test security configuration that disables most security for easier testing.
 *
 * <p>This configuration is active in test profile by default.
 * Tests that need real security (like JWT tests) should use @Import(SecurityConfig.class) instead.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@TestConfiguration
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
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Permissive security for most tests.
     * Tests needing real security should @Import(SecurityConfig.class).
     *
     * @param http ServerHttpSecurity builder
     * @return configured SecurityWebFilterChain
     */
    @Bean
    @Primary
    public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }
}
