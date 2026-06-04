package com.kitehub.subscription.config;

import com.kitehub.subscription.idempotency.interceptor.IdempotencyHandlerInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration — registers interceptors for cache-control + idempotency.
 *
 * <p>Admin endpoint authentication moved to Spring Security {@code @PreAuthorize}
 * per GAP-938 (Wave flow-kh3, 2026-06-04). The legacy {@code AdminApiKeyInterceptor}
 * (X-Admin-Key header) was deleted because Wave 79 GAP-552 default-deny migration
 * made Spring Security block requests before the interceptor ran — the X-Admin-Key
 * mechanism became dead code. JWT with {@code role=PLATFORM_ADMIN} forwarded by
 * gateway as {@code X-User-Id} + {@code X-User-Roles} is now the canonical auth path.</p>
 *
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final MagicLinkCacheControlInterceptor magicLinkCacheControlInterceptor;
    private final IdempotencyHandlerInterceptor idempotencyHandlerInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
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

        // GAP-536 Wave onboarding-polish-2 Bucket C — Stripe-style idempotency
        // cho POST /api/platform/instances. Pairs với IdempotencyCachingFilter
        // (wrap body để hash). Path pattern khớp EXACT — không cover sub-paths
        // như /api/platform/instances/{id}/extend-trial.
        //
        // Wave local-doable-10 Bucket A — GAP-730 extends scope sang signup +
        // beta-request POST endpoints. Mỗi path resolve sang endpoint id
        // riêng (POST_signup, POST_beta_request) qua resolveEndpoint() trong
        // interceptor; same Idempotency-Key reused giữa các endpoint không
        // collision vì key cache là (key, endpoint_id) composite.
        registry.addInterceptor(idempotencyHandlerInterceptor)
                .addPathPatterns(
                        "/api/platform/instances",
                        "/api/auth/register",
                        "/api/v1/auth/request-beta-access"
                );
    }
}
