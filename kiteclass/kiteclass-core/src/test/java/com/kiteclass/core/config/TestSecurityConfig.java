package com.kiteclass.core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that disables security for controller tests.
 *
 * @author KiteClass Team
 * @since 2.11.0
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    /**
     * Configures a permissive security filter chain for testing.
     * Disables CSRF and allows all requests without authentication.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }
}
