package com.kiteclass.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for Gateway service.
 *
 * <p>Configures Cross-Origin Resource Sharing (CORS) to allow frontend
 * applications to make requests to the Gateway API.
 *
 * <p>Allowed origins:
 * <ul>
 *   <li>http://localhost:3000 (Next.js development server)</li>
 *   <li>http://localhost:3001 (Alternative frontend port)</li>
 * </ul>
 *
 * <p>Allowed headers include: Content-Type, Authorization, X-Tenant-Id,
 * X-Request-Id, and standard CORS headers.
 *
 * @author KiteClass Team
 * @since 1.9.0
 */
@Configuration
public class CorsConfig {

    /**
     * CORS filter for WebFlux reactive endpoints.
     *
     * @return CorsWebFilter configured with allowed origins, methods, and headers
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allow localhost origins for development
        corsConfig.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:3001"
        ));

        // Allow all HTTP methods
        corsConfig.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Allow common headers
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization",
            "X-Tenant-Id",
            "X-Request-Id",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));

        // Expose headers that frontend needs to read
        corsConfig.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Request-Id"
        ));

        // Allow credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(true);

        // Max age for preflight cache (1 hour)
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
