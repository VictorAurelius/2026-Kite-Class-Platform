package com.kiteclass.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Global CORS configuration with highest priority.
 *
 * <p>This filter runs BEFORE Spring Security to ensure CORS preflight
 * (OPTIONS) requests are handled correctly.
 *
 * <p>Uses @Order(Ordered.HIGHEST_PRECEDENCE) to execute before security filters.
 *
 * @author KiteClass Team
 * @since 1.9.0
 */
@Configuration
public class CorsGlobalConfig {

    /**
     * CORS filter with highest precedence to handle preflight requests.
     *
     * @return CorsWebFilter configured to run before security filters
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allow localhost origins for development
        corsConfig.setAllowedOriginPatterns(Arrays.asList("http://localhost:*"));

        // Also allow specific origins
        corsConfig.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:3001"
        ));

        // Allow all HTTP methods
        corsConfig.setAllowedMethods(Arrays.asList("*"));

        // Allow all headers
        corsConfig.setAllowedHeaders(Arrays.asList("*"));

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
