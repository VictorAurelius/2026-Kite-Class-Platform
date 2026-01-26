package com.kiteclass.gateway.module.user.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Test security configuration that disables security for controller tests.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@TestConfiguration
@EnableWebFluxSecurity
public class TestSecurityConfig {

    @Bean
    public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
            .build();
    }
}
