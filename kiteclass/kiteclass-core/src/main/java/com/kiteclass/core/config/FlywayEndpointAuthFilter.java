package com.kiteclass.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Admin-only access filter for the Spring Actuator Flyway endpoint
 * ({@code /actuator/flyway}).
 *
 * <p>GAP-476 — exposes Flyway migration head version to authenticated admin
 * users (via smoke-test or operations runbooks) without making it public.
 *
 * <p><strong>Auth model:</strong> mirrors the project-wide gateway-trust pattern
 * (see {@link SecurityConfig} javadoc): the Gateway authenticates the caller
 * and forwards the {@code X-User-Roles} header. This filter requires that
 * header to contain the {@code ADMIN} role (case-insensitive; comma- or
 * space-separated). Requests missing the role or header receive HTTP 401.
 *
 * <p>Path-specific: only intercepts {@code /actuator/flyway} and direct
 * sub-paths. Other actuator endpoints (health, info, metrics, prometheus)
 * remain governed by their own access rules.
 *
 * @author KiteClass Team
 * @since 2026-05-12
 */
@Slf4j
@Component
@Order(5)
public class FlywayEndpointAuthFilter extends OncePerRequestFilter {

    /** Header forwarded by Gateway after authentication (per SecurityConfig). */
    public static final String ROLES_HEADER = "X-User-Roles";

    /** Path prefix this filter protects. */
    public static final String FLYWAY_PATH_PREFIX = "/actuator/flyway";

    /** Required role for access. */
    private static final String REQUIRED_ROLE = "ADMIN";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith(FLYWAY_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String rolesHeader = request.getHeader(ROLES_HEADER);
        if (!hasAdminRole(rolesHeader)) {
            log.warn("Denied access to {} — missing/invalid {} header from IP {}",
                    uri, ROLES_HEADER, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"errorCode\":\"FLYWAY_ENDPOINT_UNAUTHORIZED\","
                            + "\"message\":\"Admin role required to access Flyway migration endpoint.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Parses the {@code X-User-Roles} header and returns whether ADMIN is present.
     *
     * @param rolesHeader header value, may be null/blank
     * @return true iff {@value #REQUIRED_ROLE} appears (case-insensitive) in the list
     */
    static boolean hasAdminRole(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }
        List<String> roles = Arrays.stream(rolesHeader.split("[,\\s]+"))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .toList();
        return roles.contains(REQUIRED_ROLE);
    }
}
