package com.kitehub.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Admin-service web MVC configuration (GAP-654).
 *
 * <p>Registers {@link SunsetHeaderInterceptor} for the legacy {@code /api/platform/admin/**}
 * surface so deprecated responses carry RFC 8594 deprecation headers. Explicit bean name
 * ({@code "adminWebMvcConfig"}) avoids a bean-definition conflict when
 * {@link com.kitehub.admin.KiteHubAdminApplication} component-scans {@code com.kitehub.subscription}.</p>
 *
 * @since 1.0
 */
@Configuration("adminWebMvcConfig")
public class AdminWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SunsetHeaderInterceptor())
                .addPathPatterns("/api/platform/admin/**");
    }
}
