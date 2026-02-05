package com.kiteclass.core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    private TenantFilterInterceptor tenantFilterInterceptor;

    /**
     * Adds tenant filter interceptor to all API endpoints.
     *
     * <p>Excluded paths:
     * <ul>
     *   <li>/actuator/** - Health checks, metrics (no tenant context needed)</li>
     *   <li>/api/v1/auth/** - Authentication endpoints (tenant determined after auth)</li>
     *   <li>/internal/** - Internal service-to-service calls (use X-Internal-Request header)</li>
     * </ul>
     *
     * @param registry the interceptor registry
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Only register if tenantFilterInterceptor is available (requires EntityManager)
        if (tenantFilterInterceptor != null) {
            registry.addInterceptor(tenantFilterInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/actuator/**",
                    "/api/v1/auth/**",
                    "/internal/**"
                );
        }
    }
}
