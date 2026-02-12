package com.kiteclass.gateway.config;

import com.kiteclass.gateway.security.SecurityContextRepository;
import lombok.RequiredArgsConstructor;
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
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
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
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                // CORS handled by Nginx, no Spring CORS config needed
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())

                // JWT authentication via SecurityContextRepository
                .securityContextRepository(securityContextRepository)

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
                        // Public endpoints - no authentication required
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/actuator/health/**").permitAll()
                        .pathMatchers("/api/v1/auth/**").permitAll()  // All auth endpoints public
                        .pathMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()

                        // User management - requires ADMIN or OWNER role
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/**").hasAnyRole("ADMIN", "OWNER", "STAFF")
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/**").hasAnyRole("ADMIN", "OWNER")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/users/**").hasAnyRole("ADMIN", "OWNER")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("OWNER")

                        // All other requests require authentication
                        .anyExchange().authenticated()
                )

                .build();
    }
}
