package com.kiteclass.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bridges the Gateway-forwarded {@code X-User-Roles} header into the Spring
 * Security {@link SecurityContext} so that method-level {@code @PreAuthorize}
 * expressions using {@code hasRole(...)} / {@code hasAnyRole(...)} can be
 * satisfied.
 *
 * <p>Core trusts the Gateway ({@code JwtAuthenticationGatewayFilter}) which
 * validates the JWT and forwards the resolved identity via {@code X-User-Id},
 * {@code X-User-Roles} (single role or future comma-separated), and
 * {@code X-User-Email}. {@link SecurityConfig} permits all requests at the URL
 * layer; authorization happens at the method layer. Without this filter the
 * security context is empty, so every {@code hasRole}/{@code hasAnyRole} guard
 * (and {@code AuthorizationBean.isAdmin()}, which reads authorities) denies with
 * HTTP 403 regardless of the forwarded role — see KC-7 G1 walk finding
 * 2026-06-05 (24 endpoints across 10 controllers were dead-deny).
 *
 * <p>Roles are normalised to Spring's {@code ROLE_} authority convention:
 * {@code OWNER} → {@code ROLE_OWNER}. Values already prefixed with
 * {@code ROLE_} are passed through unchanged.
 *
 * <p>Wired into the security filter chain via
 * {@code http.addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)}
 * so the authority context is established before the authorization decision.
 * Tenant + user context are set separately by {@link TenantFilterInterceptor};
 * this filter is concerned only with Spring Security authorities.
 *
 * <p>Mirrors the proven {@code XUserRolesHeaderFilter} in kitehub-subscription
 * (GAP-706 / GAP-783) — kiteclass-core was never given the equivalent bridge.
 *
 * @author KiteClass Team
 * @see SecurityConfig
 * @see com.kiteclass.core.common.security.AuthorizationBean
 */
@Slf4j
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    /** Header carrying the Gateway-resolved role(s). */
    public static final String ROLES_HEADER = "X-User-Roles";

    /** Header carrying the Gateway-resolved user id (JWT {@code sub}). */
    public static final String USER_ID_HEADER = "X-User-Id";

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String rolesHeader = request.getHeader(ROLES_HEADER);

        if (rolesHeader != null && !rolesHeader.isBlank()) {
            List<GrantedAuthority> authorities = toAuthorities(rolesHeader);
            if (!authorities.isEmpty()) {
                String principal = request.getHeader(USER_ID_HEADER);
                if (principal == null || principal.isBlank()) {
                    principal = "gateway-user";
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, "", authorities);

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                if (log.isDebugEnabled()) {
                    log.debug("Gateway auth established: principal={} authorities={}", principal, authorities);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Maps a raw {@code X-User-Roles} header value into Spring authorities.
     * Supports a single role or a comma-separated list; each value is upper-cased
     * and prefixed with {@code ROLE_} unless already prefixed.
     *
     * @param rolesHeader raw header value (e.g. {@code "OWNER"} or {@code "OWNER,ADMIN"})
     * @return authorities (never null; may be empty for a blank/garbage header)
     */
    private List<GrantedAuthority> toAuthorities(String rolesHeader) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String raw : rolesHeader.split(",")) {
            String role = raw.trim();
            if (role.isEmpty()) {
                continue;
            }
            String normalised = role.toUpperCase();
            if (!normalised.startsWith(ROLE_PREFIX)) {
                normalised = ROLE_PREFIX + normalised;
            }
            authorities.add(new SimpleGrantedAuthority(normalised));
        }
        return authorities;
    }
}
