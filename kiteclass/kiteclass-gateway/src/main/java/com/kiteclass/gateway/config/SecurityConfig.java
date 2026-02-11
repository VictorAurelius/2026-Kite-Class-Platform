package com.kiteclass.gateway.config;

import com.kiteclass.gateway.security.SecurityContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Security configuration for the Gateway service.
 *
 * <p>Configures:
 * <ul>
 *   <li>JWT-based authentication</li>
 *   <li>Public and protected endpoints</li>
 *   <li>Role-based access control</li>
 *   <li>CSRF disabled for API</li>
 *   <li>Password encoder</li>
 * </ul>
 *
 * <p>This configuration is only active in non-test profiles. For tests, {@link TestSecurityConfig} is used.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Configuration
// TEMPORARY: Disable Spring Security entirely for CORS testing
// @EnableWebFluxSecurity
// @EnableReactiveMethodSecurity
@RequiredArgsConstructor
@Profile("!test")
public class SecurityConfig {

    private final SecurityContextRepository securityContextRepository;

    /**
     * Password encoder using BCrypt.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security filter chain configuration.
     *
     * @param http ServerHttpSecurity builder
     * @return configured SecurityWebFilterChain
     */
    // TEMPORARY: Disable entire security chain for CORS testing
    // @Bean
    public SecurityWebFilterChain securityWebFilterChain_DISABLED(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                // CORS handled by Nginx, no Spring CORS config needed
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())

                // TEMPORARY: Disable security context repository to allow CORS for auth endpoints
                // TODO: Re-enable after fixing CORS properly
                // .securityContextRepository(securityContextRepository)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return Mono.empty();
                        })
                        .accessDeniedHandler((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return Mono.empty();
                        })
                )

                .authorizeExchange(auth -> auth
                        // TEMPORARY: Allow ALL requests without authentication for CORS testing
                        // TODO: Restore proper authorization rules after fixing CORS
                        .anyExchange().permitAll()
                )

                .build();
    }
}
