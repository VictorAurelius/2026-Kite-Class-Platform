package com.kiteclass.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS configuration for local (non-Gateway) endpoints.
 *
 * <p>Spring Cloud Gateway's globalcors only applies to routed requests.
 * This configuration handles CORS for locally handled endpoints like /api/v1/auth/**.
 *
 * @author KiteClass Team
 * @since 1.9.0
 */
@Configuration
public class WebCorsConfiguration {

    /**
     * CORS configuration source for Spring Security.
     *
     * @return CorsConfigurationSource for local endpoints
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allow localhost origins
        corsConfig.setAllowedOriginPatterns(Arrays.asList("http://localhost:*"));
        corsConfig.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:3001"
        ));

        // Allow all methods
        corsConfig.setAllowedMethods(Arrays.asList("*"));

        // Allow all headers
        corsConfig.setAllowedHeaders(Arrays.asList("*"));

        // Expose headers
        corsConfig.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Request-Id"
        ));

        // Allow credentials
        corsConfig.setAllowCredentials(true);

        // Max age
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return source;
    }
}
