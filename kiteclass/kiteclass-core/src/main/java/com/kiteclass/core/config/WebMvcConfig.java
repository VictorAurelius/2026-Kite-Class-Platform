package com.kiteclass.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for Core Service.
 *
 * <p>Registers interceptors:
 * <ul>
 *   <li>TenantFilterInterceptor - Multi-tenant data isolation</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantFilterInterceptor tenantFilterInterceptor;

    /**
     * Adds tenant filter interceptor to all API endpoints.
     *
     * <p>Excluded paths:
     * <ul>
     *   <li>/actuator/** - Health checks, metrics (no tenant context needed)</li>
     *   <li>/api/v1/auth/** - Authentication endpoints (tenant determined after auth)</li>
     * </ul>
     *
     * <p>Included paths:
     * <ul>
     *   <li>/api/** - Public API endpoints (tenant from JWT or header)</li>
     *   <li>/internal/** - Internal service-to-service calls (tenant from X-Tenant-Id header)</li>
     * </ul>
     *
     * @param registry the interceptor registry
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        log.info("Registering TenantFilterInterceptor for /api/** and /internal/**");
        registry.addInterceptor(tenantFilterInterceptor)
            .addPathPatterns("/api/**", "/internal/**")
            .excludePathPatterns(
                "/actuator/**",
                "/api/v1/auth/**"
            );
    }
}
