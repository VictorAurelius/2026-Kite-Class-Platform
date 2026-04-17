package com.kitehub.subscription.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that enforces API key authentication on admin endpoints.
 * <p>
 * Checks for X-Admin-Key header. If key is not configured (empty),
 * admin endpoints are open (dev mode). In production, set ADMIN_API_KEY env var.
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class AdminApiKeyInterceptor implements HandlerInterceptor {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";

    @Value("${kitehub.admin.api-key:}")
    private String adminApiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // If no key configured, allow all (dev mode)
        if (adminApiKey == null || adminApiKey.isBlank()) {
            return true;
        }

        String providedKey = request.getHeader(ADMIN_KEY_HEADER);
        if (adminApiKey.equals(providedKey)) {
            return true;
        }

        log.warn("Unauthorized admin access attempt from {} to {}",
                request.getRemoteAddr(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Valid X-Admin-Key header required\"}");
        return false;
    }
}
