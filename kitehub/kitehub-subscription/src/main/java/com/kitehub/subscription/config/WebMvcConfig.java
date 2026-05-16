package com.kitehub.subscription.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration — registers interceptors for admin endpoint protection.
 *
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminApiKeyInterceptor adminApiKeyInterceptor;
    private final MagicLinkCacheControlInterceptor magicLinkCacheControlInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminApiKeyInterceptor)
                .addPathPatterns("/api/platform/admin/**");

        // Wave 86 GAP-584 AC#2 — origin defense-in-depth for magic-link / invite
        // single-use token endpoints. Pairs with edge layer Cloudflare Page Rule
        // (AC#1) so any intermediate cache between origin + client is forbidden
        // from storing a token-bearing response.
        registry.addInterceptor(magicLinkCacheControlInterceptor)
                .addPathPatterns(
                        "/api/v1/auth/beta-signup",
                        "/api/v1/auth/beta-signup/**",
                        "/api/v1/auth/magic",
                        "/api/v1/auth/magic/**",
                        "/api/v1/auth/invite",
                        "/api/v1/auth/invite/**"
                );
    }
}
