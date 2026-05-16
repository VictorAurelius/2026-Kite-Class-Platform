package com.kitehub.subscription.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Sets {@code Cache-Control: no-store} (+ legacy {@code Pragma}/{@code Expires}) headers on
 * magic-link / beta-signup invite endpoints to prevent intermediate caching of
 * single-use tokens.
 *
 * <p>Origin defense-in-depth pairing the edge layer Cloudflare Page Rule
 * ({@code cache_level=bypass} on {@code *kitehub.me/auth/magic*} +
 * {@code *kitehub.me/auth/invite/*}) from GAP-584 AC#1. Even if a future proxy /
 * CDN re-introduces caching, the origin response forbids it — closing GAP-584
 * AC#2 (Wave 86).
 *
 * <p>Closes the cross-tenant invite redirect leak risk described in
 * {@code documents/04-quality/gaps/GAP-584-magic-link-cloudflare-cache-bypass.md}
 * §Problem (simulation cell 19 — tenant B clicks shortly after tenant A and
 * receives cached redirect → onboarding-flow security breach).
 *
 * <p>Scope: any request path matching the patterns registered in
 * {@link WebMvcConfig#addInterceptors}. Currently {@code /api/v1/auth/beta-signup/**}
 * which maps the invite-token endpoints (per
 * {@code BetaAccessController}). Add new path patterns there if magic-link
 * endpoints under {@code /auth/magic/**} are introduced.
 *
 * @since Wave 86 — GAP-584 AC#2
 */
@Component
public class MagicLinkCacheControlInterceptor implements HandlerInterceptor {

    static final String CACHE_CONTROL_VALUE = "no-store, no-cache, max-age=0, must-revalidate";
    static final String PRAGMA_VALUE = "no-cache";
    static final String EXPIRES_VALUE = "0";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // Set BEFORE handler runs so downstream cannot accidentally override
        // with a Spring default (e.g., @ResponseBody convention).
        response.setHeader("Cache-Control", CACHE_CONTROL_VALUE);
        response.setHeader("Pragma", PRAGMA_VALUE);
        response.setHeader("Expires", EXPIRES_VALUE);
        return true;
    }
}
