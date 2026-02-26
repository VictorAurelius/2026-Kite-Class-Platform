package com.kiteclass.gateway.config;

import com.kiteclass.gateway.security.SecurityContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

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
     * CORS configuration source for development and E2E testing.
     *
     * <p>Allows frontend (localhost:3000) to call backend APIs.
     * In production, CORS is handled by Nginx reverse proxy.
     *
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow localhost origins for development
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://localhost:8090",
            "http://127.0.0.1:8090"
        ));

        // Allow all common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Expose headers
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "X-Tenant-Id", "Content-Type"
        ));

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * CORS web filter with highest priority.
     *
     * <p>Must run BEFORE Spring Security to handle OPTIONS preflight requests.
     *
     * @return CORS web filter
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        return new CorsWebFilter(corsConfigurationSource());
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
                // Enable CORS FIRST - must run before security filters
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(Arrays.asList(
                        "http://localhost:3000",
                        "http://127.0.0.1:3000",
                        "http://localhost:8090",
                        "http://127.0.0.1:8090"
                    ));
                    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    return config;
                }))
                .csrf(csrf -> csrf.disable())
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
                        // OPTIONS must be first to handle CORS preflight
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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
